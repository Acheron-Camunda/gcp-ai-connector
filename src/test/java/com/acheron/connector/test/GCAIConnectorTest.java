package com.acheron.connector.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.acheron.connector.GoogleCloudAIFunction;
import com.acheron.connector.exception.InvalidCredentialFileException;
import com.acheron.connector.model.AuthenticationData;
import com.acheron.connector.model.Operation;
import com.acheron.connector.model.OperationType;
import com.acheron.connector.model.request.AIRequestClass;
import com.acheron.connector.model.request.GoogleCloudAIRequest;
import com.acheron.connector.model.request.PromptRequest;
import com.acheron.connector.model.response.GoogleCloudAIResponse;
import com.acheron.connector.services.PromptService;
import com.acheron.connector.util.GCAIUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;

import io.camunda.connector.api.error.ConnectorInputException;
import io.camunda.connector.test.outbound.OutboundConnectorContextBuilder;
import io.camunda.connector.test.outbound.OutboundConnectorContextBuilder.TestConnectorContext;

class GCAIConnectorTest {

	private static PromptService mockPromptService;
	private static PromptService promptService;
	private static GCAIUtil gcaiUtil;

	@BeforeEach
	public void setup() {

		// Initialize the mock objects
		mockPromptService = Mockito.mock(PromptService.class);

		promptService = new PromptService();
		gcaiUtil = new GCAIUtil();

	}

	@AfterEach
	public void teardown() {
		mockPromptService = null;
		promptService = null;
		gcaiUtil = null;
	}

	/**
	 * Test case to verify that the validation fails if inputs are missing in the
	 * GCAIRequest.
	 */
	@Test
	void shouldFailIfInputsAreMissing() throws JsonProcessingException {
		// given
		GoogleCloudAIRequest request = getGoogleCloudAIRequestWithNull();

		String variablesAsJson = new ObjectMapper().writeValueAsString(request);

		// when
		TestConnectorContext context = OutboundConnectorContextBuilder.create().variables(variablesAsJson).build();

		// then
		assertThatThrownBy(() -> context.validate(request)).isInstanceOf(ConnectorInputException.class)
				.hasMessageContaining(
						"jakarta.validation.ValidationException: Found constraints violated while validating input:");
	}

	/**
	 * Test case to verify the behavior when providing invalid JSON credentials in
	 * the key file.
	 */
	@Test
	void testWithInvalidJsonCredentialsInFile() throws JsonProcessingException {
		// given
		GoogleCloudAIRequest googleCloudAIRequest = getGooogleCloudAIPromptRequest();
		googleCloudAIRequest.getAuthentication().setJsonFileAsString("src/test/resources/key.json");
		GoogleCloudAIFunction function = new GoogleCloudAIFunction();

		// when
		TestConnectorContext context = OutboundConnectorContextBuilder.create()
				.variables(new ObjectMapper().writeValueAsString(googleCloudAIRequest)).build();

		// then
		assertThatThrownBy(() -> function.execute(context)).isInstanceOf(InvalidCredentialFileException.class)
				.hasMessageContaining("Invalid credentials in the provided JSON");

	}

	/**
	 * Test case to verify the behavior when providing an invalid file path for the
	 * JSON key file.
	 */
	@Test
	void testWithInvalidFilePathforJsonKey() throws JsonProcessingException {
		// given
		GoogleCloudAIRequest googleCloudAIRequest = getGooogleCloudAIPromptRequest();
		googleCloudAIRequest.getAuthentication().setJsonFileAsString("src/test/resources/invalid-key.json");
		GoogleCloudAIFunction function = new GoogleCloudAIFunction();

		// when
		TestConnectorContext context = OutboundConnectorContextBuilder.create()
				.variables(new ObjectMapper().writeValueAsString(googleCloudAIRequest)).build();

		// then
		assertThatThrownBy(() -> function.execute(context)).isInstanceOf(FileNotFoundException.class);

	}

	/**
	 * Test case to verify the functionality of the Prompt method in prompt service.
	 * 
	 * @throws FileNotFoundException
	 */
	@Test
	void testPromptMethod() throws FileNotFoundException {

		// given
		GoogleCloudAIRequest googleCloudAIRequest = getGooogleCloudAIPromptRequest();
		GoogleCredentials credentials = gcaiUtil
				.buildCredentials(googleCloudAIRequest.getAuthentication().getJsonFileAsString());

		GoogleCloudAIResponse<String> expectedResponse = new GoogleCloudAIResponse<>();
		expectedResponse.setResponse(" Hola, ¿cómo estás?");

		when(mockPromptService.executePrompt(googleCloudAIRequest.getData().getPrompt(), credentials))
				.thenReturn(expectedResponse);

		// when
		GoogleCloudAIResponse<String> actualResponse = promptService
				.executePrompt(googleCloudAIRequest.getData().getPrompt(), credentials);

		// then
		assertEquals(expectedResponse, actualResponse);

	}

	GoogleCloudAIRequest getGoogleCloudAIRequestWithNull() {
		AuthenticationData authentication = new AuthenticationData();
		Operation operation = new Operation();
		AIRequestClass data = new AIRequestClass();
		return new GoogleCloudAIRequest(authentication, operation, data);

	}

	GoogleCloudAIRequest getGooogleCloudAIPromptRequest() {
		AuthenticationData authentication = new AuthenticationData();
		Operation operation = new Operation();
		AIRequestClass data = new AIRequestClass();
		PromptRequest promptRequest = new PromptRequest();

		authentication.setJsonFileAsString("C:\\CloudAI\\gcai-key.json");
		operation.setOperationType(OperationType.PROMPT.name());

		promptRequest.setLocation("asia-southeast1");
		promptRequest.setModel("text-bison@002");
		promptRequest.setProjectId("camunda-dev-2024");
		promptRequest.setPromptInput("Convert this to Spanish 'Hi How are you?’");
		promptRequest.setTemperature("0.2");
		promptRequest.setMaxOutputTokens("256");

		data.setPrompt(promptRequest);

		return new GoogleCloudAIRequest(authentication, operation, data);

	}

}
