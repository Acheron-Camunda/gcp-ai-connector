package com.acheron.connector.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetJobOutput {
	private String projectName;
	private String location;
	private String batchPredictionJobId;
}
