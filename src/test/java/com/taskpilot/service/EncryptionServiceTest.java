package com.taskpilot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService encryptionService;
    private static final String TEST_PLAIN_TEXT = "Hello, World!";
    private static final String VALID_BASE64_KEY = "5ztQ9vi8XElkYDUJ4sQLIUKqooCVNpzUzoGtHAHGbN4=";

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService(VALID_BASE64_KEY);
    }

    @Test
    @DisplayName("encrypt() should create different ciphertexts for same plaintext")
    void encrypt_ShouldCreateDifferentCiphertexts() {
        // ARRANGE & ACT
        String ciphertext1 = encryptionService.encrypt(TEST_PLAIN_TEXT);
        String ciphertext2 = encryptionService.encrypt(TEST_PLAIN_TEXT);

        // ASSERT
        assertNotNull(ciphertext1);
        assertNotNull(ciphertext2);
        assertNotEquals(ciphertext1, ciphertext2);
    }

    @Test
    @DisplayName("decrypt() should recover original plaintext")
    void decrypt_ShouldRecoverPlaintext() {
        // ARRANGE
        String ciphertext = encryptionService.encrypt(TEST_PLAIN_TEXT);

        // ACT
        String decrypted = encryptionService.decrypt(ciphertext);

        // ASSERT
        assertEquals(TEST_PLAIN_TEXT, decrypted);
    }

    @Test
    @DisplayName("encrypt() should handle null input")
    void encrypt_ShouldHandleNull() {
        assertNull(encryptionService.encrypt(null));
    }

    @Test
    @DisplayName("decrypt() should handle null input")
    void decrypt_ShouldHandleNull() {
        assertNull(encryptionService.decrypt(null));
    }

    @Test
    @DisplayName("decrypt() should throw exception for invalid ciphertext")
    void decrypt_ShouldThrowOnInvalidInput() {
        // ARRANGE
        String invalidCiphertext = "InvalidBase64Data";

        // ACT & ASSERT
        assertThrows(EncryptionService.EncryptionException.class,
                () -> encryptionService.decrypt(invalidCiphertext));
    }

    @Test
    @DisplayName("Service should initialize with generated key when none provided")
    void constructor_ShouldGenerateKeyWhenNoneProvided() {
        // ARRANGE & ACT
        EncryptionService serviceWithGeneratedKey = new EncryptionService(null);

        // ASSERT
        String encrypted = serviceWithGeneratedKey.encrypt(TEST_PLAIN_TEXT);
        String decrypted = serviceWithGeneratedKey.decrypt(encrypted);
        assertEquals(TEST_PLAIN_TEXT, decrypted);
    }

    @Test
    @DisplayName("generateSecretKeyString() should produce valid Base64 key")
    void generateSecretKeyString_ShouldProduceValidKey() {
        // ACT
        String generatedKey = EncryptionService.generateSecretKeyString();

        // ASSERT
        assertNotNull(generatedKey);
        assertDoesNotThrow(() -> new EncryptionService(generatedKey));
    }
}