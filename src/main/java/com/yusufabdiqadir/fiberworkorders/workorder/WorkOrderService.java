package com.yusufabdiqadir.fiberworkorders.workorder;

import com.yusufabdiqadir.fiberworkorders.common.NotFoundException;
import com.yusufabdiqadir.fiberworkorders.technician.Technician;
import com.yusufabdiqadir.fiberworkorders.technician.TechnicianService;
import com.yusufabdiqadir.fiberworkorders.workorder.WorkOrderDtos.CreateWorkOrderRequest;
import com.yusufabdiqadir.fiberworkorders.workorder.WorkOrderDtos.PageResponse;
import com.yusufabdiqadir.fiberworkorders.workorder.WorkOrderDtos.ReadingResponse;
import com.yusufabdiqadir.fiberworkorders.workorder.WorkOrderDtos.RecordReadingRequest;
import com.yusufabdiqadir.fiberworkorders.workorder.WorkOrderDtos.SummaryReport;
import com.yusufabdiqadir.fiberworkorders.workorder.WorkOrderDtos.WorkOrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

/**
 * Application service for work orders. Each public method is one transaction and returns
 * DTOs, so lazy JPA relationships are never touched outside a transaction.
 */
@Service
@Transactional(readOnly = true)
public class WorkOrderService {

    private final WorkOrderRepository workOrders;
    private final TechnicianService technicianService;
    private final Clock clock;

    public WorkOrderService(WorkOrderRepository workOrders, TechnicianService technicianService, Clock clock) {
        this.workOrders = workOrders;
        this.technicianService = technicianService;
        this.clock = clock;
    }

    @Transactional
    public WorkOrderResponse create(CreateWorkOrderRequest request) {
        WorkOrder order = new WorkOrder(
                request.customerName().trim(),
                request.address().trim(),
                request.serviceType(),
                request.scheduledDate(),
                Instant.now(clock));
        return WorkOrderResponse.from(workOrders.save(order));
    }

    public WorkOrderResponse get(Long id) {
        return WorkOrderResponse.from(find(id));
    }

    public PageResponse<WorkOrderResponse> search(WorkOrderStatus status, Long technicianId, Pageable pageable) {
        Page<WorkOrder> page = workOrders.search(status, technicianId, pageable);
        return new PageResponse<>(
                page.getContent().stream().map(WorkOrderResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional
    public WorkOrderResponse assign(Long id, Long technicianId) {
        WorkOrder order = find(id);
        Technician technician = technicianService.get(technicianId);
        order.assignTo(technician);
        return WorkOrderResponse.from(order);
    }

    @Transactional
    public WorkOrderResponse start(Long id) {
        WorkOrder order = find(id);
        order.start();
        return WorkOrderResponse.from(order);
    }

    @Transactional
    public ReadingResponse recordReading(Long id, RecordReadingRequest request) {
        WorkOrder order = find(id);
        SignalReading reading = order.recordReading(request.location().trim(), request.receivedPowerDbm(), Instant.now(clock));
        workOrders.flush(); // assigns the reading's generated id before it is returned
        return ReadingResponse.from(reading);
    }

    @Transactional
    public WorkOrderResponse complete(Long id) {
        WorkOrder order = find(id);
        order.complete(Instant.now(clock));
        return WorkOrderResponse.from(order);
    }

    @Transactional
    public WorkOrderResponse cancel(Long id) {
        WorkOrder order = find(id);
        order.cancel();
        return WorkOrderResponse.from(order);
    }

    public SummaryReport summary() {
        Map<WorkOrderStatus, Long> byStatus = new EnumMap<>(WorkOrderStatus.class);
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            byStatus.put(status, 0L);
        }
        workOrders.countByStatus().forEach(count -> byStatus.put(count.getStatus(), count.getTotal()));
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return new SummaryReport(total, byStatus);
    }

    private WorkOrder find(Long id) {
        return workOrders.findById(id).orElseThrow(() -> new NotFoundException("Work order", id));
    }
}
