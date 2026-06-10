package com.spiritlane.service;

import com.spiritlane.dto.AddressDto;
import com.spiritlane.dto.RegistrationDto;
import com.spiritlane.entity.User;
import com.spiritlane.entity.UserAddress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface UserService {

    User register(RegistrationDto dto);

    User findById(Long id);

    User findByEmail(String email);

    User updateProfile(Long userId, String fullName, String phone);

    void changePassword(Long userId, String oldPassword, String newPassword);

    // Address management
    UserAddress addAddress(Long userId, AddressDto dto);

    UserAddress updateAddress(Long addressId, AddressDto dto);

    void deleteAddress(Long addressId);

    void setDefaultAddress(Long userId, Long addressId);

    List<UserAddress> getUserAddresses(Long userId);

    // Admin operations
    Page<User> getAllCustomers(Pageable pageable);

    Page<User> getAllDeliveryAgents(Pageable pageable);

    void blockUser(Long userId);

    void unblockUser(Long userId);

    long countByRole(String roleName);
}
