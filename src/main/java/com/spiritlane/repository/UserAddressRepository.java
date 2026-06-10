package com.spiritlane.repository;

import com.spiritlane.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserIdOrderByIsDefaultDesc(Long userId);

    Optional<UserAddress> findByUserIdAndIsDefault(Long userId, Boolean isDefault);

    long countByUserId(Long userId);
}
