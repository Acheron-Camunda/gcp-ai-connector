package com.acheron.connector.exception;

/**
 * @author PraveenKumarReddy
 *     <p>This exception will be thrown while prompt could not be processed
 */
public class CouldNotProcessPromptException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** This is a default constructor */
  public CouldNotProcessPromptException() {
    super();
  }

  /**
   * This is a parameterized constructor
   *
   * @param message To Pass the message about exception
   */
  public CouldNotProcessPromptException(String message) {
    super(message);
  }
}
