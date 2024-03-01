package com.acheron.connector.model.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AIRequestClass {

	@Valid
	private PromptRequest prompt;

	@Valid
	private PretrainedObjectDetectionRequest pretrainedDetection;

	@Valid
	private CustomBatchPredictionRequest customPrediction;

	@Valid
	private GetJobOutput jobOutput;

}
