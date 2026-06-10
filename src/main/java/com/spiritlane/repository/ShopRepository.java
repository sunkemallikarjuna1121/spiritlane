package com.spiritlane.repository;

import com.spiritlane.entity.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {

    List<Shop> findByOwnerId(Long ownerId);

    Optional<Shop> findByOwnerIdAndIsActive(Long ownerId, Boolean isActive);

    List<Shop> findByIsApprovedAndIsActive(Boolean isApproved, Boolean isActive);

    Page<Shop> findByIsApprovedAndIsActive(Boolean isApproved, Boolean isActive, Pageable pageable);

    List<Shop> findByPincodeAndIsApprovedAndIsActive(String pincode, Boolean approved, Boolean active);

    List<Shop> findByCityIgnoreCaseAndIsApprovedAndIsActive(String city, Boolean approved, Boolean active);

    boolean existsByLicenseNumber(String licenseNumber);

    @Query("SELECT s FROM Shop s WHERE s.isApproved = false AND s.isActive = true ORDER BY s.createdAt ASC")
    List<Shop> findPendingApproval();

    @Query("SELECT s FROM Shop s WHERE " +
           "(LOWER(s.shopName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(s.city) LIKE LOWER(CONCAT('%',:q,'%'))) " +
           "AND s.isApproved = true AND s.isActive = true")
    Page<Shop> searchShops(@Param("q") String query, Pageable pageable);
}
