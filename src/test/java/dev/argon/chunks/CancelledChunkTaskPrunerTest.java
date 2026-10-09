package dev.argon.chunks;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class CancelledChunkTaskPrunerTest {
    @Test
    void removesOnlyCancelledTasksAndPreservesRemainingOrder() {
        List<String> tasks = new ArrayList<>(List.of("cancel-a", "keep-a", "cancel-b", "keep-b"));

        int removed = CancelledChunkTaskPruner.pruneCancelled(
                tasks, task -> task.startsWith("cancel"));

        assertEquals(2, removed);
        assertEquals(List.of("keep-a", "keep-b"), tasks);
    }

    @Test
    void doesNotMutateListWhenNoTasksAreCancelled() {
        List<Integer> tasks = new ArrayList<>(List.of(3, 2, 1));

        int removed = CancelledChunkTaskPruner.pruneCancelled(tasks, task -> false);

        assertEquals(0, removed);
        assertEquals(List.of(3, 2, 1), tasks);
    }

    @Test
    void emptyListIsSafe() {
        List<String> tasks = new ArrayList<>();

        assertEquals(0, CancelledChunkTaskPruner.pruneCancelled(tasks, task -> true));
        assertTrue(tasks.isEmpty());
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class,
                () -> CancelledChunkTaskPruner.pruneCancelled(null, task -> true));
        assertThrows(NullPointerException.class,
                () -> CancelledChunkTaskPruner.pruneCancelled(new ArrayList<>(), null));
    }
}
