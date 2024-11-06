package tukano.impl.storage;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import tukano.api.Result;
import tukano.api.Result.ErrorCode;
import utils.TryCatch;

import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Implementation of BlobStorage for Azure Blob Storage
 */
public class CloudStorage implements BlobStorage {
    private static final Logger Log = Logger.getLogger(CloudStorage.class.getName());
    private static final String CONTAINER_NAME = "shorts";
    private static CloudStorage instance;
    private static BlobContainerClient containerClient;

    private CloudStorage(String connectionString) {
        containerClient = new BlobContainerClientBuilder()
                .connectionString(connectionString)
                .containerName(CONTAINER_NAME)
                .buildClient();
    }

    public synchronized static CloudStorage getInstance(String connectionString) {
        if (instance == null) {
            // Ensure the connection string is not null or empty
            if (BlobStorage.isValidPath(connectionString)) {
                Log.severe("Invalid connection string provided.");
                throw new IllegalArgumentException("Invalid connection string provided.");
            }

            // Ensure the connection string is valid
            if (!connectionString.startsWith("DefaultEndpointsProtocol=")) {
                Log.severe("Invalid connection string provided.");
                throw new IllegalArgumentException("Invalid connection string provided.");
            }

            instance = new CloudStorage(connectionString);

            // Ensure the container exists
            if (!containerClient.exists()) {
                containerClient.create();
                Log.info("Created new Azure Blob container: " + CONTAINER_NAME);
            }
        }
        return instance;
    }

    @Override
    public Result<Void> write(String path, byte[] bytes) {
        if (BlobStorage.isValidPath(path)) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        return TryCatch.tryCatchForResult(Log, () -> {
            var blobClient = containerClient.getBlobClient(path);
            blobClient.upload(BinaryData.fromBytes(bytes), true);

            Log.info("Uploaded blob to path: " + path);
            return Result.ok();
        });
    }

    @Override
    public Result<Void> delete(String path) {
        if (BlobStorage.isValidPath(path)) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        return TryCatch.tryCatchForResult(Log, () -> {
            var blobClient = containerClient.getBlobClient(path);
            if (!blobClient.exists()) {
                return Result.error(ErrorCode.NOT_FOUND);
            }
            blobClient.delete();
            Log.info("Deleted blob at path: " + path);
            return Result.ok();
        });
    }

    @Override
    public Result<byte[]> read(String path) {
        if (BlobStorage.isValidPath(path)) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        return TryCatch.tryCatchForResult(Log, () -> {
            var blobClient = containerClient.getBlobClient(path);
            if (!blobClient.exists()) {
                return Result.error(ErrorCode.NOT_FOUND);
            }
            byte[] data = blobClient.downloadContent().toBytes();
            Log.info("Downloaded blob from path: " + path);
            return Result.ok(data);
        });
    }

    @Override
    public Result<Void> read(String path, Consumer<byte[]> sink) {
        if (BlobStorage.isValidPath(path)) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        return TryCatch.tryCatchForResult(Log, () -> {
            var blobClient = containerClient.getBlobClient(path);
            if (!blobClient.exists()) {
                return Result.error(ErrorCode.NOT_FOUND);
            }
            byte[] data = blobClient.downloadContent().toBytes();
            sink.accept(data);
            Log.info("Downloaded blob and passed data to sink for path: " + path);
            return Result.ok();
        });
    }
}
