package com.taskpilot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EncryptionService class.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EncryptionServiceTest {

    private EncryptionService encryptionService;
    private EncryptionService encryptionServiceWithConfiguredKey;

    // Test key for consistent testing (Base64 encoded)

//    private static final String TEST_SECRET_KEY = "1234567890123456789012345678901234567890123="; // This should be a proper 256-bit key
    private static final String TEST_SECRET_KEY = "raxh8HNFcCrUBgxB6144U2/azwt1xB9KBPemjPpTGDphuJIwVeVZYrWyF6vcQCdh"; // This should be a proper 256-bit key
    @BeforeEach
    void setUp() {
        // Create service with auto-generated key
        encryptionService = new EncryptionService(null);

        // Create service with configured key
        String validTestKey = EncryptionService.generateSecretKeyString();
        encryptionServiceWithConfiguredKey = new EncryptionService(validTestKey);
    }

    @Test
    @DisplayName("Should encrypt and decrypt simple text successfully")
    void testBasicEncryptionDecryption() {
        // Given
        String plainText = "Hello, World!";

        // When
        String encrypted = encryptionService.encrypt(plainText);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    @DisplayName("Should handle null input gracefully")
    void testNullInputHandling() {
        // When & Then
        assertNull(encryptionService.encrypt(null));
        assertNull(encryptionService.decrypt(null));
    }

    @Test
    @DisplayName("Should handle empty string")
    void testEmptyStringHandling() {
        // Given
        String emptyString = "";

        // When
        String encrypted = encryptionService.encrypt(emptyString);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertNotNull(encrypted);
        assertEquals(emptyString, decrypted);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Short text",
            "A much longer text that contains various characters and symbols !@#$%^&*()",
            "Text with unicode characters: 你好世界 🌍",
            "Text with newlines\nand\ttabs",
            "Special characters: <>?:\"{}|_+",
            "Numbers: 1234567890",
            " Leading and trailing spaces "
    })
    @DisplayName("Should handle various text inputs correctly")
    void testVariousTextInputs(String plainText) {
        // When
        String encrypted = encryptionService.encrypt(plainText);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    @DisplayName("Should produce different encrypted values for same input (due to random IV)")
    void testEncryptionRandomness() {
        // Given
        String plainText = "Same input text";

        // When
        String encrypted1 = encryptionService.encrypt(plainText);
        String encrypted2 = encryptionService.encrypt(plainText);

        // Then
        assertNotEquals(encrypted1, encrypted2, "Encrypted values should be different due to random IV");

        // But both should decrypt to the same plain text
        assertEquals(plainText, encryptionService.decrypt(encrypted1));
        assertEquals(plainText, encryptionService.decrypt(encrypted2));
    }

    @Test
    @DisplayName("Should work with configured secret key")
    void testWithConfiguredSecretKey() {
        // Given
        String plainText = "Test with configured key";

        // When
        String encrypted = encryptionServiceWithConfiguredKey.encrypt(plainText);
        String decrypted = encryptionServiceWithConfiguredKey.decrypt(encrypted);

        // Then
        assertEquals(plainText, decrypted);
    }

    @Test
    @DisplayName("Should throw exception for invalid encrypted data")
    void testInvalidEncryptedData() {
        // Given
        String invalidEncryptedData = "This is not valid encrypted data";

        // When & Then
        assertThrows(EncryptionService.EncryptionException.class, () -> {
            encryptionService.decrypt(invalidEncryptedData);
        });
    }

    @Test
    @DisplayName("Should throw exception for corrupted encrypted data")
    void testCorruptedEncryptedData() {
        // Given
        String plainText = "Test data";
        String encrypted = encryptionService.encrypt(plainText);

        // Corrupt the encrypted data by modifying a character
        String corruptedEncrypted = encrypted.substring(0, encrypted.length() - 5) + "XXXXX";

        // When & Then
        assertThrows(EncryptionService.EncryptionException.class, () -> {
            encryptionService.decrypt(corruptedEncrypted);
        });
    }

    @Test
    @DisplayName("Should handle very long text")
    void testVeryLongText() {
        // Given
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longText.append("This is a very long text that we're using to test encryption. ");
        }
        String plainText = longText.toString();

        // When
        String encrypted = encryptionService.encrypt(plainText);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertEquals(plainText, decrypted);
        assertTrue(encrypted.length() > 0);
    }

    @Test
    @DisplayName("Should generate valid secret key string")
    void testGenerateSecretKeyString() {
        // When
        String secretKeyString = EncryptionService.generateSecretKeyString();

        // Then
        assertNotNull(secretKeyString);
        assertFalse(secretKeyString.isEmpty());

        // Should be valid Base64
        assertDoesNotThrow(() -> Base64.getDecoder().decode(secretKeyString));

        // Should be able to create service with generated key
        assertDoesNotThrow(() -> new EncryptionService(secretKeyString));
    }

    @Test
    @DisplayName("Should create different instances with different keys")
    void testDifferentServiceInstances() {
        // Given
        String key1 = EncryptionService.generateSecretKeyString();
        String key2 = EncryptionService.generateSecretKeyString();

        EncryptionService service1 = new EncryptionService(key1);
        EncryptionService service2 = new EncryptionService(key2);

        String plainText = "Test with different keys";

        // When
        String encrypted1 = service1.encrypt(plainText);
        String encrypted2 = service2.encrypt(plainText);

        // Then
        assertNotEquals(encrypted1, encrypted2);

        // Each service should decrypt its own encryption
        assertEquals(plainText, service1.decrypt(encrypted1));
        assertEquals(plainText, service2.decrypt(encrypted2));

        // But services with different keys shouldn't decrypt each other's data
        assertThrows(EncryptionService.EncryptionException.class, () -> {
            service1.decrypt(encrypted2);
        });
        assertThrows(EncryptionService.EncryptionException.class, () -> {
            service2.decrypt(encrypted1);
        });
    }

    @Test
    @DisplayName("Should maintain consistency across multiple encrypt/decrypt cycles")
    void testMultipleEncryptDecryptCycles() {
        // Given
        String originalText = "Consistency test text";
        String currentText = originalText;

        // When - perform multiple encrypt/decrypt cycles
        for (int i = 0; i < 10; i++) {
            String encrypted = encryptionService.encrypt(currentText);
            currentText = encryptionService.decrypt(encrypted);
        }

        // Then
        assertEquals(originalText, currentText);
    }

    @Test
    @DisplayName("Should handle whitespace-only text")
    void testWhitespaceOnlyText() {
        // Given
        String whitespaceText = "   \t\n\r   ";

        // When
        String encrypted = encryptionService.encrypt(whitespaceText);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertEquals(whitespaceText, decrypted);
    }

    @Test
    @DisplayName("Should throw exception for invalid Base64 in decrypt")
    void testInvalidBase64Decrypt() {
        // Given
        String invalidBase64 = "This is not Base64!@#$%^&*()";

        // When & Then
        assertThrows(EncryptionService.EncryptionException.class, () -> {
            encryptionService.decrypt(invalidBase64);
        });
    }
}