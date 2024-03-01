package com.acheron.connector.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This is the response model which is specific for the Pretrained Video Object Tracking requests.
 * 
 * @author HariharanB
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PretrainedVideoResponse {
	private String className;
	private String segment;
	private String confidence;
}
