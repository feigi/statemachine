package de.core_concepts.statemachine;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

/**
 * An exception that represents a validation error during an ExitValidation.
 *
 * Created by zieglerch on 08.01.2016.
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public static final String ERROR_MSG = "Errorcode: %s";

  @NonNull
  private final Error error;

  public ValidationException(Error error) {
    this(error, null);

  }

  public ValidationException(Error error, Throwable throwable) {
    super(String.format(ERROR_MSG, error.getId()), throwable);
    this.error = error;
  }
}
