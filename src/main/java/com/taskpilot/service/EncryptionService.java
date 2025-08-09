package com.taskpilot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting and decrypting strings using AES-GCM encryption.
 * This implementation uses AES-256 with GCM mode for authenticated encryption.
 */
@Service
public class EncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final int AES_KEY_LENGTH = 256; // 256 bits

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    /**
     * Constructor that initializes the encryption service.
     * The secret key can be provided via application.properties or generated automatically.
     *
     * @param secretKeyString Base64 encoded secret key from application.properties (optional)
     */
    public EncryptionService(@Value("${app.encryption.secret-key:#{null}}") String secretKeyString) {
        this.secureRandom = new SecureRandom();

        if (secretKeyString != null && !secretKeyString.trim().isEmpty()) {
            // Use provided key from configuration
            byte[] keyBytes = Base64.getDecoder().decode(secretKeyString);
            this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            logger.info("Encryption service initialized with configured secret key");
        } else {
            // Generate a new key (not recommended for production)
            this.secretKey = generateSecretKey();
            logger.warn("Encryption service initialized with generated key. " +
                    "For production, configure app.encryption.secret-key property. " +
                    "Generated key (Base64): {}", Base64.getEncoder().encodeToString(secretKey.getEncoded()));
        }
    }

    /**
     * Encrypts a plain text string.
     *
     * @param plainText The string to encrypt
     * @return Base64 encoded encrypted string with IV prepended
     * @throws EncryptionException if encryption fails
     */
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }

        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            // Encrypt the text
            byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Combine IV and encrypted data
            byte[] encryptedWithIv = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
            System.arraycopy(encryptedData, 0, encryptedWithIv, iv.length, encryptedData.length);

            // Return Base64 encoded result
            return Base64.getEncoder().encodeToString(encryptedWithIv);

        } catch (Exception e) {
            logger.error("Encryption failed", e);
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypts an encrypted string.
     *
     * @param encryptedText Base64 encoded encrypted string with IV prepended
     * @return Decrypted plain text string
     * @throws EncryptionException if decryption fails
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null) {
            return null;
        }

        try {
            // Decode from Base64
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedText);

            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedData = new byte[encryptedWithIv.length - GCM_IV_LENGTH];

            System.arraycopy(encryptedWithIv, 0, iv, 0, iv.length);
            System.arraycopy(encryptedWithIv, iv.length, encryptedData, 0, encryptedData.length);

            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            // Decrypt the data
            byte[] decryptedData = cipher.doFinal(encryptedData);

            return new String(decryptedData, StandardCharsets.UTF_8);

        } catch (Exception e) {
            logger.error("Decryption failed", e);
            throw new EncryptionException("Failed to decrypt data", e);
        }
    }

    /**
     * Generates a new AES secret key.
     *
     * @return Generated secret key
     */
    private SecretKey generateSecretKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(AES_KEY_LENGTH);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate secret key", e);
        }
    }

    /**
     * Generates a new random secret key and returns it as Base64 string.
     * Useful for generating keys to put in configuration files.
     *
     * @return Base64 encoded secret key
     */
    public static String generateSecretKeyString() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(AES_KEY_LENGTH);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate secret key", e);
        }
    }

    /**
     * Custom exception for encryption/decryption errors.
     */
    public static class EncryptionException extends RuntimeException {
        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}