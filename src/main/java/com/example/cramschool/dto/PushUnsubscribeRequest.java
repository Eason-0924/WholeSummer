package com.example.cramschool.dto;

import jakarta.validation.constraints.NotBlank;

public class PushUnsubscribeRequest {

	@NotBlank
	private String endpoint;

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
}
