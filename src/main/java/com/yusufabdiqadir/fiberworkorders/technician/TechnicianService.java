package com.yusufabdiqadir.fiberworkorders.technician;

import com.yusufabdiqadir.fiberworkorders.common.InvalidStateException;
import com.yusufabdiqadir.fiberworkorders.common.NotFoundException;
import com.yusufabdiqadir.fiberworkorders.technician.TechnicianDtos.CreateTechnicianRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TechnicianService {

    private final TechnicianRepository technicians;

    public TechnicianService(TechnicianRepository technicians) {
        this.technicians = technicians;
    }

    @Transactional
    public Technician create(CreateTechnicianRequest request) {
        String email = request.email().trim().toLowerCase();
        if (technicians.existsByEmailIgnoreCase(email)) {
            throw new InvalidStateException("A technician with email " + email + " already exists");
        }
        return technicians.save(new Technician(request.name().trim(), email));
    }

    public Technician get(Long id) {
        return technicians.findById(id).orElseThrow(() -> new NotFoundException("Technician", id));
    }

    public List<Technician> list() {
        return technicians.findAll();
    }

    @Transactional
    public Technician deactivate(Long id) {
        Technician technician = get(id);
        technician.deactivate();
        return technician;
    }
}
