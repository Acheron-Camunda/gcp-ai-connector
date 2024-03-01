package com.acheron.connector.model.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This is the request model which is specific for the Image Labeling and Video
 * Object Tracking requests using pretrained models on the GCP Vision AI
 * platform.
 * 
 * @author HariharanB
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PretrainedObjectDetectionRequest {
	@Valid
	private String testFilePath;
	@Valid
	private String bucketName;
	@Valid
	private String pathInBucket;
}
