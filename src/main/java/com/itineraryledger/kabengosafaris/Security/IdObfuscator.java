package com.itineraryledger.kabengosafaris.Security;

import lombok.extern.slf4j.Slf4j;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ID Obfuscator Component
 * Encodes numeric IDs into hash strings to hide internal ID sequences.
 * Configuration values are sourced from the database via SecuritySettingsService.
 * Falls back to application.properties if database settings not available.
 *
 * Configurable Settings:
 * - idObfuscator.obfuscated.length: Length of the hash string (default: 70)
 * - idObfuscator.salt.length: Length of the salt for obfuscation (default: 21)
 * - idObfuscator.enabled: Whether obfuscation is enabled (default: true)
 */
@Component
@Slf4j
public class IdObfuscator {

    private Hashids hashids;
    private int obfuscatedIdLength;
    private int saltLength;

    // Fallback values from application.properties
    @Value("${security.idObfuscator.obfuscated.length:70}")
    private int defaultObfuscatedLength;

    @Value("${security.idObfuscator.salt.length:21}")
    private int defaultSaltLength;

    @Autowired
    public IdObfuscator(SecuritySettingsService securitySettingsService) {
        this.hashids = initializeHashids(securitySettingsService);
    }

    /**
     * Initialize Hashids with configuration from database or fallback
     */
    private Hashids initializeHashids(SecuritySettingsService securitySettingsService) {
        try {
            // Try to get configuration from database
            this.obfuscatedIdLength = securitySettingsService.getIdObfuscationLength();
            this.saltLength = securitySettingsService.getIdObfuscationSaltLength();

            log.info("IdObfuscator: Using database settings - length={}, saltLength={}",
                    obfuscatedIdLength, saltLength);
        } catch (Exception e) {
            // Fallback to application.properties values
            this.obfuscatedIdLength = defaultObfuscatedLength > 0 ? defaultObfuscatedLength : 70;
            this.saltLength = defaultSaltLength > 0 ? defaultSaltLength : 21;

            log.warn("IdObfuscator: Database settings not available, using application.properties fallback - " +
                    "length={}, saltLength={}", obfuscatedIdLength, saltLength);
        }

        // Ensure saltLength is at least 8 for StrongPasswordGenerator
        int saltLengthForGenerator = Math.max(saltLength, 8);

        // Generate salt with configured length
        String salt = StrongPasswordGenerator.generateStrongPassword(saltLengthForGenerator);

        // Initialize and return Hashids with configured settings
        Hashids result = new Hashids(salt, obfuscatedIdLength);
        log.info("IdObfuscator initialized successfully with length={}, saltLength={}",
                obfuscatedIdLength, saltLength);
        return result;
    }

    /**
     * Encodes a numeric ID into a hash string.
     * @param id The ID to encode
     * @return The encoded hash string
     * @throws IllegalArgumentException if id is null
     */
    public String encodeId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        return hashids.encode(id);
    }

    /**
     * Decodes a hash string back to numeric ID.
     * @param hash The hash to decode
     * @return The decoded ID
     * @throws IllegalArgumentException if hash is null or empty
     * @throws IllegalStateException if hash cannot be decoded
     */
    public Long decodeId(String hash) {
        if (hash == null || hash.trim().isEmpty()) {
            throw new IllegalArgumentException("Hash cannot be null or empty");
        }

        long[] decoded = hashids.decode(hash);
        if (decoded.length == 0) {
            throw new IllegalStateException("Unable to decode hash: " + hash);
        }

        return decoded[0];
    }
}
