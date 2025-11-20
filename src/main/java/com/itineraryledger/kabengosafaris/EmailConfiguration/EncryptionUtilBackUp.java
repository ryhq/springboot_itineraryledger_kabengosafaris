package com.itineraryledger.kabengosafaris.EmailConfiguration;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for encrypting and decrypting sensitive email configuration data
 * Uses AES (Advanced Encryption Standard) symmetric encryption
 *
 * WARNING: This is a basic encryption utility for demonstration purposes.
 * For production use, consider:
 * - Using Spring Cloud Config Encryption
 * - Using AWS Secrets Manager or similar
 * - Using Spring Security's encryption features
 * - Storing encryption keys in a secure vault (HashiCorp Vault, etc.)
 */
@Slf4j
public class EncryptionUtilBackUp {

    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;

    // Default encryption key - SHOULD BE EXTERNALIZED IN PRODUCTION
    // This should be stored in environment variables or a secrets manager
    // This is a Base64-encoded 256-bit (32-byte) key
    private static final String DEFAULT_KEY = "8JY8VxzKfXyP3mL9N5qR2tWaBcDeFgHiJkLmNoPqRsT=";

    /**
     * Generate a secure encryption key
     * This should be done once and stored securely
     */
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE);
        return keyGenerator.generateKey();
    }

    /**
     * Get the encryption key from a string
     * In production, this should load from a secure source
     */
    private static SecretKey getKeyFromString(String keyString) {
        byte[] decodedKey = Base64.getDecoder().decode(keyString);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
    }

    /**
     * Encrypt a plain text value (like SMTP password)
     * @param plainText the text to encrypt
     * @return encrypted value as Base64 string
     */
    public static String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey key = getKeyFromString(DEFAULT_KEY);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("Error encrypting data", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt an encrypted value
     * @param encryptedText the Base64-encoded encrypted text
     * @return decrypted plain text
     */
    public static String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey key = getKeyFromString(DEFAULT_KEY);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes);
        } catch (Exception e) {
            log.error("Error decrypting data", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Check if a string is encrypted (Base64 format)
     */
    public static boolean isEncrypted(String value) {
        try {
            Base64.getDecoder().decode(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
