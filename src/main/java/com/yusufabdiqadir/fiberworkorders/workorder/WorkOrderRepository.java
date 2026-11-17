package com.yusufabdiqadir.fiberworkorders.workorder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    @Query("""
            select w from WorkOrder w
            where (:status is null or w.status = :status)
              and (:technicianId is null or w.technician.id = :technicianId)
            """)
    Page<WorkOrder> search(WorkOrderStatus status, Long technicianId, Pageable pageable);

    @Query("select w.status as status, count(w) as total from WorkOrder w group by w.status")
    List<StatusCount> countByStatus();

    interface StatusCount {
        WorkOrderStatus getStatus();

        long getTotal();
    }
}
