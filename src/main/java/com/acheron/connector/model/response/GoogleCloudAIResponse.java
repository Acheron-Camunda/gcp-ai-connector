package com.acheron.connector.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This is the global response model which will be returned for the connector
 * requests
 * 
 * @param <T> - The Response Type as per the operation
 * @author HariharanB
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleCloudAIResponse<T> {

	private T response;
}
