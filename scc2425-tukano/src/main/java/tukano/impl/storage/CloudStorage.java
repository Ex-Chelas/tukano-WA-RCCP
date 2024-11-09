package tukano.impl.storage;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import tukano.api.Result;
import tukano.api.Result.ErrorCode;
import utils.TryCatch;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Implementation of BlobStorage for Azure Blob Storage
 */
public class CloudStorage implements BlobStorage {
    private static final Logger Log = Logger.getLogger(CloudStorage.class.getName());
    private static final String CONTAINER_NAME = "shorts";
    private static CloudStorage instance;
    private static HashMap<String, BlobContainerClient> containersClients;
    private boolean isPrimary = true;

    private CloudStorage(List<String> connectionStrings) {
        containersClients = initBlobClientPool(connectionStrings);
    }

    public synchronized static CloudStorage getInstance(List<String> connectionStrings) {
        if (instance == null) {
            for (String connString : connectionStrings) {
                if (BlobStorage.isValidPath(connString)) {
                    Log.severe("Invalid connection string provided.");
                    throw new IllegalArgumentException("Invalid connection string provided.");
                }

                if (!connString.startsWith("DefaultEndpointsProtocol=")) {
                    Log.severe("Invalid connection string provided.");
                    throw new IllegalArgumentException("Invalid connection string provided.");
                }
            }

            instance = new CloudStorage(connectionStrings);

            // Ensure the containers exist
            for (BlobContainerClient containerClient : containersClients.values()) {
                if (!containerClient.exists()) {
                    containerClient.create();
                    Log.info("Created new Azure Blob container: " + CONTAINER_NAME);
                }
            }

        }
        return instance;
    }

    /**
     * Initializes the Azure Blob Storage client pool with the provided connection strings
     * The first connection string is considered the primary connection string
     *
     * @param connectionStrings List of connection strings
     * @return HashMap of BlobContainerClient objects
     */
    private HashMap<String, BlobContainerClient> initBlobClientPool(List<String> connectionStrings) {
        HashMap<String, BlobContainerClient> blobClientPool = new HashMap<>();
        for (String connectionString : connectionStrings) {
            BlobContainerClient blobClient = new BlobContainerClientBuilder()
                    .connectionString(connectionString)
                    .containerName(CONTAINER_NAME)
                    .buildClient();
            blobClientPool.put((isPrimary) ? "Primary" : "Secondary", blobClient);
            isPrimary = false;
        }
        return blobClientPool;
    }

    @Override
    public Result<Void> write(String path, byte[] bytes) {
        if (BlobStorage.isValidPath(path)) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        return TryCatch.tryCatchForResult(Log, () -> {
            for (BlobContainerClient containerClient : containersClients.values()) {
                var blobClient = containerClient.getBlobClient(path);
                blobClient.upload(BinaryData.fromBytes(bytes), true);
            }
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
            for (BlobContainerClient containerClient : containersClients.values()) {
                var blobClient = containerClient.getBlobClient(path);
                if (!blobClient.exists()) {
                    return Result.error(ErrorCode.NOT_FOUND);
                }
                blobClient.delete();
            }
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
            var containerClient = containersClients.get("Primary");
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
            var containerClient = containersClients.get("Primary");
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
