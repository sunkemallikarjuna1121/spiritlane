package com.spiritlane.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "delivery_agent_details")
public class DeliveryAgentDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false, unique = true)
    private User agent;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type")
    private VehicleType vehicleType = VehicleType.BIKE;

    @Column(name = "vehicle_number", length = 20)
    private String vehicleNumber;

    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Column(name = "is_available")
    private Boolean isAvailable = true;

    @Column(name = "current_lat", precision = 10, scale = 8)
    private BigDecimal currentLat;

    @Column(name = "current_lng", precision = 11, scale = 8)
    private BigDecimal currentLng;

    @Column(name = "total_deliveries")
    private Integer totalDeliveries = 0;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    public enum VehicleType {
        BIKE, SCOOTER, CAR
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getAgent() {
		return agent;
	}

	public void setAgent(User agent) {
		this.agent = agent;
	}

	public VehicleType getVehicleType() {
		return vehicleType;
	}

	public void setVehicleType(VehicleType vehicleType) {
		this.vehicleType = vehicleType;
	}

	public String getVehicleNumber() {
		return vehicleNumber;
	}

	public void setVehicleNumber(String vehicleNumber) {
		this.vehicleNumber = vehicleNumber;
	}

	public String getLicenseNumber() {
		return licenseNumber;
	}

	public void setLicenseNumber(String licenseNumber) {
		this.licenseNumber = licenseNumber;
	}

	public Boolean getIsAvailable() {
		return isAvailable;
	}

	public void setIsAvailable(Boolean isAvailable) {
		this.isAvailable = isAvailable;
	}

	public BigDecimal getCurrentLat() {
		return currentLat;
	}

	public void setCurrentLat(BigDecimal currentLat) {
		this.currentLat = currentLat;
	}

	public BigDecimal getCurrentLng() {
		return currentLng;
	}

	public void setCurrentLng(BigDecimal currentLng) {
		this.currentLng = currentLng;
	}

	public Integer getTotalDeliveries() {
		return totalDeliveries;
	}

	public void setTotalDeliveries(Integer totalDeliveries) {
		this.totalDeliveries = totalDeliveries;
	}

	public BigDecimal getRating() {
		return rating;
	}

	public void setRating(BigDecimal rating) {
		this.rating = rating;
	}
    
}
