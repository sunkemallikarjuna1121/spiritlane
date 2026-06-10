package com.spiritlane.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

// ============================================================
// Cart Item Request
// ============================================================
public class CartItemRequest {
    @NotNull private Long inventoryId;
    @NotNull @Min(1) private Integer quantity;
	public Long getInventoryId() {
		return inventoryId;
	}
	public void setInventoryId(Long inventoryId) {
		this.inventoryId = inventoryId;
	}
	public Integer getQuantity() {
		return quantity;
	}
	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}
}
