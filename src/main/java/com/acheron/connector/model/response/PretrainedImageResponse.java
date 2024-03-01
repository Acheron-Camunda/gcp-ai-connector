package com.acheron.connector.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This is the response model which is specific for the Pretrained Image Labeling requests.
 * 
 * @author HariharanB
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PretrainedImageResponse {
	private String description;
	private float score;
}
