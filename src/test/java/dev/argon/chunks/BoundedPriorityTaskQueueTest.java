package dev.argon.chunks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class BoundedPriorityTaskQueueTest {
    @Test
    void higherPriorityTasksArePolledFirst() {
        BoundedPriorityTaskQueue<String, String> queue = new BoundedPriorityTaskQueue<>(4);
        queue.offer("far", "far task", 1);
        queue.offer("near", "near task", 10);
        queue.offer("middle", "middle task", 5);

        assertEquals("near", queue.poll().orElseThrow().key());
        assertEquals("middle", queue.poll().orElseThrow().key());
        assertEquals("far", queue.poll().orElseThrow().key());
        assertTrue(queue.isEmpty());
    }

    @Test
    void duplicateKeysDoNotConsumeAdditionalCapacity() {
        BoundedPriorityTaskQueue<String, String> queue = new BoundedPriorityTaskQueue<>(1);
        assertEquals(BoundedPriorityTaskQueue.OfferResult.ADDED,
                queue.offer("chunk", "first", 1));
        assertEquals(BoundedPriorityTaskQueue.OfferResult.ALREADY_QUEUED,
                queue.offer("chunk", "duplicate", 1));
        assertEquals(1, queue.size());
        assertEquals("first", queue.poll().orElseThrow().value());
    }

    @Test
    void higherPriorityReplacesQueuedTask() {
        BoundedPriorityTaskQueue<String, String> queue = new BoundedPriorityTaskQueue<>(2);
        queue.offer("chunk", "old", 1);

        assertEquals(BoundedPriorityTaskQueue.OfferResult.PRIORITY_RAISED,
                queue.offer("chunk", "urgent", 9));
        var task = queue.poll().orElseThrow();
        assertEquals("urgent", task.value());
        assertEquals(9, task.priority());
        assertTrue(queue.isEmpty());
    }

    @Test
    void queueRejectsNewKeysAtCapacityButAllowsPriorityUpgrade() {
        BoundedPriorityTaskQueue<String, String> queue = new BoundedPriorityTaskQueue<>(1);
        queue.offer("a", "a", 1);

        assertEquals(BoundedPriorityTaskQueue.OfferResult.FULL, queue.offer("b", "b", 99));
        assertEquals(BoundedPriorityTaskQueue.OfferResult.PRIORITY_RAISED,
                queue.offer("a", "urgent a", 2));
    }

    @Test
    void repeatedPriorityUpgradesKeepStaleEntriesBounded() {
        BoundedPriorityTaskQueue<String, String> queue = new BoundedPriorityTaskQueue<>(1);
        queue.offer("chunk", "v0", 0);

        for (int i = 1; i <= 10_000; i++) {
            assertEquals(BoundedPriorityTaskQueue.OfferResult.PRIORITY_RAISED,
                    queue.offer("chunk", "v" + i, i));
            assertTrue(queue.retainedEntryCount() <= 16);
        }

        assertEquals("v10000", queue.poll().orElseThrow().value());
        assertTrue(queue.isEmpty());
    }

    @Test
    void repeatedCancellationDoesNotAccumulateHeapEntries() {
        BoundedPriorityTaskQueue<String, String> queue = new BoundedPriorityTaskQueue<>(1);
        for (int i = 0; i < 1_000; i++) {
            assertEquals(BoundedPriorityTaskQueue.OfferResult.ADDED,
                    queue.offer("chunk", "task" + i, i));
            assertTrue(queue.cancel("chunk"));
            assertTrue(queue.retainedEntryCount() <= 16);
        }
        assertTrue(queue.isEmpty());
    }

    @Test
    void cancelAndClearRemovePendingWork() {
        BoundedPriorityTaskQueue<String, String> queue = new BoundedPriorityTaskQueue<>(3);
        queue.offer("a", "a", 1);
        queue.offer("b", "b", 2);

        assertTrue(queue.cancel("a"));
        assertFalse(queue.cancel("missing"));
        assertEquals("b", queue.poll().orElseThrow().key());
        queue.offer("c", "c", 1);
        queue.clear();

        assertTrue(queue.isEmpty());
        assertTrue(queue.poll().isEmpty());
    }

    @Test
    void capacityMustBePositive() {
        assertThrows(IllegalArgumentException.class,
                () -> new BoundedPriorityTaskQueue<>(0));
    }
}
