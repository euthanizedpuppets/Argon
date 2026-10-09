package dev.argon.core;

/** Independently controlled optimization capabilities. */
public enum ArgonFeature {
    VISIBILITY_OPTIMIZATION,
    CHUNK_PRIORITY_SCHEDULING,
    CHUNK_REBUILD_DEDUPLICATION,
    CANCELLED_CHUNK_TASK_CLEANUP,
    MESH_BUFFER_REUSE,
    ADAPTIVE_WORK_BUDGETS,
    EXPERIMENTAL_RENDERER
}
