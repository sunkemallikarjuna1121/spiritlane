package com.spiritlane.repository;

import com.spiritlane.entity.DeliveryAgentDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryAgentDetailRepository extends JpaRepository<DeliveryAgentDetail, Long> {
    Optional<DeliveryAgentDetail> findByAgentId(Long agentId);
    List<DeliveryAgentDetail> findByIsAvailableTrue();
}
