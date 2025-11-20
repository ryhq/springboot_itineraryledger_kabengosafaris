package com.itineraryledger.kabengosafaris.Security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHasher {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    /**
     * Hashes a password using BCrypt.
     *
     * @param password the password to hash
     * @return the hashed password
     */
    public static String hashPassword(String password) {
        return encoder.encode(password);
    }


    /**
     * Compares a raw password with its BCrypt hashed counterpart.
     *
     * @param rawPassword     the raw password to compare
     * @param hashedPassword  the hashed password to compare against
     * @return true if the raw password matches the hashed password, false otherwise
     */
    public static boolean comparePasswords(String rawPassword, String hashedPassword) {
        return encoder.matches(rawPassword, hashedPassword);
    }
}
