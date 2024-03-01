package com.acheron.connector.services;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.acheron.connector.model.request.CustomBatchPredictionRequest;
import com.acheron.connector.model.request.GetJobOutput;
import com.acheron.connector.model.response.GoogleCloudAIResponse;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.aiplatform.util.ValueConverter;
import com.google.cloud.aiplatform.v1.BatchPredictionJob;
import com.google.cloud.aiplatform.v1.BatchPredictionJobName;
import com.google.cloud.aiplatform.v1.GcsDestination;
import com.google.cloud.aiplatform.v1.GcsSource;
import com.google.cloud.aiplatform.v1.JobServiceClient;
import com.google.cloud.aiplatform.v1.JobServiceSettings;
import com.google.cloud.aiplatform.v1.JobState;
import com.google.cloud.aiplatform.v1.LocationName;
import com.google.cloud.aiplatform.v1.ModelName;
import com.google.protobuf.Value;

/**
 * This service class is responsible for performing Batch Predictions for a
 * custom pretrained model on GCP Vertex AI
 * 
 * @author HariharanB
 */
public class VertexService {
	public static final Logger LOGGER = LoggerFactory.getLogger(VertexService.class);
	public static final String LOCATIONFORMATTER = "%s-aiplatform.googleapis.com:443";

	/**
	 * This method is responsible for creating a Batch Prediction job for a custom
	 * trained model on Vertex AI. This intakes the parameters of the model, and
	 * path to the input jsonl file for making the predictions and outputs the
	 * result jsonl file in the given GCS output path.
	 * 
	 * @param vertexImpl  - The inputs from the connector
	 * @param credentials - Credentials to connect to GCP
	 * @return GoogleCloudAIResponse containing the batch prediction job ID
	 */
	public GoogleCloudAIResponse<String> performBatchPrediction(CustomBatchPredictionRequest vertexImpl,
			GoogleCredentials credentials) {
		GoogleCloudAIResponse<String> aiResponse = null;
		String endpoint = String.format(LOCATIONFORMATTER, vertexImpl.getLocation());
		String batchPredictionJobId = null;
		JobServiceSettings settings = null;
		BatchPredictionJob response = null;
		LOGGER.info("Building the job settings with the authentication to create a batch prediction job");
		try {
			settings = JobServiceSettings.newBuilder()
					.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).setEndpoint(endpoint).build();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (JobServiceClient client = JobServiceClient.create(settings)) {

			Value modelParameters = ValueConverter.EMPTY_VALUE;

			LOGGER.info("Intaking and applying the job and model configurations");
			GcsSource gcsSource = GcsSource.newBuilder().addUris(vertexImpl.getGcsSourceUri()).build();
			BatchPredictionJob.InputConfig inputConfig = BatchPredictionJob.InputConfig.newBuilder()
					.setInstancesFormat("jsonl").setGcsSource(gcsSource).build();

			GcsDestination gcsDestination = GcsDestination.newBuilder()
					.setOutputUriPrefix(vertexImpl.getGcsDestinationOutputUriPrefix()).build();

			BatchPredictionJob.OutputConfig outputConfig = BatchPredictionJob.OutputConfig.newBuilder()
					.setPredictionsFormat("jsonl").setGcsDestination(gcsDestination).build();

			String modelName = ModelName
					.of(vertexImpl.getProjectName(), vertexImpl.getLocation(), vertexImpl.getModelId()).toString();

			BatchPredictionJob batchPredictionJob = BatchPredictionJob.newBuilder()
					.setDisplayName(vertexImpl.getDisplayName()).setModel(modelName).setModelParameters(modelParameters)
					.setInputConfig(inputConfig).setOutputConfig(outputConfig).build();

			LocationName parent = LocationName.of(vertexImpl.getProjectName(), vertexImpl.getLocation());
			response = client.createBatchPredictionJob(parent, batchPredictionJob);

			batchPredictionJobId = response.getName().substring(response.getName().lastIndexOf("/") + 1);
		} catch (IOException e) {
			e.printStackTrace();
		}

		aiResponse = new GoogleCloudAIResponse<>(batchPredictionJobId);
		return aiResponse;
	}

