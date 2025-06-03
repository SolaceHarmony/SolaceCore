package ai.solace.core.storage

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class InMemoryConfigurationStorageTest {

    private lateinit var configStorage: InMemoryConfigurationStorage

    @BeforeTest
    fun setup() {
        configStorage = InMemoryConfigurationStorage()
    }

    @Test
    fun testGetSetConfigValue() = runBlocking {
        withTimeout(5.seconds) {
        // Store a simple config directly
        val key = "test"
        val config = mapOf("simple" to 12345)

        val storeResult = configStorage.store(key, config)
        assertTrue(storeResult, "Storing config should succeed")

        // Retrieve the config
        val retrievedConfig = configStorage.retrieve(key)
        assertNotNull(retrievedConfig, "Retrieved config should not be null")
        assertEquals(config["simple"], retrievedConfig.first["simple"], "Config values should match")
        }
    }

    @Test
    fun testNestedConfigValues() = runBlocking {
        withTimeout(5.seconds) {
        // Set nested config values
        configStorage.setConfigValue("server", "database.url", "jdbc:mysql://localhost:3306/mydb")
        configStorage.setConfigValue("server", "database.username", "root")
        configStorage.setConfigValue("server", "database.password", "password")
        configStorage.setConfigValue("server", "http.port", 8080)

        // Get nested config values
        assertEquals("jdbc:mysql://localhost:3306/mydb", configStorage.getConfigValue("server", "database.url"))
        assertEquals("root", configStorage.getConfigValue("server", "database.username"))
        assertEquals("password", configStorage.getConfigValue("server", "database.password"))
        assertEquals(8080, configStorage.getConfigValue("server", "http.port"))

        // Retrieve the entire config
        val config = configStorage.retrieve("server")?.first
        assertNotNull(config, "Config should not be null")

        val database = config["database"] as? Map<*, *>
        assertNotNull(database, "Database config should not be null")
        assertEquals("jdbc:mysql://localhost:3306/mydb", database["url"])
        assertEquals("root", database["username"])
        assertEquals("password", database["password"])

        val http = config["http"] as? Map<*, *>
        assertNotNull(http, "HTTP config should not be null")
        assertEquals(8080, http["port"])
        }
    }

    @Test
    fun testComponentConfig() = runBlocking {
        withTimeout(5.seconds) {
        // Set component config
        val componentId = "myComponent"
        val config = mapOf(
            "enabled" to true,
            "maxConnections" to 10,
            "timeout" to 5000L
        )

        val setResult = configStorage.setComponentConfig(componentId, config)
        assertTrue(setResult, "Setting component config should succeed")

        // Get component config
        val retrievedConfig = configStorage.getComponentConfig(componentId)
        assertNotNull(retrievedConfig, "Retrieved component config should not be null")
        assertEquals(config["enabled"], retrievedConfig["enabled"], "Config values should match")
        assertEquals(config["maxConnections"], retrievedConfig["maxConnections"], "Config values should match")
        assertEquals(config["timeout"], retrievedConfig["timeout"], "Config values should match")
        }
    }

    @Test
    fun testSystemConfig() = runBlocking {
        withTimeout(5.seconds) {
        // Initially, system config should be empty
        val initialConfig = configStorage.getSystemConfig()
        assertTrue(initialConfig.isEmpty(), "Initial system config should be empty")

        // Set system config
        val systemConfig = mapOf(
            "appName" to "MyApp",
            "version" to "1.0.0",
            "logLevel" to "INFO"
        )

        val setResult = configStorage.setSystemConfig(systemConfig)
        assertTrue(setResult, "Setting system config should succeed")

        // Get system config
        val retrievedConfig = configStorage.getSystemConfig()
        assertEquals(systemConfig.size, retrievedConfig.size, "Config size should match")
        assertEquals(systemConfig["appName"], retrievedConfig["appName"], "Config values should match")
        assertEquals(systemConfig["version"], retrievedConfig["version"], "Config values should match")
        assertEquals(systemConfig["logLevel"], retrievedConfig["logLevel"], "Config values should match")
        }
    }

    @Test
    fun testNonExistentConfig() = runBlocking {
        withTimeout(5.seconds) {
        // Try to get a non-existent config value
        val value = configStorage.getConfigValue("non-existent", "path")
        assertNull(value, "Non-existent config value should be null")

        // Try to get a non-existent component config
        val componentConfig = configStorage.getComponentConfig("non-existent")
        assertNull(componentConfig, "Non-existent component config should be null")
        }
    }

    @Test
    fun testOverwriteConfigValue() = runBlocking {
        withTimeout(5.seconds) {
        // Set a config value
        configStorage.setConfigValue("server", "port", 8080)
        assertEquals(8080, configStorage.getConfigValue("server", "port"))

        // Overwrite the config value
        configStorage.setConfigValue("server", "port", 9090)
        assertEquals(9090, configStorage.getConfigValue("server", "port"))
        }
    }

    @Test
    fun testInvalidPath() = runBlocking {
        withTimeout(5.seconds) {
        // Try with an empty path (which should be invalid)
        val emptyPathResult = configStorage.setConfigValue("server", "", 8080)
        assertFalse(emptyPathResult, "Setting config with empty path should fail")

        // Try with invalid characters
        val invalidCharResult = configStorage.setConfigValue("server", "port$@#!", 8080)
        assertFalse(invalidCharResult, "Setting config with invalid characters should fail")

        // Test with consecutive dots which should be invalid
        val consecutiveDotsResult = configStorage.setConfigValue("server", "network..port", 8080)
        assertFalse(consecutiveDotsResult, "Setting config with consecutive dots should fail")

        // Test with a path that starts with a dot
        val startsWithDotResult = configStorage.setConfigValue("server", ".port", 8080)
        assertFalse(startsWithDotResult, "Setting config with path starting with dot should fail")

        // Test with a path that ends with a dot
        val endsWithDotResult = configStorage.setConfigValue("server", "port.", 8080)
        assertFalse(endsWithDotResult, "Setting config with path ending with dot should fail")
        }
    }
}
