package de.core_concepts.statemachine;

/**
 * An exception that represents a validation error during an ExitValidation.
 * <p>
 * Created by zieglerch on 08.01.2016.
 */
public class ValidationException extends RuntimeException {

    public static final String ERROR_MSG = "Errorcode: %s";

    private static final long serialVersionUID = 1L;

    private final Error error;

    public ValidationException(Error error) {
        this(error, null);

    }

    public ValidationException(Error error, Throwable throwable) {
        super(String.format(ERROR_MSG, error.getId()), throwable);
        this.error = error;
    }

    public Error getError() {
        return this.error;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ValidationException)) return false;
        final ValidationException other = (ValidationException) o;
        if (!other.canEqual((Object) this)) return false;
        if (!super.equals(o)) return false;
        final Object this$error = this.getError();
        final Object other$error = other.getError();
        if (this$error == null ? other$error != null : !this$error.equals(other$error)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + super.hashCode();
        final Object $error = this.getError();
        result = result * PRIME + ($error == null ? 43 : $error.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof ValidationException;
    }

    public String toString() {
        return "ValidationException(super=" + super.toString() + ", error=" + this.getError() + ")";
    }
}
