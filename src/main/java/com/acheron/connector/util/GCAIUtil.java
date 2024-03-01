package com.acheron.connector.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;

import com.acheron.connector.exception.InvalidCredentialFileException;
import com.google.auth.oauth2.GoogleCredentials;


/**
 * Utility class for Google Cloud AI operations.
 */
public class GCAIUtil {

	/**
	 * Builds Google credentials from the JSON file in the provided path.
	 * 
	 * @param jsonFilePath The file path of the JSON file containing Google service
	 *                     account credentials.
	 * @return GoogleCredentials built from the provided JSON file.
	 * @throws FileNotFoundException          if the specified file path is invalid
	 *                                        or the file does not exist.
	 * @throws InvalidCredentialFileException if the JSON file contains invalid
	 *                                        credentials.
	 */
	public GoogleCredentials buildCredentials(String jsonFilePath) throws FileNotFoundException {
		FileInputStream serviceAccountStream = new FileInputStream(jsonFilePath);

		GoogleCredentials credentials = null;
		try {
			credentials = GoogleCredentials.fromStream(serviceAccountStream)
					.createScoped(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));
		} catch (Exception e) {
			throw new InvalidCredentialFileException("Invalid credentials in the provided JSON");
		}
		return credentials;

	}

}
