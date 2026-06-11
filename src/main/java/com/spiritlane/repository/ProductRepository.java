package com.spiritlane.repository;

import com.spiritlane.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Product> findByCategoryIdAndIsActiveTrue(Long categoryId);

    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.createdAt DESC")
    List<Product> findLatestProducts(Pageable pageable);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.isActive = true ORDER BY p.createdAt DESC")
    List<Product> findLatestProductsWithImages(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.tags) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Product> searchProducts(@Param("q") String query, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
           "(:catId IS NULL OR p.category.id = :catId) AND " +
           "(:brandId IS NULL OR p.brand.id = :brandId) AND " +
           "(:type IS NULL OR CAST(p.productType AS string) = :type)")
    Page<Product> filterProducts(
            @Param("catId") Long catId,
            @Param("brandId") Long brandId,
            @Param("type") String type,
            Pageable pageable);
}
