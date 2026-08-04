package com.spiritlane.repository;

import com.spiritlane.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByEmailAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(
            String email, OtpVerification.Purpose purpose);

    @Modifying
    @Query("DELETE FROM OtpVerification o WHERE o.email = :email AND o.purpose = :purpose")
    void deleteByEmailAndPurpose(@Param("email") String email,
                                  @Param("purpose") OtpVerification.Purpose purpose);
}