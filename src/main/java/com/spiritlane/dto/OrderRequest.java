package com.spiritlane.dto;

import jakarta.validation.constraints.NotNull;

public class OrderRequest {

    @NotNull(message = "Delivery address is required")
    private Long addressId;

    private String couponCode;

    public Long getAddressId() {
		return addressId;
	}

	public void setAddressId(Long addressId) {
		this.addressId = addressId;
	}

	public String getCouponCode() {
		return couponCode;
	}

	public void setCouponCode(String couponCode) {
		this.couponCode = couponCode;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	private String notes;
}
