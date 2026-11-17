package com.yusufabdiqadir.fiberworkorders.workorder;

import com.yusufabdiqadir.fiberworkorders.technician.Technician;
import com.yusufabdiqadir.fiberworkorders.technician.TechnicianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class WorkOrderRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-11-16T15:00:00Z");

    @Autowired
    private WorkOrderRepository workOrders;

    @Autowired
    private TechnicianRepository technicians;

    private Technician sam;

    @BeforeEach
    void setUp() {
        sam = technicians.save(new Technician("Sam Tech", "sam@example.com"));
        Technician alex = technicians.save(new Technician("Alex Tech", "alex@example.com"));

        workOrders.save(order("Open order"));

        WorkOrder assignedToSam = order("Sam's order");
        assignedToSam.assignTo(sam);
        workOrders.save(assignedToSam);

        WorkOrder assignedToAlex = order("Alex's order");
        assignedToAlex.assignTo(alex);
        workOrders.save(assignedToAlex);
    }

    @Test
    void searchWithNoFiltersReturnsEverything() {
        assertThat(workOrders.search(null, null, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(3);
    }

    @Test
    void searchFiltersByStatusAndTechnician() {
        Page<WorkOrder> page = workOrders.search(WorkOrderStatus.ASSIGNED, sam.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(WorkOrder::getCustomerName).containsExactly("Sam's order");
    }

    @Test
    void countByStatusGroupsOrders() {
        assertThat(workOrders.countByStatus())
                .extracting(WorkOrderRepository.StatusCount::getStatus, WorkOrderRepository.StatusCount::getTotal)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(WorkOrderStatus.OPEN, 1L),
                        org.assertj.core.groups.Tuple.tuple(WorkOrderStatus.ASSIGNED, 2L));
    }

    private static WorkOrder order(String customer) {
        return new WorkOrder(customer, "1 Test Ave", ServiceType.RESIDENTIAL, LocalDate.of(2026, 12, 1), NOW);
    }
}
