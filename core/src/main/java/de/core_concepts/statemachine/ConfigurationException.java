package de.core_concepts.statemachine;


/**
 * Created by zieglerch on 27.11.2015.
 */
public class ConfigurationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ConfigurationException(String message) {
    super(message);
  }
}
