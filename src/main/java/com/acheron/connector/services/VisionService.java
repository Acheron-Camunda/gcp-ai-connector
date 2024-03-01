package com.acheron.connector.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.acheron.connector.model.request.PretrainedObjectDetectionRequest;
import com.acheron.connector.model.response.GoogleCloudAIResponse;
import com.acheron.connector.model.response.PretrainedImageResponse;
import com.acheron.connector.model.response.PretrainedVideoResponse;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.videointelligence.v1.AnnotateVideoProgress;
import com.google.cloud.videointelligence.v1.AnnotateVideoRequest;
import com.google.cloud.videointelligence.v1.AnnotateVideoResponse;
import com.google.cloud.videointelligence.v1.ObjectTrackingAnnotation;
import com.google.cloud.videointelligence.v1.VideoAnnotationResults;
import com.google.cloud.videointelligence.v1.VideoIntelligenceServiceClient;
import com.google.cloud.videointelligence.v1.VideoIntelligenceServiceSettings;
import com.google.cloud.videointelligence.v1.VideoSegment;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;

/**
 * This service class is responsible for performing Image Annotation for an
 * uploaded image and Object Tracking for an uploaded video using Google Vision
 * AI service.
 * 
 * @author HariharanB
 */
public class VisionService {
	public static final Logger LOGGER = LoggerFactory.getLogger(VisionService.class);

