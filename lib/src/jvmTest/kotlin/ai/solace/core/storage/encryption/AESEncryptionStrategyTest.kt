package ai.solace.core.storage.encryption

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AESEncryptionStrategyTest {
    @Test
    fun `test encryption and decryption`() {
        val strategy = AESEncryptionStrategy()
        val data = "Hello, World!".toByteArray()

        val encrypted = strategy.encrypt(data)
        val decrypted = strategy.decrypt(encrypted)

        assertNotEquals(data.toList(), encrypted.toList())
        assertEquals(data.toList(), decrypted.toList())
    }

    @Test
    fun `test key generation and persistence`() {
        val strategy1 = AESEncryptionStrategy()
        val keyBytes = strategy1.getKeyBytes()
        val keyBase64 = strategy1.getKeyBase64()

        val strategy2 = AESEncryptionStrategy(AESEncryptionStrategy.createKeyFromBytes(keyBytes))
        val strategy3 = AESEncryptionStrategy(AESEncryptionStrategy.createKeyFromBase64(keyBase64))

        val data = "Hello, World!".toByteArray()
        val encrypted1 = strategy1.encrypt(data)

        // Different encryption instances with the same key should be able to decrypt each other's data
        val decrypted2 = strategy2.decrypt(encrypted1)
        val decrypted3 = strategy3.decrypt(encrypted1)

        assertEquals(data.toList(), decrypted2.toList())
        assertEquals(data.toList(), decrypted3.toList())
    }

    @Test
    fun `test different encryptions of same data are different`() {
        val strategy = AESEncryptionStrategy()
        val data = "Hello, World!".toByteArray()

        val encrypted1 = strategy.encrypt(data)
        val encrypted2 = strategy.encrypt(data)

        // Each encryption should produce different results due to random IV
        assertNotEquals(encrypted1.toList(), encrypted2.toList())

        // But both should decrypt to the same original data
        val decrypted1 = strategy.decrypt(encrypted1)
        val decrypted2 = strategy.decrypt(encrypted2)

        assertEquals(data.toList(), decrypted1.toList())
        assertEquals(data.toList(), decrypted2.toList())
    }
}
