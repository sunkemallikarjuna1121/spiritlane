package com.spiritlane.repository;

import com.spiritlane.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findByIsActiveTrueAndPositionOrderBySortOrderAsc(Banner.BannerPosition position);
}
