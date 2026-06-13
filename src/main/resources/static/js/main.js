/* ============================================================
   SpiritLane - Main JavaScript
   Cart, Toast notifications, AJAX helpers, UI utilities
   ============================================================ */

/* ─── CSRF helper ─── */
function getCsrfToken() {
    return $('meta[name="_csrf"]').attr('content') || '';
}

function getCsrfHeader() {
    return $('meta[name="_csrf_header"]').attr('content') || 'X-CSRF-TOKEN';
}

/* ─── Setup AJAX CSRF ─── */
$(document).ready(function () {
    var token  = getCsrfToken();
    var header = getCsrfHeader();
    if (token) {
        $.ajaxSetup({
            beforeSend: function (xhr) {
                xhr.setRequestHeader(header, token);
            }
        });
    }
    updateCartCount();
    initAutoHideAlerts();
    initTableSearch();
});

/* ============================================================
   TOAST NOTIFICATIONS
   ============================================================ */
function showToast(message, type) {
    type = type || 'success';
    var container = document.getElementById('toastContainer');
    if (!container) return;

    var toast = document.createElement('div');
    toast.className = 'sl-toast toast-' + type;
    toast.innerHTML =
        '<div style="display:flex;align-items:flex-start;gap:0.6rem;">' +
            '<i class="fas ' + (type === 'success' ? 'fa-check-circle' : 'fa-times-circle') +
            '" style="margin-top:2px;font-size:1rem;flex-shrink:0;"></i>' +
            '<span style="font-size:0.9rem;line-height:1.4;">' + message + '</span>' +
        '</div>';

    container.appendChild(toast);

    setTimeout(function () {
        toast.style.animation = 'slideOutRight 0.3s ease forwards';
        setTimeout(function () {
            if (toast.parentNode) toast.parentNode.removeChild(toast);
        }, 300);
    }, 3500);
}

/* ============================================================
   CART FUNCTIONS
   ============================================================ */

/* Update cart badge count in navbar */
function updateCartCount() {
    $.ajax({
        url: '/cart/count',
        method: 'GET',
        success: function (resp) {
            var badge = document.getElementById('cartCountBadge');
            if (badge) {
                badge.textContent = resp.count || 0;
                badge.style.display = (resp.count && resp.count > 0) ? 'flex' : 'none';
            }
        },
        error: function () { /* Silently ignore — user may not be logged in */ }
    });
}

/* Add to cart — button click on product card (browse/home) */
function addToCart(btn) {
    var inventoryId = btn.getAttribute('data-inventory-id');
    if (!inventoryId) return;
    addToCartQty(inventoryId, 1);
}

/* Add to cart with specific quantity */
function addToCartQty(inventoryId, quantity) {
    var originalText = '';
    var btn = document.querySelector('[data-inventory-id="' + inventoryId + '"]');
    if (btn) {
        originalText = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Adding...';
    }

    $.ajax({
        url: '/cart/add',
        method: 'POST',
        data: {
            inventoryId: inventoryId,
            quantity: quantity
        },
        success: function (resp) {
            if (resp.success) {
                showToast(resp.message || 'Added to cart!', 'success');
                updateCartCount();
            } else {
                showToast(resp.message || 'Could not add to cart.', 'error');
            }
        },
        error: function (xhr) {
            if (xhr.status === 401 || xhr.status === 403) {
                showToast('Please login to add items to cart.', 'error');
                setTimeout(function () { window.location.href = '/auth/login'; }, 1200);
            } else {
                showToast('Something went wrong. Please try again.', 'error');
            }
        },
        complete: function () {
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = originalText;
            }
        }
    });
}

/* ============================================================
   TABLE SEARCH / FILTER
   ============================================================ */
function filterTable(inputId, tbodyId) {
    var input  = document.getElementById(inputId);
    var tbody  = document.getElementById(tbodyId);
    if (!input || !tbody) return;

    var filter = input.value.toLowerCase().trim();
    var rows   = tbody.getElementsByTagName('tr');

    for (var i = 0; i < rows.length; i++) {
        var text = rows[i].textContent || rows[i].innerText;
        rows[i].style.display = text.toLowerCase().indexOf(filter) > -1 ? '' : 'none';
    }
}

/* Bind all table searches on page load */
function initTableSearch() {
    var searches = document.querySelectorAll('.table-search');
    searches.forEach(function (input) {
        var tbodyId = input.getAttribute('data-target');
        if (tbodyId) {
            input.addEventListener('keyup', function () {
                filterTable(input.id, tbodyId);
            });
        }
    });
}

/* ============================================================
   AUTO-HIDE ALERTS
   ============================================================ */
function initAutoHideAlerts() {
    var alerts = document.querySelectorAll('.sl-alert');
    alerts.forEach(function (alert) {
        setTimeout(function () {
            alert.style.transition = 'opacity 0.5s ease';
            alert.style.opacity = '0';
            setTimeout(function () {
                if (alert.parentNode) alert.style.display = 'none';
            }, 500);
        }, 5000);
    });
}

