
package com.acheron.connector.model.request;

import com.acheron.connector.model.AuthenticationData;
import com.acheron.connector.model.Operation;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleCloudAIRequest {

	@Valid
	private AuthenticationData authentication;

	@Valid
	private Operation operation;

	@Valid
	private AIRequestClass data;

}
