package com.yusufabdiqadir.fiberworkorders.technician;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class TechnicianDtos {

    private TechnicianDtos() {
    }

    public record CreateTechnicianRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 255) String email) {
    }

    public record TechnicianResponse(Long id, String name, String email, boolean active) {

        public static TechnicianResponse from(Technician technician) {
            return new TechnicianResponse(technician.getId(), technician.getName(), technician.getEmail(), technician.isActive());
        }
    }
}
