package com.spiritlane.repository;

import com.spiritlane.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    List<Brand> findByIsActiveTrueOrderByNameAsc();
    Optional<Brand> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
