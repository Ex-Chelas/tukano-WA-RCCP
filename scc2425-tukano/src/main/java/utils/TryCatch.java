package utils;


import com.azure.cosmos.CosmosException;
import tukano.api.Result;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tukano.api.Result.ErrorCode.errorCodeFromStatus;

/**
 * Utility class to handle exceptions in a functional way.
 */
public class TryCatch {

    public static <T> T maybe(ThrowableSupplier<T> s) {
        try {
            return s.get();
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    /**
     * Utility method to execute a function that returns a Result, capturing and handling exceptions.
     *
     * @param logger    the logger to use
     * @param operation the operation to execute
     * @param <T>       the type of the result
     * @return the result of the operation
     */
    public static <T> Result<T> tryCatchForResult(Logger logger, Supplier<Result<T>> operation) {
        try {
            return operation.get();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error during operation.", e);
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * Utility method to execute a function that returns a value, capturing and handling exceptions.
     *
     * @param logger       the logger to use
     * @param supplierFunc the function to execute
     * @param <T>          the type of the result
     * @return the result of the function
     */
    public static <T> Result<T> tryCatch(Logger logger, Supplier<T> supplierFunc) {
        try {
            return Result.ok(supplierFunc.get());
        } catch (CosmosException ce) {
            logger.severe("CosmosException: " + ce.getMessage());
            return Result.error(errorCodeFromStatus(ce.getStatusCode()));
        } catch (Exception x) {
            logger.severe("Exception: " + x.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    public interface ThrowableSupplier<T> {
        T get() throws Exception;
    }
}
