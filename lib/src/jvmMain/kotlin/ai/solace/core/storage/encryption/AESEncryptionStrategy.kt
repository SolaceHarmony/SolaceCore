package ai.solace.core.storage.encryption

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

/**
 * AES encryption strategy implementation.
 *
 * This class implements the EncryptionStrategy interface using the AES algorithm
 * with GCM (Galois/Counter Mode) for authenticated encryption.
 *
 * @param key The encryption key to use. If not provided, a random key will be generated.
 */
class AESEncryptionStrategy(private val key: SecretKey = generateKey()) : EncryptionStrategy {
    companion object {
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12

        /**
         * Generates a random AES key.
         *
         * @return A random AES key.
         */
        fun generateKey(): SecretKey {
            val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
            keyGenerator.init(256) // Use 256-bit keys for better security
            return keyGenerator.generateKey()
        }

        /**
         * Creates a SecretKey from a byte array.
         *
         * @param keyBytes The key as a byte array.
         * @return A SecretKey instance.
         */
        fun createKeyFromBytes(keyBytes: ByteArray): SecretKey {
            return SecretKeySpec(keyBytes, ALGORITHM)
        }

        /**
         * Creates a SecretKey from a Base64-encoded string.
         *
         * @param keyBase64 The key as a Base64-encoded string.
         * @return A SecretKey instance.
         */
        fun createKeyFromBase64(keyBase64: String): SecretKey {
            val keyBytes = Base64.getDecoder().decode(keyBase64)
            return createKeyFromBytes(keyBytes)
        }
    }

    /**
     * Encrypts the given data using AES with GCM mode.
     *
     * @param data The data to encrypt.
     * @return The encrypted data, with the IV prepended.
     */
    override fun encrypt(data: ByteArray): ByteArray {
        // Generate a random IV
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        // Initialize the cipher with the key and IV
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

        // Encrypt the data
        val encrypted = cipher.doFinal(data)

        // Combine IV and encrypted data
        return iv + encrypted
    }

    /**
     * Decrypts the given data using AES with GCM mode.
     *
     * @param data The data to decrypt, with the IV prepended.
     * @return The decrypted data.
     */
    override fun decrypt(data: ByteArray): ByteArray {
        // Extract the IV and encrypted data
        val iv = data.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = data.copyOfRange(GCM_IV_LENGTH, data.size)

        // Initialize the cipher with the key and IV
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

        // Decrypt the data
        return cipher.doFinal(encrypted)
    }

    /**
     * Gets the encryption key as a byte array.
     *
     * @return The encryption key as a byte array.
     */
    fun getKeyBytes(): ByteArray {
        return key.encoded
    }

    /**
     * Gets the encryption key as a Base64-encoded string.
     *
     * @return The encryption key as a Base64-encoded string.
     */
    fun getKeyBase64(): String {
        return Base64.getEncoder().encodeToString(key.encoded)
    }
}
