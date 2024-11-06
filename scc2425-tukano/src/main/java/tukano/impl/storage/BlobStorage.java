package tukano.impl.storage;

import tukano.api.Result;

import java.util.function.Consumer;

/**
 * Interface for blob storage
 */
public interface BlobStorage {

    /**
     * Check if a path is valid
     *
     * @param path Path to check
     * @return True if the path is valid, false otherwise
     */
    static boolean isValidPath(String path) {
        return path == null || path.isBlank();
    }

    /**
     * Write bytes to a path
     *
     * @param path  Path to write to
     * @param bytes Bytes to write
     * @return Result containing the status of the operation
     */
    Result<Void> write(String path, byte[] bytes);

    /**
     * Delete a file at a path
     *
     * @param path Path to delete
     * @return Result containing the status of the operation
     */
    Result<Void> delete(String path);

    /**
     * Read bytes from a path
     *
     * @param path Path to read from
     * @return Result containing the bytes
     */
    Result<byte[]> read(String path);

    /**
     * Read bytes from a path and write them to a sink
     *
     * @param path Path to read from
     * @param sink Sink to write to
     * @return Result containing the status of the operation
     */
    Result<Void> read(String path, Consumer<byte[]> sink);
}
