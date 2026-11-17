package com.yusufabdiqadir.fiberworkorders.workorder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Optical power measured at a point on the install (for example at the ONT).
 * A reading passes when the received power falls inside the acceptable window.
 */
@Entity
@Table(name = "signal_readings")
public class SignalReading {

    public static final double MIN_ACCEPTABLE_DBM = -27.0;
    public static final double MAX_ACCEPTABLE_DBM = -8.0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private double receivedPowerDbm;

    @Column(nullable = false)
    private Instant recordedAt;

    protected SignalReading() {
        // required by JPA
    }

    SignalReading(WorkOrder workOrder, String location, double receivedPowerDbm, Instant recordedAt) {
        this.workOrder = workOrder;
        this.location = location;
        this.receivedPowerDbm = receivedPowerDbm;
        this.recordedAt = recordedAt;
    }

    public boolean isPassing() {
        return receivedPowerDbm >= MIN_ACCEPTABLE_DBM && receivedPowerDbm <= MAX_ACCEPTABLE_DBM;
    }

    public Long getId() {
        return id;
    }

    public String getLocation() {
        return location;
    }

    public double getReceivedPowerDbm() {
        return receivedPowerDbm;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