/* ============================================================
   SIDEBAR ACTIVE LINK
   ============================================================ */
$(document).ready(function () {
    var currentPath = window.location.pathname;
    $('.sl-sidebar .nav-link, .sidebar-nav .nav-link').each(function () {
        var href = $(this).attr('href');
        if (href && currentPath.startsWith(href) && href !== '/') {
            $(this).addClass('active');
        }
    });
});

/* ============================================================
   ADDRESS SELECTION HIGHLIGHT (Checkout)
   ============================================================ */
$(document).on('change', 'input[name="addressId"]', function () {
    var id = $(this).val();
    $('[id^="addr-card-"]').css('border-color', 'var(--border-color)');
    $('#addr-card-' + id).css('border-color', 'var(--primary)');
});

/* ============================================================
   CONFIRM DELETE / BLOCK DIALOGS
   ============================================================ */
$(document).on('submit', 'form[data-confirm]', function (e) {
    var msg = $(this).attr('data-confirm') || 'Are you sure?';
    if (!confirm(msg)) {
        e.preventDefault();
        return false;
    }
});

/* ============================================================
   SMOOTH SCROLL FOR ANCHOR LINKS
   ============================================================ */
$(document).on('click', 'a[href^="#"]', function (e) {
    var target = $(this.getAttribute('href'));
    if (target.length) {
        e.preventDefault();
        $('html, body').animate({ scrollTop: target.offset().top - 80 }, 400);
    }
});

/* ============================================================
   PRODUCT IMAGE LAZY LOAD FALLBACK
   ============================================================ */
$(document).ready(function () {
    $('img').on('error', function () {
        $(this).attr('src', '/images/product-placeholder.png');
    });
});

/* ============================================================
   MOBILE NAV CLOSE ON LINK CLICK
   ============================================================ */
$(document).ready(function () {
    // Only close navbar when clicking real nav links — NOT dropdown toggles
    $('.navbar-nav .nav-link:not(.dropdown-toggle)').on('click', function () {
        var toggler = document.querySelector('.navbar-toggler');
        var collapse = document.querySelector('#navbarMain');
        if (collapse && collapse.classList.contains('show') && toggler) {
            toggler.click();
        }
    });

    // Close navbar when a dropdown ITEM (inside the menu) is clicked
    $('.navbar-nav .dropdown-menu .dropdown-item').on('click', function () {
        var toggler = document.querySelector('.navbar-toggler');
        var collapse = document.querySelector('#navbarMain');
        if (collapse && collapse.classList.contains('show') && toggler) {
            toggler.click();
        }
    });
});

/* ============================================================
   FORM VALIDATION HELPERS
   ============================================================ */
function validatePhone(input) {
    var val = input.value.replace(/\D/g, '');
    if (val.length > 10) val = val.slice(0, 10);
    input.value = val;
    var isValid = /^[6-9]\d{9}$/.test(val);
    input.style.borderColor = isValid ? 'var(--primary)' : (val.length > 0 ? 'var(--danger)' : 'var(--border-color)');
    return isValid;
}

function validatePincode(input) {
    var val = input.value.replace(/\D/g, '');
    if (val.length > 6) val = val.slice(0, 6);
    input.value = val;
    var isValid = /^[1-9][0-9]{5}$/.test(val);
    input.style.borderColor = isValid ? 'var(--primary)' : (val.length > 0 ? 'var(--danger)' : 'var(--border-color)');
    return isValid;
}

/* ============================================================
   PASSWORD STRENGTH INDICATOR
   ============================================================ */
function checkPasswordStrength(password) {
    var strength = 0;
    if (password.length >= 8) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/[a-z]/.test(password)) strength++;
    if (/[0-9]/.test(password)) strength++;
    if (/[^A-Za-z0-9]/.test(password)) strength++;
    return strength;
}

$(document).on('input', 'input[name="password"]', function () {
    var val = $(this).val();
    var strength = checkPasswordStrength(val);
    var indicator = $(this).siblings('.password-strength');
    if (!indicator.length) {
        $(this).after('<div class="password-strength" style="height:3px;border-radius:2px;margin-top:4px;transition:all 0.3s;"></div>');
        indicator = $(this).siblings('.password-strength');
    }
    var colors = ['', '#dc3545', '#fd7e14', '#ffc107', '#28a745', '#20c997'];
    var widths = ['0%', '20%', '40%', '60%', '80%', '100%'];
    indicator.css({ background: colors[strength] || '#444', width: widths[strength] || '0%' });
});

/* ============================================================
   NUMERIC INPUT: prevent non-numeric except allowed keys
   ============================================================ */
$(document).on('keypress', 'input[type="number"]', function (e) {
    if (e.which < 48 || e.which > 57) e.preventDefault();
});
