package com.acheron.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.acheron.connector.model.OperationType;
import com.acheron.connector.model.request.GoogleCloudAIRequest;
import com.acheron.connector.model.response.GoogleCloudAIResponse;
import com.acheron.connector.services.PromptService;
import com.acheron.connector.services.VertexService;
import com.acheron.connector.services.VisionService;
import com.acheron.connector.util.GCAIUtil;
import com.google.auth.oauth2.GoogleCredentials;

import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;

@OutboundConnector(name = "GoogleCloudAI", inputVariables = { "operation", "data",
		"authentication" }, type = "com.acheron.connector:gcai:1")
public class GoogleCloudAIFunction implements OutboundConnectorFunction {

	public static final Logger LOGGER = LoggerFactory.getLogger(GoogleCloudAIFunction.class);

	@Override
	public Object execute(OutboundConnectorContext context) throws Exception {
		final var connectorRequest = context.bindVariables(GoogleCloudAIRequest.class);

		LOGGER.info("Connector Request{}", connectorRequest);

		String jsonFilePath = connectorRequest.getAuthentication().getJsonFileAsString();
		GCAIUtil gcaiUtil = new GCAIUtil();
		GoogleCredentials credentials = gcaiUtil.buildCredentials(jsonFilePath);

		return executeConnector(connectorRequest, credentials);
	}

	private GoogleCloudAIResponse<?> executeConnector(final GoogleCloudAIRequest connectorRequest,
			GoogleCredentials credentials) {

		GoogleCloudAIResponse<?> response = null;
		VisionService visionService = new VisionService();
		VertexService vertexService = new VertexService();
		PromptService promptService = new PromptService();

		// Based on If condition with operation sent from connector request we can
		// choose what to be called and done
		if (connectorRequest.getOperation().getOperationType().equals(OperationType.PROMPT.name())) {
			response = promptService.executePrompt(connectorRequest.getData().getPrompt(), credentials);

		} else if (connectorRequest.getOperation().getOperationType().equals(OperationType.PRETRAINEDIMAGE.name())) {
			response = visionService.pretrainedObjectDetection(connectorRequest.getData().getPretrainedDetection(),
					credentials);
		} else if (connectorRequest.getOperation().getOperationType().equals(OperationType.PRETRAINEDVIDEO.name())) {
			response = visionService.pretrainedObjectDetectionVideo(connectorRequest.getData().getPretrainedDetection(),
					credentials);
		} else if (connectorRequest.getOperation().getOperationType().equals(OperationType.CUSTOMBATCH.name())) {
			response = vertexService.performBatchPrediction(connectorRequest.getData().getCustomPrediction(),
					credentials);
		} else if (connectorRequest.getOperation().getOperationType().equals(OperationType.GETJOBSTATE.name())) {
			response = vertexService.getJobState(connectorRequest.getData().getJobOutput(), credentials);
		} else if (connectorRequest.getOperation().getOperationType().equals(OperationType.GETURI.name())) {
			response = vertexService.getGcsUri(connectorRequest.getData().getJobOutput(), credentials);
		}

		LOGGER.info("Response {}", response);
		return response;

	}
}
