package com.acheron.connector.model.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This is the request model which is specific for the Batch Prediction requests
 * on a custom trained model on the GCP Vertex AI platform.
 * 
 * @author HariharanB
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomBatchPredictionRequest {
	@Valid
	private String projectName;

	@Valid
	private String displayName;

	@Valid
	private String modelId;

	@Valid
	private String gcsSourceUri;

	@Valid
	private String gcsDestinationOutputUriPrefix;

	@Valid
	private String location;
}