	/**
	 * This method is responsible for uploading the file to GCS and then loading the
	 * image to the Vision AI for performing image labeling operation. It then
	 * returns the result of the predictions from the image.
	 * 
	 * @param visionImpl  - The inputs from the connector
	 * @param credentials - Credentials to connect to GCP
	 * @return GoogleCloudAIResponse containing the list of PretrainedImageResponse
	 */
	public GoogleCloudAIResponse<List<PretrainedImageResponse>> pretrainedObjectDetection(
			PretrainedObjectDetectionRequest visionImpl, GoogleCredentials credentials) {
		ByteString imgBytes = null;
		ImageAnnotatorSettings settings = null;
		GoogleCloudAIResponse<List<PretrainedImageResponse>> aiResponse = null;
		try {
			LOGGER.info("Creating storage instance and blob");
			Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
			Path path = Paths.get(visionImpl.getTestFilePath());
			String fileName = path.getFileName().toString();
			BlobId blobId = BlobId.of(visionImpl.getBucketName(), visionImpl.getPathInBucket() + fileName);

			LOGGER.info("Reading the byte content of the file");
			byte[] fileBytes = Files.readAllBytes(path);
			BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
			Blob blob = storage.create(blobInfo, fileBytes);

			LOGGER.info("Loading the image for prediction");
			blob = storage.get(visionImpl.getBucketName(), visionImpl.getPathInBucket() + fileName);
			byte[] data = blob.getContent();
			imgBytes = ByteString.copyFrom(data);

			LOGGER.info("Initiating the Image Annotator Settings");
			settings = ImageAnnotatorSettings.newBuilder()
					.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();
		} catch (IOException e) {
			LOGGER.error("IOException occurred while performing pretrained object detection", e);
			return null;
		}

		try (ImageAnnotatorClient vision = ImageAnnotatorClient.create(settings)) {
			LOGGER.info("Building image annotation request");
			List<AnnotateImageRequest> requests = new ArrayList<>();
			Image img = Image.newBuilder().setContent(imgBytes).build();
			Feature feat = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
			AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
			requests.add(request);

			LOGGER.info("Performing label detection on the image file");
			BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);
			List<AnnotateImageResponse> responses = response.getResponsesList();

			// List to hold ImageResult objects
			List<PretrainedImageResponse> imageResults = new ArrayList<>();

			LOGGER.info("Extracting description and score");
			for (AnnotateImageResponse res : responses) {
				if (res.hasError()) {
					LOGGER.error("Error: {}", res.getError().getMessage());
				}
				LOGGER.info("Adding the PretrainedImageResponse to a list");
				for (EntityAnnotation annotation : res.getLabelAnnotationsList()) {
					String description = annotation.getDescription();
					float score = annotation.getScore();
					float ascore = Float.parseFloat(String.format("%.2f", score));
					// Create ImageResult object and add it to the list
					PretrainedImageResponse imageResult = new PretrainedImageResponse(description, ascore);
					imageResults.add(imageResult);
				}
			}
			LOGGER.info("Returning the Image Prediction Results");
			aiResponse = new GoogleCloudAIResponse<>();
			aiResponse.setResponse(imageResults);
		} catch (IOException e) {
			LOGGER.error("IOException occurred while performing pretrained object detection", e);
			return null;
		}
		return aiResponse;
	}

	/**
	 * This method is responsible for uploading the file to GCS and then loading the
	 * video to the GCP Cloud Video Intelligence AI for performing the object
	 * tracking operation. It then returns the result of the prediction classes
	 * along with the confidence from the video.
	 * 
	 * @param visionImpl  - The inputs from the connector
	 * @param credentials - Credentials to connect to GCP
	 * @return GoogleCloudAIResponse containing the list of PretrainedVideoResponse
	 */
	public GoogleCloudAIResponse<List<PretrainedVideoResponse>> pretrainedObjectDetectionVideo(
			PretrainedObjectDetectionRequest visionImpl, GoogleCredentials credentials) {
		Storage storage = createStorage(credentials);
		String gcsUri = uploadFileToStorage(storage, visionImpl);
		VideoIntelligenceServiceSettings settings = createVideoIntelligenceSettings(credentials);
		
		if (storage == null || gcsUri == null || settings == null) {
			return null;
		}
		
		LOGGER.info("Starting the steps for video object tracking");
		return performObjectTracking(gcsUri, settings);
	}

	/**
	 * Creates a Storage instance using the provided GoogleCredentials.
	 *
	 * @param credentials GoogleCredentials for authentication
	 * @return Storage instance
	 */
	private Storage createStorage(GoogleCredentials credentials) {
		LOGGER.info("Creating Storage instance using provided GoogleCredentials");

		return StorageOptions.newBuilder().setCredentials(credentials).build().getService();
	}

	/**
	 * Uploads a file to the specified Storage and returns the GCS URI of the
	 * uploaded file.
	 *
	 * @param storage    Storage instance to use for uploading
	 * @param visionImpl PretrainedObjectDetectionRequest containing information
	 *                   about the file to upload
	 * @return GCS URI of the uploaded file
	 */
	private String uploadFileToStorage(Storage storage, PretrainedObjectDetectionRequest visionImpl) {
		LOGGER.info("Uploading file to Storage");

		try {
			Path path = Paths.get(visionImpl.getTestFilePath());
			String fileName = path.getFileName().toString();
			byte[] fileBytes = Files.readAllBytes(path);
			BlobId blobId = BlobId.of(visionImpl.getBucketName(), visionImpl.getPathInBucket() + fileName);
			BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
			storage.create(blobInfo, fileBytes);
			return "gs://" + visionImpl.getBucketName() + "/" + visionImpl.getPathInBucket() + fileName;
		} catch (IOException e) {
			LOGGER.error("IOException occurred while uploading file to storage", e);
			return null;
		}
	}

	/**
	 * Creates VideoIntelligenceServiceSettings using the provided
	 * GoogleCredentials.
	 *
	 * @param credentials GoogleCredentials for authentication
	 * @return VideoIntelligenceServiceSettings instance
	 */
	private VideoIntelligenceServiceSettings createVideoIntelligenceSettings(GoogleCredentials credentials) {
		LOGGER.info("Creating Video Intelligence Service Settings");

		try {
			return VideoIntelligenceServiceSettings.newBuilder()
					.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();
		} catch (IOException e) {
			LOGGER.error("IOException occurred while creating video intelligence settings", e);
			return null;
		}
	}

	/**
	 * Performs object tracking on the video located at the specified GCS URI using
	 * the provided settings.
	 *
	 * @param gcsUri   GCS URI of the video to perform object tracking on
	 * @param settings VideoIntelligenceServiceSettings to use for object tracking
	 * @return GoogleCloudAIResponse containing the list of PretrainedVideoResponse
	 */
	private GoogleCloudAIResponse<List<PretrainedVideoResponse>> performObjectTracking(String gcsUri,
			VideoIntelligenceServiceSettings settings) {
		LOGGER.info("Performing object tracking on the video");

		try (VideoIntelligenceServiceClient client = VideoIntelligenceServiceClient.create(settings)) {
			AnnotateVideoRequest request = AnnotateVideoRequest.newBuilder().setInputUri(gcsUri)
					.addFeatures(com.google.cloud.videointelligence.v1.Feature.OBJECT_TRACKING)
					.setLocationId("us-east1").build();

			OperationFuture<AnnotateVideoResponse, AnnotateVideoProgress> future = client.annotateVideoAsync(request);
			AnnotateVideoResponse response = future.get(450, TimeUnit.SECONDS);

			List<PretrainedVideoResponse> videoResults = processAnnotationResults(response);
			return createAIResponse(videoResults);
		} catch (TimeoutException | ExecutionException | InterruptedException | IOException e) {
			Thread.currentThread().interrupt();
			LOGGER.error("Exception occurred during video object tracking", e);
			return null;
		}
	}

	/**
	 * Processes the annotation results from the AnnotateVideoResponse and returns a
	 * list of PretrainedVideoResponse.
	 *
	 * @param response AnnotateVideoResponse containing the annotation results
	 * @return List of PretrainedVideoResponse
	 */
	private List<PretrainedVideoResponse> processAnnotationResults(AnnotateVideoResponse response) {
		LOGGER.info("Processing annotation results from AnnotateVideoResponse");

		List<PretrainedVideoResponse> videoResults = new ArrayList<>();
		for (int i = 0; i < response.getAnnotationResultsCount(); i++) {
			VideoAnnotationResults results = response.getAnnotationResults(i);
			for (ObjectTrackingAnnotation annotation : results.getObjectAnnotationsList()) {
				String confidence = String.format("%.2f", annotation.getConfidence());
				String entityDescription = annotation.hasEntity() ? annotation.getEntity().getDescription() : null;
				String segment = annotation.hasSegment() ? formatVideoSegment(annotation.getSegment()) : null;
				if (Double.parseDouble(confidence) > 0.85) {
					videoResults.add(new PretrainedVideoResponse(entityDescription, segment, confidence));
				}
			}
		}
		return videoResults;
	}

	/**
	 * Formats the given VideoSegment into a human-readable string representation.
	 *
	 * @param videoSegment VideoSegment to format
	 * @return Formatted string representation of the VideoSegment
	 */
	private String formatVideoSegment(VideoSegment videoSegment) {
		LOGGER.info("Formatting VideoSegment into a human-readable string representation");

		Duration startTimeOffset = videoSegment.getStartTimeOffset();
		Duration endTimeOffset = videoSegment.getEndTimeOffset();
		return String.format("%.2fs to %.2fs", startTimeOffset.getSeconds() + startTimeOffset.getNanos() / 1e9,
				endTimeOffset.getSeconds() + endTimeOffset.getNanos() / 1e9);
	}

	/**
	 * Creates a GoogleCloudAIResponse containing the provided list of
	 * PretrainedVideoResponse.
	 *
	 * @param videoResults List of PretrainedVideoResponse
	 * @return GoogleCloudAIResponse containing the list of PretrainedVideoResponse
	 */
	private GoogleCloudAIResponse<List<PretrainedVideoResponse>> createAIResponse(
			List<PretrainedVideoResponse> videoResults) {
		LOGGER.info("Creating GoogleCloudAIResponse containing the list of PretrainedVideoResponse");

		GoogleCloudAIResponse<List<PretrainedVideoResponse>> aiResponse = new GoogleCloudAIResponse<>();
		if (videoResults.size() > 20) {
			aiResponse.setResponse(videoResults.subList(0, 20));
		} else {
			aiResponse.setResponse(videoResults);
		}
		return aiResponse;
	}

}
