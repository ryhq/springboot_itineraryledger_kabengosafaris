package com.itineraryledger.kabengosafaris.User.Services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import com.itineraryledger.kabengosafaris.User.DTOs.UserDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for user-related operations
 */
@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IdObfuscator idObfuscator;

    /**
     * Get user by username
     * @param username the username to search for
     * @return Optional containing the User if found
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Get user by email
     * @param email the email to search for
     * @return Optional containing the User if found
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Convert User entity to UserDTO with obfuscated ID
     * @param user the User entity
     * @return UserDTO with obfuscated ID and sensitive fields excluded
     */
    public UserDTO convertToDTO(User user) {
        if (user == null) {
            return null;
        }

        UserDTO dto = new UserDTO();
        // Use obfuscated ID instead of numeric ID
        dto.setId(idObfuscator.encodeId(user.getId()));
        dto.setEmail(user.getEmail());
        // Exclude password from DTO
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setUsername(user.getUsername());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setEnabled(user.getEnabled());
        dto.setAccountLocked(user.getAccountLocked());
        dto.setBio(user.getBio());
        dto.setProfilePictureUrl(user.getProfilePictureUrl());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        return dto;
    }
}
