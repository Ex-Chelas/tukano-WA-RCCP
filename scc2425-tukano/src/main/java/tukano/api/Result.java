package tukano.api;

import java.util.function.Function;

/**
 * Represents the result of an operation, either wrapping a result of the given type,
 * or an error.
 *
 * @param <T> type of the result value associated with success
 * @author smd
 */
public interface Result<T> {

    /**
     * Convenience method for returning non-error results of the given type
     *
     * @param result of value of the result
     * @return the value of the result
     */
    static <T> Result<T> ok(T result) {
        return new OkResult<>(result);
    }

    /**
     * Convenience method for returning non-error results without a value
     *
     * @return non-error result
     */
    static <T> Result<T> ok() {
        return new OkResult<>(null);
    }

    /**
     * Convenience method used to return an error
     *
     * @return error result
     */
    static <T> Result<T> error(ErrorCode error) {
        return new ErrorResult<>(error);
    }

    /**
     * Convenience method used to return an error
     *
     * @return error result
     */
    static <T> Result<T> errorOrValue(Result<?> res, T val) {
        if (res.isOK())
            return ok(val);
        else
            return error(res.error());
    }

    static <T> Result<T> errorOrValue(Result<?> res, Result<T> other) {
        if (res.isOK())
            return other;
        else
            return error(res.error());
    }

    static Result<Void> errorOrVoid(Result<?> res, Result<?> other) {
        if (res.isOK())
            return other.isOK() ? ok() : error(other.error());
        else
            return error(res.error());
    }

    static <T, Q> Result<Void> errorOrVoid(Result<T> res, Function<T, Result<Q>> b) {
        if (!res.isOK())
            return error(res.error());

        var bRes = b.apply(res.value());
        return bRes.isOK() ? ok() : error(bRes.error());
    }

    static <T, Q> Result<Q> errorOrResult(Result<T> a, Function<T, Result<Q>> b) {
        if (a.isOK())
            return b.apply(a.value());
        else
            return error(a.error());
    }

    static <T, Q> Result<Q> errorOrValue(Result<T> a, Function<T, Q> b) {
        if (a.isOK())
            return ok(b.apply(a.value()));
        else
            return error(a.error());
    }

    /**
     * Tests if the result is an error.
     */
    boolean isOK();

    /**
     * Gets the payload value of this result
     *
     * @return the value of this result.
     */
    T value();

    /**
     * Gets the error code of this result
     *
     * @return the error code
     */
    ErrorCode error();

    /**
     * @author smd
     * <p>
     * Service errors:
     * OK - no error, implies a non-null result of type T, except for Void-operations
     * CONFLICT - something is being created but already exists
     * NOT_FOUND - access occurred to something that does not exist
     * INTERNAL_ERROR - something unexpected happened
     */
    enum ErrorCode {
        OK, CONFLICT, NOT_FOUND, BAD_REQUEST, FORBIDDEN, INTERNAL_ERROR, NOT_IMPLEMENTED, TIMEOUT;

        public static ErrorCode errorCodeFromStatus(int status) {
            return switch (status) {
                case 200 -> Result.ErrorCode.OK;
                case 404 -> Result.ErrorCode.NOT_FOUND;
                case 409 -> Result.ErrorCode.CONFLICT;
                default -> Result.ErrorCode.INTERNAL_ERROR;
            };
        }
    }
}

/**
 * Represents a successful result of an operation
 *
 * @param result
 * @param <T>
 */
record OkResult<T>(T result) implements Result<T> {

    @Override
    public boolean isOK() {
        return true;
    }

    @Override
    public T value() {
        return result;
    }

    @Override
    public ErrorCode error() {
        return ErrorCode.OK;
    }

    public String toString() {
        return "(OK, " + value() + ")";
    }
}

/**
 * Represents an error result of an operation
 *
 * @param error
 * @param <T>
 */
record ErrorResult<T>(ErrorCode error) implements Result<T> {

    @Override
    public boolean isOK() {
        return false;
    }

    @Override
    public T value() {
        throw new RuntimeException("Attempting to extract the value of an Error: " + error());
    }

    public String toString() {
        return "(" + error() + ")";
    }
}
