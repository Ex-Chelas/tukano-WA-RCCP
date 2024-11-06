package tukano.impl.storage;

import tukano.api.Result;

import java.util.function.Consumer;

/**
 * Abstraction class for blob storage
 * Singleton class aka kotlin equivalent of an object
 *
 * @See CloudStorage
 * @See BlobStorage
 */
public class Storage {
    private static BlobStorage instance;

    public static void configureInstance(BlobStorage blob) {
        instance = blob;
    }

    public static Result<Void> write(String path, byte[] bytes) {
        return instance.write(path, bytes);
    }

    public static Result<Void> delete(String path) {
        return instance.delete(path);
    }

    public static Result<byte[]> read(String path) {
        return instance.read(path);
    }

    public static Result<Void> read(String path, Consumer<byte[]> sink) {
        return instance.read(path, sink);
    }
}
