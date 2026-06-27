<!-- topic: Runtime -->
<!-- title: Storage Testing -->

## Testing

The storage system includes comprehensive tests to ensure that it works correctly and handles edge cases properly. The tests are organized into the following categories:

### Unit Tests

Unit tests verify that individual components work correctly in isolation. They test the basic functionality of each storage implementation.

```kotlin
@Test
fun testStore() = runBlocking {
    val storage = InMemoryStorage<String, String>()
    assertTrue(storage.store("key", "value"))
    assertEquals("value", storage.retrieve("key")?.first)
}
```

### Thread Safety Tests

Thread safety tests verify that the storage system properly handles concurrent access and prevents deadlocks in various scenarios.

```kotlin
@Test
fun testConcurrentAccess() = runBlocking {
    val storage = InMemoryStorage<String, String>()
    val jobs = List(10) { index ->
        launch {
            storage.store("key$index", "value$index")
            assertEquals("value$index", storage.retrieve("key$index")?.first)
        }
    }
    jobs.forEach { it.join() }
}
```

### Transaction Tests

Transaction tests verify that the transaction support works correctly, ensuring that operations within a transaction are atomic and that data consistency is maintained.

```kotlin
@Test
fun testTransaction() = runBlocking {
    val storage = TransactionalInMemoryStorage<String, String>()
    storage.begin()
    storage.storeInTransaction("key1", "value1")
    storage.storeInTransaction("key2", "value2")
    storage.commit()
    assertEquals("value1", storage.retrieve("key1")?.first)
    assertEquals("value2", storage.retrieve("key2")?.first)
}
```


[Back to Storage & Persistence](Storage-and-Persistence)
