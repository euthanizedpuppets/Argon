package dev.argon.chunks;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Removes cancelled entries from a task list without reordering the remaining
 * entries. It intentionally does not execute tasks or alter scheduler policy.
 */
public final class CancelledChunkTaskPruner {
    private CancelledChunkTaskPruner() {
    }

    /**
     * Removes cancelled items from the end toward the start to avoid repeated
     * shifts in array-backed task lists. The relative order of survivors stays
     * exactly the same.
     *
     * @return number of removed entries
     */
    public static <T> int pruneCancelled(List<T> tasks, Predicate<? super T> isCancelled) {
        Objects.requireNonNull(tasks, "tasks must not be null");
        Objects.requireNonNull(isCancelled, "isCancelled must not be null");

        int removed = 0;
        for (int index = tasks.size() - 1; index >= 0; index--) {
            if (isCancelled.test(tasks.get(index))) {
                tasks.remove(index);
                removed++;
            }
        }
        return removed;
    }
}
