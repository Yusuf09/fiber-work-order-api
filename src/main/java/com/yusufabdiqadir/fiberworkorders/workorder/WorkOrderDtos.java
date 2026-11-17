package com.yusufabdiqadir.fiberworkorders.workorder;

import com.yusufabdiqadir.fiberworkorders.technician.Technician;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class WorkOrderDtos {

    private WorkOrderDtos() {
    }

    public record CreateWorkOrderRequest(
            @NotBlank @Size(max = 100) String customerName,
            @NotBlank @Size(max = 255) String address,
            @NotNull ServiceType serviceType,
            @NotNull @FutureOrPresent LocalDate scheduledDate) {
    }

    public record AssignRequest(@NotNull Long technicianId) {
    }

    public record RecordReadingRequest(
            @NotBlank @Size(max = 100) String location,
            @NotNull @DecimalMin("-60.0") @DecimalMax("10.0") Double receivedPowerDbm) {
    }

    public record ReadingResponse(Long id, String location, double receivedPowerDbm, boolean passing, Instant recordedAt) {

        public static ReadingResponse from(SignalReading reading) {
            return new ReadingResponse(reading.getId(), reading.getLocation(), reading.getReceivedPowerDbm(),
                    reading.isPassing(), reading.getRecordedAt());
        }
    }

    public record TechnicianSummary(Long id, String name) {

        static TechnicianSummary from(Technician technician) {
            return technician == null ? null : new TechnicianSummary(technician.getId(), technician.getName());
        }
    }

    public record WorkOrderResponse(
            Long id,
            String customerName,
            String address,
            ServiceType serviceType,
            WorkOrderStatus status,
            LocalDate scheduledDate,
            TechnicianSummary technician,
            List<ReadingResponse> readings,
            Instant createdAt,
            Instant completedAt) {

        public static WorkOrderResponse from(WorkOrder order) {
            return new WorkOrderResponse(
                    order.getId(),
                    order.getCustomerName(),
                    order.getAddress(),
                    order.getServiceType(),
                    order.getStatus(),
                    order.getScheduledDate(),
                    TechnicianSummary.from(order.getTechnician()),
                    order.getReadings().stream().map(ReadingResponse::from).toList(),
                    order.getCreatedAt(),
                    order.getCompletedAt());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }

    public record SummaryReport(long totalWorkOrders, Map<WorkOrderStatus, Long> byStatus) {
    }
}
