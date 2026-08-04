package com.spiritlane.service.impl;

import com.spiritlane.dto.AddressDto;
import com.spiritlane.dto.RegistrationDto;
import com.spiritlane.exception.BusinessException;
import com.spiritlane.exception.GlobalExceptionHandler;
import com.spiritlane.exception.ResourceNotFoundException;
import com.spiritlane.entity.*;
import com.spiritlane.repository.*;
import com.spiritlane.service.UserService;
import com.spiritlane.util.AppUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {

	private static final Logger log =
            LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final UserAddressRepository addressRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, UserAddressRepository addressRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public User register(RegistrationDto dto) {
        if (!dto.isPasswordMatch()) {
            throw new BusinessException("Passwords do not match");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Email already registered. Please login.");
        }
        if (userRepository.existsByPhone(dto.getPhone())) {
            throw new BusinessException("Phone number already registered.");
        }
        if (!AppUtils.isAdult(dto.getDateOfBirth())) {
            throw new BusinessException("You must be 18+ years old to register on SpiritLane.");
        }

        String roleName = dto.getRole() != null ? dto.getRole() : "ROLE_CUSTOMER";
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        User user = new User();

        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail().toLowerCase().trim());
        user.setPhone(dto.getPhone());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(role);
        user.setDateOfBirth(dto.getDateOfBirth());
        user.setIsAgeVerified(true);
        user.setIsActive(true);
        user.setIsBlocked(false);

        User saved = userRepository.save(user);
        log.info("New user registered: {} [{}]", saved.getEmail(), roleName);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No user found with email: " + email));
    }

    @Override
    public User updateProfile(Long userId, String fullName, String phone) {
        User user = findById(userId);
        if (!user.getPhone().equals(phone) && userRepository.existsByPhone(phone)) {
            throw new BusinessException("Phone number already in use by another account.");
        }
        user.setFullName(fullName);
        user.setPhone(phone);
        return userRepository.save(user);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = findById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email.toLowerCase().trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean phoneExists(String phone) {
        return userRepository.existsByPhone(phone);
    }

    @Override
    public void resetPasswordByEmail(String email, String newPassword) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("No user found with email: " + email));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password reset via OTP for user: {}", user.getEmail());
    }

    @Override
    public UserAddress addAddress(Long userId, AddressDto dto) {
        User user = findById(userId);
        long count = addressRepository.countByUserId(userId);
        if (count >= 5) {
            throw new BusinessException("You can have a maximum of 5 saved addresses.");
        }
        // If this is the first address, make it default
        boolean makeDefault = (count == 0) || Boolean.TRUE.equals(dto.getIsDefault());
        if (makeDefault) {
            addressRepository.findByUserIdAndIsDefault(userId, true)
                    .ifPresent(addr -> { addr.setIsDefault(false); addressRepository.save(addr); });
        }
        UserAddress address = new UserAddress();

        address.setUser(user);
        address.setAddressLine1(dto.getAddressLine1());
        address.setAddressLine2(dto.getAddressLine2());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setPincode(dto.getPincode());
        address.setLandmark(dto.getLandmark());
        address.setAddressType(dto.getAddressType());
        address.setIsDefault(makeDefault);
        return addressRepository.save(address);
    }

    @Override
    public UserAddress updateAddress(Long addressId, AddressDto dto) {
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
        if (Boolean.TRUE.equals(dto.getIsDefault()) && !address.getIsDefault()) {
            addressRepository.findByUserIdAndIsDefault(address.getUser().getId(), true)
                    .ifPresent(a -> { a.setIsDefault(false); addressRepository.save(a); });
        }
        address.setAddressLine1(dto.getAddressLine1());
        address.setAddressLine2(dto.getAddressLine2());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setPincode(dto.getPincode());
        address.setLandmark(dto.getLandmark());
        address.setAddressType(dto.getAddressType());
        address.setIsDefault(dto.getIsDefault());
        return addressRepository.save(address);
    }

    @Override
    public void deleteAddress(Long addressId) {
        addressRepository.deleteById(addressId);
    }

    @Override
    public void setDefaultAddress(Long userId, Long addressId) {
        addressRepository.findByUserIdAndIsDefault(userId, true)
                .ifPresent(a -> { a.setIsDefault(false); addressRepository.save(a); });
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
        address.setIsDefault(true);
        addressRepository.save(address);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAddress> getUserAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllCustomers(Pageable pageable) {
        return userRepository.findByRoleName("ROLE_CUSTOMER", pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllDeliveryAgents(Pageable pageable) {
        return userRepository.findByRoleName("ROLE_DELIVERY_AGENT", pageable);
    }

    @Override
    public void blockUser(Long userId) {
        User user = findById(userId);
        user.setIsBlocked(true);
        userRepository.save(user);
    }

    @Override
    public void unblockUser(Long userId) {
        User user = findById(userId);
        user.setIsBlocked(false);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByRole(String roleName) {
        return userRepository.countByRoleName(roleName);
    }
}