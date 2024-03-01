package com.acheron.connector.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.acheron.connector.exception.CouldNotProcessPromptException;
import com.acheron.connector.model.request.PromptRequest;
import com.acheron.connector.model.response.GoogleCloudAIResponse;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.aiplatform.v1beta1.EndpointName;
import com.google.cloud.aiplatform.v1beta1.PredictResponse;
import com.google.cloud.aiplatform.v1beta1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1beta1.PredictionServiceSettings;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;

/**
 * This class provides functionality for interacting with prediction services of
 * GCP Vertex AI and executing prompts.
 * 
 * @author PraveenKumarReddy
 */
public class PromptService {
	/**
	 * Builds PredictionServiceSettings for interacting with the prediction service.
	 *
	 * @param credentials GoogleCredentials for authentication.
	 * @param endpoint    The endpoint URL for the prediction service.
	 * @return PredictionServiceSettings for the prediction service.
	 */
	public PredictionServiceSettings buildPredictionServiceSettings(GoogleCredentials credentials, String endpoint) {

		PredictionServiceSettings predictionServiceSettings = null;

		try {
			predictionServiceSettings = PredictionServiceSettings.newBuilder()
					.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).setEndpoint(endpoint).build();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return predictionServiceSettings;
	}

	/**
	 * Executes a prompt using the provided PromptRequest and GoogleCredentials.
	 *
	 * @param promptRequest The request containing prompt-related information.
	 * @param credentials   GoogleCredentials for authentication.
	 * @return GoogleCloudAIResponse containing the response to the executed prompt.
	 */
	public GoogleCloudAIResponse<String> executePrompt(PromptRequest promptRequest, GoogleCredentials credentials) {
		String endpoint = String.format("%s-aiplatform.googleapis.com:443", promptRequest.getLocation());

		String response = null;

		try (PredictionServiceClient predictionServiceClient = PredictionServiceClient
				.create(buildPredictionServiceSettings(credentials, endpoint))) {

			String projectId = promptRequest.getProjectId();
			String promptInput = promptRequest.getPromptInput();
			String temperature = promptRequest.getTemperature();
			String maxOutputTokens = promptRequest.getMaxOutputTokens();
			String location = promptRequest.getLocation();
			String model = promptRequest.getModel();
			String publisher = "google";
			String instance = "{ \"prompt\": \"" + promptInput + "\"}";
			String parameters = "{\n" + "  \"temperature\": " + temperature + ",\n" + "  \"maxOutputTokens\": "
					+ maxOutputTokens + "\n" + "}";

			final EndpointName endpointName = EndpointName.ofProjectLocationPublisherModelName(projectId, location,
					publisher, model);

			// Initialize client that will be used to send requests. This client only needs
			// to be created
			// once, and can be reused for multiple requests.
			Value.Builder instanceValue = Value.newBuilder();
			JsonFormat.parser().merge(instance, instanceValue);
			List<Value> instances = new ArrayList<>();
			instances.add(instanceValue.build());

			// Use Value.Builder to convert instance to a dynamically typed value that can
			// be
			// processed by the service.
			Value.Builder parameterValueBuilder = Value.newBuilder();
			JsonFormat.parser().merge(parameters, parameterValueBuilder);
			Value parameterValue = parameterValueBuilder.build();

			PredictResponse predictResponse = predictionServiceClient.predict(endpointName, instances, parameterValue);

			response = predictResponse.getPredictions(0).getStructValue().getFieldsOrThrow("content").getStringValue();

			if (response.isBlank()) {
				throw new CouldNotProcessPromptException("Could Not Process the Requested Prompt");
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return new GoogleCloudAIResponse<>(response);
	}
}
