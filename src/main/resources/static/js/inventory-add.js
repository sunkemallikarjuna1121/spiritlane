/**
 * inventory-add.js
 * Powers the "Add Product to Inventory" modal on shop/inventory.html:
 *  - type-to-search product picker (jQuery UI autocomplete) instead of a
 *    long <select>, with already-added products filtered out
 *  - live preview (image + brand/category/volume) of the selected product
 *  - MRP / Selling Price / Discount % kept in sync automatically
 *  - AJAX submit so the shop owner can save & keep adding products without
 *    the page reloading each time
 *
 * Expects these globals to be defined by the page before this file loads:
 *   ALL_PRODUCTS            -> [{id, name, brand, category, volumeMl, image}]
 *   EXISTING_PRODUCT_IDS    -> [productId, ...] already in this shop's inventory
 *   ADD_INVENTORY_AJAX_URL  -> string URL for the JSON add endpoint
 */
(function () {
    'use strict';

    var existingIds = new Set((window.EXISTING_PRODUCT_IDS || []).map(Number));
    var productsById = {};
    (window.ALL_PRODUCTS || []).forEach(function (p) {
        productsById[p.id] = p;
    });

    var pendingAction = 'another'; // which submit button was pressed

    function csrfToken() {
        var m = document.querySelector('meta[name="_csrf"]');
        return m ? m.content : '';
    }

    function csrfHeaderName() {
        var m = document.querySelector('meta[name="_csrf_header"]');
        return m ? m.content : 'X-CSRF-TOKEN';
    }

    function availableProducts() {
        return (window.ALL_PRODUCTS || []).filter(function (p) {
            return !existingIds.has(Number(p.id));
        });
    }

    function productLabel(p) {
        var meta = [];
        if (p.brand) meta.push(p.brand);
        if (p.volumeMl) meta.push(p.volumeMl + 'ml');
        return p.name + (meta.length ? ' (' + meta.join(' · ') + ')' : '');
    }

    /* ---------- Modal open/close ---------- */

    window.openAddModal = function () {
        document.getElementById('addProductModal').style.display = 'flex';
        resetForm(true);
        setTimeout(function () {
            var input = document.getElementById('productSearchInput');
            if (input) input.focus();
        }, 50);
    };

    window.closeAddModal = function () {
        document.getElementById('addProductModal').style.display = 'none';
    };

    /* ---------- Product picker (jQuery UI autocomplete) ---------- */

    function initAutocomplete() {
        var $input = $('#productSearchInput');
        if (!$input.length || !$.ui) return;

        $input.autocomplete({
            minLength: 0,
            delay: 100,
            source: function (request, response) {
                var term = $.ui.autocomplete.escapeRegex(request.term.trim());
                var re = new RegExp(term, 'i');
                var matches = availableProducts()
                    .filter(function (p) {
                        return term === '' ||
                            re.test(p.name) ||
                            re.test(p.brand || '') ||
                            re.test(p.category || '');
                    })
                    .slice(0, 50)
                    .map(function (p) {
                        return { label: productLabel(p), value: productLabel(p), id: p.id };
                    });
                response(matches);
            },
            select: function (event, ui) {
                event.preventDefault();
                selectProduct(ui.item.id);
            },
            open: function () {
                $(this).autocomplete('widget').addClass('sl-autocomplete-menu');
            }
        }).on('focus', function () {
            $(this).autocomplete('search', '');
        });
    }

    function selectProduct(productId) {
        var p = productsById[productId];
        if (!p) return;

        document.getElementById('productIdInput').value = p.id;
        document.getElementById('productSearchInput').value = productLabel(p);

        var metaParts = [];
        if (p.brand) metaParts.push(p.brand);
        if (p.category) metaParts.push(p.category);
        if (p.volumeMl) metaParts.push(p.volumeMl + 'ml');

        document.getElementById('previewImg').src = p.image || '/images/product-placeholder.png';
        document.getElementById('previewName').textContent = p.name;
        document.getElementById('previewMeta').textContent = metaParts.join(' · ');
        document.getElementById('productPreview').style.display = 'flex';
    }

    document.addEventListener('click', function (e) {
        if (e.target.closest && e.target.closest('#previewClearBtn')) {
            document.getElementById('productIdInput').value = '';
            document.getElementById('productSearchInput').value = '';
            document.getElementById('productPreview').style.display = 'none';
            document.getElementById('productSearchInput').focus();
        }
    });

    /* ---------- MRP / Selling Price / Discount auto-calc ---------- */

    function initPriceSync() {
        var mrp = document.getElementById('mrpInput');
        var price = document.getElementById('sellingPriceInput');
        var disc = document.getElementById('discountInput');
        if (!mrp || !price || !disc) return;

        var syncing = false;

        function recalcFromDiscount() {
            if (syncing) return;
            var m = parseFloat(mrp.value);
            var d = parseFloat(disc.value);
            if (!isNaN(m) && m > 0 && !isNaN(d)) {
                syncing = true;
                price.value = (m - (m * d / 100)).toFixed(2);
                syncing = false;
            }
        }

        function recalcFromPrice() {
            if (syncing) return;
            var m = parseFloat(mrp.value);
            var s = parseFloat(price.value);
            if (!isNaN(m) && m > 0 && !isNaN(s)) {
                syncing = true;
                disc.value = Math.max(0, ((m - s) / m * 100)).toFixed(2);
                syncing = false;
            }
        }

        mrp.addEventListener('input', recalcFromDiscount);
        disc.addEventListener('input', recalcFromDiscount);
        price.addEventListener('input', recalcFromPrice);
    }

    /* ---------- Form reset ---------- */

    function resetForm(clearMessage) {
        var form = document.getElementById('addInventoryForm');
        form.reset();
        document.getElementById('productIdInput').value = '';
        document.getElementById('productSearchInput').value = '';
        document.getElementById('productPreview').style.display = 'none';
        if (clearMessage) showFormMessage('', false);
    }

    function showFormMessage(text, isError) {
        var el = document.getElementById('modalFormMsg');
        if (!text) {
            el.style.display = 'none';
            return;
        }
        el.textContent = text;
        el.className = 'modal-form-msg ' + (isError ? 'is-error' : 'is-success');
        el.style.display = 'block';
    }

    /* ---------- Submit (AJAX) ---------- */

    function setSubmitting(isSubmitting) {
        ['saveAddAnotherBtn', 'saveCloseBtn'].forEach(function (id) {
            var btn = document.getElementById(id);
            if (btn) btn.disabled = isSubmitting;
        });
    }

    function buildRowHtml(inv) {
        var metaBits = [];
        if (inv.brand) metaBits.push(inv.brand);
        if (inv.volumeMl) metaBits.push(inv.volumeMl + 'ml');
        var discountBadge = (inv.discountPct && Number(inv.discountPct) > 0)
            ? '<span style="color:var(--success);font-size:0.82rem;font-weight:600;">' + Number(inv.discountPct).toFixed(0) + '%</span>'
            : '<span style="color:var(--text-muted);font-size:0.82rem;">—</span>';
        var token = csrfToken();

        return '' +
            '<tr data-inventory-id="' + inv.id + '">' +
            '  <td>' +
            '    <div style="display:flex;align-items:center;gap:0.7rem;">' +
            '      <img src="' + inv.imageUrl + '" style="width:40px;height:40px;object-fit:cover;border-radius:var(--radius-sm);background:var(--dark-surface);" onerror="this.src=\'/images/product-placeholder.png\'"/>' +
            '      <div>' +
            '        <div style="font-weight:600;color:var(--text-light);font-size:0.88rem;">' + escapeHtml(inv.productName) + '</div>' +
            '        <div style="font-size:0.75rem;color:var(--text-muted);">' + escapeHtml(metaBits.join(' · ')) + '</div>' +
            '      </div>' +
            '    </div>' +
            '  </td>' +
            '  <td style="font-size:0.85rem;">' + escapeHtml(inv.category || '') + '</td>' +
            '  <td style="color:var(--text-muted);">₹' + inv.mrp + '</td>' +
            '  <td style="font-weight:600;color:var(--primary-light);">₹' + inv.sellingPrice + '</td>' +
            '  <td>' + discountBadge + '</td>' +
            '  <td>' +
            '    <form action="/shop/inventory/' + inv.id + '/update-stock" method="post" style="display:flex;align-items:center;gap:0.4rem;">' +
            '      <input type="hidden" name="_csrf" value="' + token + '"/>' +
            '      <input type="number" name="stock" value="' + inv.stockQuantity + '" min="0" style="width:65px;background:var(--dark-surface);border:1px solid var(--border-color);color:var(--text-light);border-radius:var(--radius-sm);padding:0.25rem 0.4rem;font-size:0.85rem;text-align:center;"/>' +
            '      <button type="submit" style="background:rgba(200,134,10,0.2);color:var(--primary-light);border:1px solid rgba(200,134,10,0.3);border-radius:var(--radius-sm);padding:0.25rem 0.5rem;cursor:pointer;font-size:0.78rem;"><i class="fas fa-save"></i></button>' +
            '    </form>' +
            '  </td>' +
            '  <td>' +
            '    <form action="/shop/inventory/' + inv.id + '/toggle" method="post">' +
            '      <input type="hidden" name="_csrf" value="' + token + '"/>' +
            '      <button type="submit" style="background:rgba(40,167,69,0.2);color:#6ee08a;border:1px solid rgba(40,167,69,0.3);border-radius:var(--radius-sm);padding:0.25rem 0.6rem;cursor:pointer;font-size:0.78rem;"><span>Active</span></button>' +
            '    </form>' +
            '  </td>' +
            '  <td>' +
            '    <a href="/products/detail/' + inv.id + '" target="_blank" style="font-size:0.78rem;color:var(--text-muted);"><i class="fas fa-eye"></i></a>' +
            '  </td>' +
            '</tr>';
    }

    function escapeHtml(str) {
        return String(str == null ? '' : str)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function addRowToTable(inv) {
        var emptyRow = document.getElementById('emptyRow');
        if (emptyRow) emptyRow.remove();

        var tbody = document.getElementById('invBody');
        tbody.insertAdjacentHTML('afterbegin', buildRowHtml(inv));

        var countEl = document.getElementById('invCount');
        if (countEl) countEl.textContent = String((parseInt(countEl.textContent, 10) || 0) + 1);
    }

    function submitForm() {
        var form = document.getElementById('addInventoryForm');
        if (!document.getElementById('productIdInput').value) {
            showFormMessage('Please select a product first.', true);
            return;
        }
        if (!form.reportValidity()) return;

        var formData = new FormData(form);
        var headers = {};
        headers[csrfHeaderName()] = csrfToken();
        headers['X-Requested-With'] = 'XMLHttpRequest';

        setSubmitting(true);
        showFormMessage('', false);

        fetch(window.ADD_INVENTORY_AJAX_URL, {
            method: 'POST',
            headers: headers,
            body: formData,
            credentials: 'same-origin'
        })
            .then(function (res) {
                return res.json().then(function (data) {
                    return { ok: res.ok, data: data };
                });
            })
            .then(function (result) {
                setSubmitting(false);
                if (!result.ok || !result.data.success) {
                    showFormMessage(result.data.message || 'Could not add product.', true);
                    return;
                }

                var inv = result.data.inventory;
                existingIds.add(Number(inv.productId));
                addRowToTable(inv);

                if (pendingAction === 'close') {
                    closeAddModal();
                } else {
                    showFormMessage('"' + inv.productName + '" added. Add another below.', false);
                    resetForm(false);
                    var input = document.getElementById('productSearchInput');
                    if (input) input.focus();
                }
            })
            .catch(function () {
                setSubmitting(false);
                showFormMessage('Network error. Please try again.', true);
            });
    }

    /* ---------- Wire up ---------- */

    document.addEventListener('DOMContentLoaded', function () {
        initAutocomplete();
        initPriceSync();

        var addAnotherBtn = document.getElementById('saveAddAnotherBtn');
        var closeBtn = document.getElementById('saveCloseBtn');
        var form = document.getElementById('addInventoryForm');

        if (addAnotherBtn) {
            addAnotherBtn.addEventListener('click', function (e) {
                e.preventDefault();
                pendingAction = 'another';
                submitForm();
            });
        }
        if (closeBtn) {
            closeBtn.addEventListener('click', function (e) {
                e.preventDefault();
                pendingAction = 'close';
                submitForm();
            });
        }
        if (form) {
            // Fallback: Enter key inside a text field triggers a normal submit event
            form.addEventListener('submit', function (e) {
                e.preventDefault();
                submitForm();
            });
        }
    });
})();