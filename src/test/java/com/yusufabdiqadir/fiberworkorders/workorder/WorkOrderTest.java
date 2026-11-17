package com.yusufabdiqadir.fiberworkorders.workorder;

import com.yusufabdiqadir.fiberworkorders.common.InvalidStateException;
import com.yusufabdiqadir.fiberworkorders.technician.Technician;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkOrderTest {

    private static final Instant NOW = Instant.parse("2026-11-16T15:00:00Z");

    private WorkOrder order;
    private Technician technician;

    @BeforeEach
    void setUp() {
        order = new WorkOrder("Jane Doe", "123 Main St, Minneapolis, MN", ServiceType.RESIDENTIAL, LocalDate.of(2026, 11, 20), NOW);
        technician = new Technician("Sam Tech", "sam@example.com");
    }

    @Test
    void newWorkOrderStartsOpen() {
        assertThat(order.getStatus()).isEqualTo(WorkOrderStatus.OPEN);
    }

    @Test
    void followsHappyPathToCompleted() {
        order.assignTo(technician);
        order.start();
        order.recordReading("ONT", -18.5, NOW);
        order.complete(NOW);

        assertThat(order.getStatus()).isEqualTo(WorkOrderStatus.COMPLETED);
        assertThat(order.getCompletedAt()).isEqualTo(NOW);
        assertThat(order.getTechnician()).isSameAs(technician);
    }

    @Test
    void cannotStartBeforeAssignment() {
        assertThatThrownBy(order::start)
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("from OPEN to IN_PROGRESS");
    }

    @Test
    void cannotAssignInactiveTechnician() {
        technician.deactivate();

        assertThatThrownBy(() -> order.assignTo(technician))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("inactive");
        assertThat(order.getStatus()).isEqualTo(WorkOrderStatus.OPEN);
    }

    @Test
    void cannotRecordReadingUnlessInProgress() {
        assertThatThrownBy(() -> order.recordReading("ONT", -18.0, NOW))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void cannotCompleteWithoutReadings() {
        order.assignTo(technician);
        order.start();

        assertThatThrownBy(() -> order.complete(NOW))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("at least one signal reading");
    }

    @Test
    void cannotCompleteWhenAReadingIsOutOfRange() {
        order.assignTo(technician);
        order.start();
        order.recordReading("ONT", -18.0, NOW);
        order.recordReading("Drop", -31.2, NOW);

        assertThatThrownBy(() -> order.complete(NOW))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("dBm");
        assertThat(order.getStatus()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
    }

    @Test
    void completedOrderCannotBeCancelled() {
        order.assignTo(technician);
        order.start();
        order.recordReading("ONT", -12.0, NOW);
        order.complete(NOW);

        assertThatThrownBy(order::cancel).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void readingPassesOnlyInsideAcceptableWindow() {
        order.assignTo(technician);
        order.start();

        assertThat(order.recordReading("a", -27.0, NOW).isPassing()).isTrue();
        assertThat(order.recordReading("b", -8.0, NOW).isPassing()).isTrue();
        assertThat(order.recordReading("c", -27.1, NOW).isPassing()).isFalse();
        assertThat(order.recordReading("d", -7.9, NOW).isPassing()).isFalse();
    }
}
