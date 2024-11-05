package tukano.impl.storage;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import tukano.api.Result;
import tukano.api.Result.ErrorCode;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CloudStorage implements BlobStorage {

    private static final Logger Log = Logger.getLogger(CloudStorage.class.getName());

    // Get connection string in the storage access keys page
    // mvn -DconnectionString=some_value install
    // DefaultEndpointsProtocol=https;AccountName=scc70730n70731;AccountKey=h8uWFmizikv53KOIIiN+Jbrbhs2EDW94zgVTNZFu+ARlCOms4HWL+JzHWZ/3foavHnGWWGUi/Xe3+AStFudxPA==;EndpointSuffix=core.windows.net
    // https://scc70730n70731.blob.core.windows.net/images
    private static final String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName=scc70730n70731;AccountKey=h8uWFmizikv53KOIIiN+Jbrbhs2EDW94zgVTNZFu+ARlCOms4HWL+JzHWZ/3foavHnGWWGUi/Xe3+AStFudxPA==;EndpointSuffix=core.windows.net";
    private static final String CONTAINER_NAME = "shorts";

    private final BlobContainerClient containerClient;

    public CloudStorage() {
        this.containerClient = new BlobContainerClientBuilder()
                .connectionString(storageConnectionString)
                .containerName(CONTAINER_NAME)
                .buildClient();

        // Ensure the container exists
        if (!containerClient.exists()) {
            containerClient.create();
            Log.info("Created new Azure Blob container: " + CONTAINER_NAME);
        }
    }


    /**
     * Utility method to execute a blob operation within a try-catch block.
     *
     * @param operation The blob operation to execute.
     * @param <T>       The type of the Result.
     * @return A Result object representing the outcome of the operation.
     */
    private <T> Result<T> tryCatch(Supplier<Result<T>> operation) {
        try {
            return operation.get();
        } catch (Exception e) {
            Log.log(Level.SEVERE, "Unexpected error during Azure Blob operation.", e);
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> write(String path, byte[] bytes) {
        if (path == null || path.isBlank()) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        return tryCatch(() -> {
            var blobClient = containerClient.getBlobClient(path);
            blobClient.upload(BinaryData.fromBytes(bytes), true);

            Log.info("Uploaded blob to path: " + path);
            return Result.ok();
        });
    }

    @Override
    public Result<Void> delete(String path) {
        if (path == null || path.isBlank()) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        return tryCatch(() -> {
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
        if (path == null || path.isBlank()) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        return tryCatch(() -> {
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
        if (path == null || path.isBlank()) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        return tryCatch(() -> {
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
