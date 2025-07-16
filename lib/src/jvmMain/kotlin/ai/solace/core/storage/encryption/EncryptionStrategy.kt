package ai.solace.core.storage.encryption

/**
 * Interface for encryption strategies.
 *
 * This interface defines the contract for encryption and decryption operations.
 * Implementations of this interface provide specific encryption algorithms.
 */
interface EncryptionStrategy {
    /**
     * Encrypts the given data.
     *
     * @param data The data to encrypt.
     * @return The encrypted data.
     */
    fun encrypt(data: ByteArray): ByteArray

    /**
     * Decrypts the given data.
     *
     * @param data The data to decrypt.
     * @return The decrypted data.
     */
    fun decrypt(data: ByteArray): ByteArray
}
