package ai.solace.core.actor

import kotlin.test.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout

class RecordingActorTest {
    @Test
    fun `test basic message recording`() = runTest {
        val actor = RecordingActor("test", "TestActor")

        try {
            actor.start()

            val message = ActorMessage(
                payload = "test",
                priority = MessagePriority.NORMAL
            )

            withTimeout(1000) { // 1 second timeout
                actor.receiveMessage(message)

                val recorded = actor.getRecordedMessages()
                assertEquals(1, recorded.size)
                assertEquals("test", recorded[0].payload)

                // Test clearing recordings
                actor.clearRecordings()
                val clearedRecordings = actor.getRecordedMessages()
                assertTrue(clearedRecordings.isEmpty())
            }
        } finally {
            actor.stop()
        }
    }

    @Test
    fun `test message priority handling`() = runTest {
        val actor = RecordingActor("test", "TestActor")

        try {
            actor.start()

            withTimeout(1000) {
                MessagePriority.entries.forEach { priority ->
                    actor.receiveMessage(
                        ActorMessage(
                            payload = "test",
                            priority = priority
                        )
                    )
                }

                val recorded = actor.getRecordedMessages()
                assertEquals(MessagePriority.entries.size, recorded.size)
            }
        } finally {
            actor.stop()
        }
    }

    @Test
    fun `test companion object factory methods`() = runTest {
        val actor = RecordingActor("test", "TestActor")

        try {
            actor.start()

            withTimeout(1000) {
                // High priority factory
                val highPriorityMessage = ActorMessage.highPriority("test")
                actor.receiveMessage(highPriorityMessage)

                // With metadata factory
                val metadataMessage = ActorMessage.withMetadata("test", mapOf("key" to "value"))
                actor.receiveMessage(metadataMessage)

                // Between factory
                val betweenMessage = ActorMessage.between("test", "sender")
                actor.receiveMessage(betweenMessage)

                val recorded = actor.getRecordedMessages()
                assertEquals(3, recorded.size)

                // Verify high priority message
                assertEquals(MessagePriority.HIGH, recorded[0].priority)

                // Verify metadata message
                assertEquals("value", recorded[1].metadata["key"])

                // Verify between message
                assertEquals("sender", recorded[2].sender)
            }
        } finally {
            actor.stop()
        }
    }

    @Test
    fun `test correlation tracking`() = runTest {
        val actor = RecordingActor("test", "TestActor")

        try {
            actor.start()

            withTimeout(1000) {
                val originalMessage = ActorMessage(payload = "original")
                val correlatedMessage = ActorMessage(
                    payload = "correlated",
                    correlationId = originalMessage.correlationId
                )

                actor.receiveMessage(originalMessage)
                actor.receiveMessage(correlatedMessage)

                val recorded = actor.getRecordedMessages()
                assertEquals(2, recorded.size)
                assertEquals(recorded[0].correlationId, recorded[1].correlationId)
            }
        } finally {
            actor.stop()
        }
    }

    @Test
    fun `test timestamp ordering`() = runTest {
        val actor = RecordingActor("test", "TestActor")

        try {
            actor.start()

            withTimeout(1000) {
                actor.receiveMessage(ActorMessage(payload = "first"))
                actor.receiveMessage(ActorMessage(payload = "second"))

                val recorded = actor.getRecordedMessages()
                assertTrue(recorded[0].timestamp <= recorded[1].timestamp)
            }
        } finally {
            actor.stop()
        }
    }
}