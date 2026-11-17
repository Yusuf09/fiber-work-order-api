package com.yusufabdiqadir.fiberworkorders.workorder;

import com.yusufabdiqadir.fiberworkorders.common.InvalidStateException;
import com.yusufabdiqadir.fiberworkorders.technician.Technician;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A fiber installation job. All state changes go through methods on this class so the
 * business rules (valid transitions, active technicians, passing readings) live with the data.
 */
@Entity
@Table(name = "work_orders")
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkOrderStatus status = WorkOrderStatus.OPEN;

    @Column(nullable = false)
    private LocalDate scheduledDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Technician technician;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("recordedAt ASC")
    private List<SignalReading> readings = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant completedAt;

    @Version
    private long version;

    protected WorkOrder() {
        // required by JPA
    }

    public WorkOrder(String customerName, String address, ServiceType serviceType, LocalDate scheduledDate, Instant createdAt) {
        this.customerName = customerName;
        this.address = address;
        this.serviceType = serviceType;
        this.scheduledDate = scheduledDate;
        this.createdAt = createdAt;
    }

    public void assignTo(Technician technician) {
        if (!technician.isActive()) {
            throw new InvalidStateException("Technician " + technician.getId() + " is inactive and cannot be assigned");
        }
        moveTo(WorkOrderStatus.ASSIGNED);
        this.technician = technician;
    }

    public void start() {
        moveTo(WorkOrderStatus.IN_PROGRESS);
    }

    public SignalReading recordReading(String location, double receivedPowerDbm, Instant recordedAt) {
        if (status != WorkOrderStatus.IN_PROGRESS) {
            throw new InvalidStateException("Readings can only be recorded while the work order is IN_PROGRESS (current: " + status + ")");
        }
        SignalReading reading = new SignalReading(this, location, receivedPowerDbm, recordedAt);
        readings.add(reading);
        return reading;
    }

    public void complete(Instant completedAt) {
        if (readings.isEmpty()) {
            throw new InvalidStateException("A work order needs at least one signal reading before it can be completed");
        }
        if (!allReadingsPass()) {
            throw new InvalidStateException("All signal readings must be between "
                    + SignalReading.MIN_ACCEPTABLE_DBM + " and " + SignalReading.MAX_ACCEPTABLE_DBM + " dBm to complete");
        }
        moveTo(WorkOrderStatus.COMPLETED);
        this.completedAt = completedAt;
    }

    public void cancel() {
        moveTo(WorkOrderStatus.CANCELLED);
    }

    public boolean allReadingsPass() {
        return readings.stream().allMatch(SignalReading::isPassing);
    }

    private void moveTo(WorkOrderStatus next) {
        if (!status.canMoveTo(next)) {
            throw new InvalidStateException("Cannot move work order " + id + " from " + status + " to " + next);
        }
        this.status = next;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getAddress() {
        return address;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public WorkOrderStatus getStatus() {
        return status;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public Technician getTechnician() {
        return technician;
    }

    public List<SignalReading> getReadings() {
        return Collections.unmodifiableList(readings);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