	/**
	 * Retrieves the current state of a batch prediction job.
	 * 
	 * @param vertexImpl  Information about the job to query
	 * @param credentials Credentials to connect to GCP
	 * @return GoogleCloudAIResponse containing the state of the batch prediction
	 *         job
	 */
	public GoogleCloudAIResponse<String> getJobState(GetJobOutput vertexImpl, GoogleCredentials credentials) {
		GoogleCloudAIResponse<String> aiResponse = null;
		String endpoint = String.format(LOCATIONFORMATTER, vertexImpl.getLocation());
		JobServiceSettings jobServiceSettings = null;
		JobState jobState = null;
		try {
			jobServiceSettings = JobServiceSettings.newBuilder()
					.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).setEndpoint(endpoint).build();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (JobServiceClient jobServiceClient = JobServiceClient.create(jobServiceSettings)) {
			BatchPredictionJobName batchPredictionJobName = BatchPredictionJobName.of(vertexImpl.getProjectName(),
					vertexImpl.getLocation(), vertexImpl.getBatchPredictionJobId());
			BatchPredictionJob batchPredictionJob = jobServiceClient.getBatchPredictionJob(batchPredictionJobName);
			jobState = batchPredictionJob.getState();

		} catch (IOException e) {
			e.printStackTrace();
		}
		if (jobState != null) {
			aiResponse = new GoogleCloudAIResponse<>(jobState.name());
		}
		return aiResponse;
	}

	/**
	 * Retrieves the GCS URI of the completed batch prediction job output.
	 * 
	 * @param vertexImpl  Information about the job to query
	 * @param credentials Credentials to connect to GCP
	 * @return GoogleCloudAIResponse containing the GCS URI of the job output
	 */
	public GoogleCloudAIResponse<String> getGcsUri(GetJobOutput vertexImpl, GoogleCredentials credentials) {
		String gcsUri = null;
		GoogleCloudAIResponse<String> aiResponse = null;

		String jobState = getJobState(vertexImpl, credentials).getResponse();
		LOGGER.info(jobState);

		do {
			try {
				Thread.sleep(25000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				e.printStackTrace();
			}
			jobState = getJobState(vertexImpl, credentials).getResponse();
			LOGGER.info(jobState);
			if (jobState.equals("JOB_STATE_SUCCEEDED")) {
				gcsUri = getUri(vertexImpl, credentials) + "/predictions_00001.jsonl";
				break;
			}
		} while (jobState.equals("JOB_STATE_PENDING") || jobState.equals("JOB_STATE_RUNNING"));

		if (!jobState.equals("JOB_STATE_SUCCEEDED")) {
			LOGGER.info("URI can\'t be fetched - {}.", jobState);
		}

		aiResponse = new GoogleCloudAIResponse<>(gcsUri);
		return aiResponse;
	}

	/**
	 * Retrieves the GCS URI of the job output.
	 * 
	 * @param vertexImpl  Information about the job to query
	 * @param credentials Credentials to connect to GCP
	 * @return The GCS URI of the job output
	 */
	public String getUri(GetJobOutput vertexImpl, GoogleCredentials credentials) {
		JobServiceSettings jobServiceSettings = null;
		BatchPredictionJob batchPredictionJob = null;
		String endpoint = String.format(LOCATIONFORMATTER, vertexImpl.getLocation());
		String response = null;
		try {
			jobServiceSettings = JobServiceSettings.newBuilder()
					.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).setEndpoint(endpoint).build();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (JobServiceClient jobServiceClient = JobServiceClient.create(jobServiceSettings)) {
			BatchPredictionJobName batchPredictionJobName = BatchPredictionJobName.of(vertexImpl.getProjectName(),
					vertexImpl.getLocation(), vertexImpl.getBatchPredictionJobId());

			batchPredictionJob = jobServiceClient.getBatchPredictionJob(batchPredictionJobName);
			LOGGER.info(batchPredictionJob.getOutputInfo().getGcsOutputDirectory());
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (batchPredictionJob != null) {
			response = batchPredictionJob.getOutputInfo().getGcsOutputDirectory();
		}
		return response;
	}
}
