package com.yusufabdiqadir.fiberworkorders.technician;

import com.yusufabdiqadir.fiberworkorders.technician.TechnicianDtos.CreateTechnicianRequest;
import com.yusufabdiqadir.fiberworkorders.technician.TechnicianDtos.TechnicianResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/technicians")
public class TechnicianController {

    private final TechnicianService service;

    public TechnicianController(TechnicianService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<TechnicianResponse> create(@Valid @RequestBody CreateTechnicianRequest request) {
        Technician technician = service.create(request);
        return ResponseEntity.created(URI.create("/api/technicians/" + technician.getId()))
                .body(TechnicianResponse.from(technician));
    }

    @GetMapping
    List<TechnicianResponse> list() {
        return service.list().stream().map(TechnicianResponse::from).toList();
    }

    @GetMapping("/{id}")
    TechnicianResponse get(@PathVariable Long id) {
        return TechnicianResponse.from(service.get(id));
    }

    @PostMapping("/{id}/deactivate")
    TechnicianResponse deactivate(@PathVariable Long id) {
        return TechnicianResponse.from(service.deactivate(id));
    }
}
