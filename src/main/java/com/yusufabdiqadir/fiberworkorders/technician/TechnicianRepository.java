package com.yusufabdiqadir.fiberworkorders.technician;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    boolean existsByEmailIgnoreCase(String email);
}
