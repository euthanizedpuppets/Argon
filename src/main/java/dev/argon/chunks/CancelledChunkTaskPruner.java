package dev.argon.chunks;

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Predicate;

/**
 * Removes cancelled entries from a task list without reordering the remaining
 * entries. It intentionally does not execute tasks or alter scheduler policy.
 */
public final class CancelledChunkTaskPruner {
    private CancelledChunkTaskPruner() {
    }

    /**
     * Removes cancelled entries while preserving survivor order. Random-access
     * lists (including the native chunk queue's array-backed list) are compacted
     * in one linear pass followed by one tail clear. Other lists use their
     * iterator so linked-list removal stays linear as well.
     *
     * @return number of removed entries
     */
    public static <T> int pruneCancelled(List<T> tasks, Predicate<? super T> isCancelled) {
        Objects.requireNonNull(tasks, "tasks must not be null");
        Objects.requireNonNull(isCancelled, "isCancelled must not be null");

        if (tasks instanceof RandomAccess) {
            return compactRandomAccess(tasks, isCancelled);
        }

        int removed = 0;
        ListIterator<T> iterator = tasks.listIterator();
        while (iterator.hasNext()) {
            if (isCancelled.test(iterator.next())) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    private static <T> int compactRandomAccess(
            List<T> tasks, Predicate<? super T> isCancelled) {
        int originalSize = tasks.size();
        int writeIndex = 0;
        int removed = 0;

        for (int readIndex = 0; readIndex < originalSize; readIndex++) {
            T task = tasks.get(readIndex);
            if (isCancelled.test(task)) {
                removed++;
                continue;
            }

            if (writeIndex != readIndex) {
                tasks.set(writeIndex, task);
            }
            writeIndex++;
        }

        if (writeIndex < originalSize) {
            tasks.subList(writeIndex, originalSize).clear();
        }
        return removed;
    }
}
