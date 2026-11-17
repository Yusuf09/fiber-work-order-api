package com.yusufabdiqadir.fiberworkorders.workorder;

import com.yusufabdiqadir.fiberworkorders.common.NotFoundException;
import com.yusufabdiqadir.fiberworkorders.technician.Technician;
import com.yusufabdiqadir.fiberworkorders.technician.TechnicianService;
import com.yusufabdiqadir.fiberworkorders.workorder.WorkOrderDtos.CreateWorkOrderRequest;
import com.yusufabdiqadir.fiberworkorders.workorder.WorkOrderDtos.SummaryReport;
import com.yusufabdiqadir.fiberworkorders.workorder.WorkOrderDtos.WorkOrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-16T15:00:00Z");

    @Mock
    private WorkOrderRepository workOrders;

    @Mock
    private TechnicianService technicianService;

    private WorkOrderService service;

    @BeforeEach
    void setUp() {
        service = new WorkOrderService(workOrders, technicianService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createTrimsInputAndStampsCreationTime() {
        when(workOrders.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkOrderResponse response = service.create(new CreateWorkOrderRequest(
                "  Jane Doe ", " 123 Main St ", ServiceType.BUSINESS, LocalDate.of(2026, 12, 1)));

        ArgumentCaptor<WorkOrder> saved = ArgumentCaptor.forClass(WorkOrder.class);
        verify(workOrders).save(saved.capture());
        assertThat(saved.getValue().getCustomerName()).isEqualTo("Jane Doe");
        assertThat(saved.getValue().getAddress()).isEqualTo("123 Main St");
        assertThat(response.status()).isEqualTo(WorkOrderStatus.OPEN);
        assertThat(response.createdAt()).isEqualTo(NOW);
    }

    @Test
    void assignLooksUpTechnicianAndUpdatesOrder() {
        WorkOrder order = new WorkOrder("Jane Doe", "123 Main St", ServiceType.RESIDENTIAL, LocalDate.of(2026, 12, 1), NOW);
        Technician technician = new Technician("Sam Tech", "sam@example.com");
        when(workOrders.findById(1L)).thenReturn(Optional.of(order));
        when(technicianService.get(7L)).thenReturn(technician);

        WorkOrderResponse response = service.assign(1L, 7L);

        assertThat(response.status()).isEqualTo(WorkOrderStatus.ASSIGNED);
        assertThat(response.technician().name()).isEqualTo("Sam Tech");
    }

    @Test
    void missingWorkOrderThrowsNotFound() {
        when(workOrders.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Work order 99 was not found");
    }

    @Test
    void summaryIncludesEveryStatusEvenWhenZero() {
        when(workOrders.countByStatus()).thenReturn(List.of(
                count(WorkOrderStatus.OPEN, 3),
                count(WorkOrderStatus.COMPLETED, 2)));

        SummaryReport report = service.summary();

        assertThat(report.totalWorkOrders()).isEqualTo(5);
        assertThat(report.byStatus()).containsEntry(WorkOrderStatus.OPEN, 3L)
                .containsEntry(WorkOrderStatus.COMPLETED, 2L)
                .containsEntry(WorkOrderStatus.CANCELLED, 0L)
                .hasSize(WorkOrderStatus.values().length);
    }

    private static WorkOrderRepository.StatusCount count(WorkOrderStatus status, long total) {
        return new WorkOrderRepository.StatusCount() {
            @Override
            public WorkOrderStatus getStatus() {
                return status;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }
}
