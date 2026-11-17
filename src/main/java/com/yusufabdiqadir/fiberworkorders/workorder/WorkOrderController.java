package com.yusufabdiqadir.fiberworkorders.workorder;

import com.yusufabdiqadir.fiberworkorders.workorder.WorkOrderDtos.AssignRequest;
import com.yusufabdiqadir.fiberworkorders.workorder.WorkOrderDtos.CreateWorkOrderRequest;
import com.yusufabdiqadir.fiberworkorders.workorder.WorkOrderDtos.PageResponse;
import com.yusufabdiqadir.fiberworkorders.workorder.WorkOrderDtos.ReadingResponse;
import com.yusufabdiqadir.fiberworkorders.workorder.WorkOrderDtos.RecordReadingRequest;
import com.yusufabdiqadir.fiberworkorders.workorder.WorkOrderDtos.SummaryReport;
import com.yusufabdiqadir.fiberworkorders.workorder.WorkOrderDtos.WorkOrderResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private static final int MAX_PAGE_SIZE = 100;

    private final WorkOrderService service;

    public WorkOrderController(WorkOrderService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<WorkOrderResponse> create(@Valid @RequestBody CreateWorkOrderRequest request) {
        WorkOrderResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/work-orders/" + created.id())).body(created);
    }

    @GetMapping
    PageResponse<WorkOrderResponse> search(
            @RequestParam(required = false) WorkOrderStatus status,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE),
                Sort.by("scheduledDate").ascending().and(Sort.by("id")));
        return service.search(status, technicianId, pageable);
    }

    @GetMapping("/{id}")
    WorkOrderResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/assign")
    WorkOrderResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request) {
        return service.assign(id, request.technicianId());
    }

    @PostMapping("/{id}/start")
    WorkOrderResponse start(@PathVariable Long id) {
        return service.start(id);
    }

    @PostMapping("/{id}/readings")
    ResponseEntity<ReadingResponse> recordReading(@PathVariable Long id, @Valid @RequestBody RecordReadingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.recordReading(id, request));
    }

    @PostMapping("/{id}/complete")
    WorkOrderResponse complete(@PathVariable Long id) {
        return service.complete(id);
    }

    @PostMapping("/{id}/cancel")
    WorkOrderResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    @GetMapping("/summary")
    SummaryReport summary() {
        return service.summary();
    }
}
