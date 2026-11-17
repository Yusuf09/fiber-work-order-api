package com.yusufabdiqadir.fiberworkorders.workorder;

import java.util.Map;
import java.util.Set;

/**
 * Lifecycle of a work order. Each status lists the statuses it is allowed to move to,
 * so invalid transitions (for example COMPLETED back to OPEN) are rejected in one place.
 */
public enum WorkOrderStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> ALLOWED = Map.of(
            OPEN, Set.of(ASSIGNED, CANCELLED),
            ASSIGNED, Set.of(ASSIGNED, IN_PROGRESS, CANCELLED),
            IN_PROGRESS, Set.of(COMPLETED, CANCELLED),
            COMPLETED, Set.of(),
            CANCELLED, Set.of()
    );

    public boolean canMoveTo(WorkOrderStatus next) {
        return ALLOWED.get(this).contains(next);
    }
}
