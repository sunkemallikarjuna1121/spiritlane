package com.spiritlane.repository;

import com.spiritlane.entity.ShopInventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShopInventoryRepository extends JpaRepository<ShopInventory, Long> {

    @Query("SELECT si FROM ShopInventory si LEFT JOIN FETCH si.product p LEFT JOIN FETCH p.images WHERE si.shop.id = :shopId")
    List<ShopInventory> findByShopIdWithImages(@Param("shopId") Long shopId);

    Page<ShopInventory> findByShopIdAndIsAvailableTrue(Long shopId, Pageable pageable);

    Optional<ShopInventory> findByShopIdAndProductId(Long shopId, Long productId);

    boolean existsByShopIdAndProductId(Long shopId, Long productId);

    @Query("SELECT si FROM ShopInventory si WHERE si.shop.id = :shopId AND si.isAvailable = true AND " +
           "(LOWER(si.product.name) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<ShopInventory> searchByShop(@Param("shopId") Long shopId, @Param("q") String query, Pageable pageable);

    @Query("SELECT si FROM ShopInventory si LEFT JOIN FETCH si.product p LEFT JOIN FETCH p.images WHERE si.id = :id")
    Optional<ShopInventory> findByIdWithImages(@Param("id") Long id);

    @Query(value = "SELECT si.id FROM ShopInventory si " +
                   "WHERE si.isAvailable = true AND si.stockQuantity > 0 AND " +
                   "si.shop.isApproved = true AND si.shop.isActive = true AND " +
                   "(:catId IS NULL OR si.product.category.id = :catId) AND " +
                   "(:brandId IS NULL OR si.product.brand.id = :brandId) AND " +
                   "(LOWER(si.product.name) LIKE LOWER(CONCAT('%',:q,'%')) OR :q = '')",
           countQuery = "SELECT COUNT(si) FROM ShopInventory si " +
                   "WHERE si.isAvailable = true AND si.stockQuantity > 0 AND " +
                   "si.shop.isApproved = true AND si.shop.isActive = true AND " +
                   "(:catId IS NULL OR si.product.category.id = :catId) AND " +
                   "(:brandId IS NULL OR si.product.brand.id = :brandId) AND " +
                   "(LOWER(si.product.name) LIKE LOWER(CONCAT('%',:q,'%')) OR :q = '')")
    Page<Long> browseProductIds(
            @Param("catId") Long catId,
            @Param("brandId") Long brandId,
            @Param("q") String query,
            Pageable pageable);

    @Query("SELECT si FROM ShopInventory si LEFT JOIN FETCH si.product p LEFT JOIN FETCH p.images " +
           "WHERE si.id IN :ids")
    List<ShopInventory> findByIdsWithImages(@Param("ids") List<Long> ids);
}