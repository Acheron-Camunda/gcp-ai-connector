package com.acheron.connector.model.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PromptRequest {
	@Valid
	private String promptInput;
	@Valid
	private String temperature;
	@Valid
	private String maxOutputTokens;
	@Valid
	private String projectId;
	@Valid
	private String location;
	@Valid
	private String model;
}
