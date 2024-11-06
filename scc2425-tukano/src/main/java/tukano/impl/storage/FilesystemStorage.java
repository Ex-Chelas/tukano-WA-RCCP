package tukano.impl.storage;

import tukano.api.Result;
import utils.Hash;
import utils.IO;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static tukano.api.Result.ErrorCode.*;
import static tukano.api.Result.error;
import static tukano.api.Result.ok;

/**
 * Implementation of BlobStorage for local filesystem
 */
public class FilesystemStorage implements BlobStorage {
    private static final Logger Log = Logger.getLogger(FilesystemStorage.class.getName());
    private static final int CHUNK_SIZE = 4096;
    private static final String DEFAULT_ROOT_DIR = "/tmp/";
    private static FilesystemStorage instance;
    private final String rootDir;

    private FilesystemStorage(String rootDir) {
        if (BlobStorage.isValidPath(rootDir)) {
            this.rootDir = DEFAULT_ROOT_DIR;
        } else {
            this.rootDir = rootDir;
        }
    }

    public static FilesystemStorage getInstance(String connectionString) {
        if (instance == null) {
            instance = new FilesystemStorage(connectionString);
        }
        return instance;
    }

    @Override
    public Result<Void> write(String path, byte[] bytes) {
        if (BlobStorage.isValidPath(path)) {
            return error(BAD_REQUEST);
        }

        var file = toFile(path);

        if (file.exists()) {
            if (Arrays.equals(Hash.sha256(bytes), Hash.sha256(IO.read(file)))) {
                return ok();
            } else {
                return error(CONFLICT);
            }
        }
        IO.write(file, bytes);
        return ok();
    }

    @Override
    public Result<byte[]> read(String path) {
        if (BlobStorage.isValidPath(path)) {
            return error(BAD_REQUEST);
        }

        var file = toFile(path);
        if (!file.exists()) {
            return error(NOT_FOUND);
        }

        var bytes = IO.read(file);
        return bytes != null ? ok(bytes) : error(INTERNAL_ERROR);
    }

    @Override
    public Result<Void> read(String path, Consumer<byte[]> sink) {
        if (BlobStorage.isValidPath(path)) {
            return error(BAD_REQUEST);
        }

        var file = toFile(path);
        if (!file.exists()) {
            return error(NOT_FOUND);
        }

        IO.read(file, CHUNK_SIZE, sink);
        return ok();
    }

    @Override
    public Result<Void> delete(String path) {
        if (BlobStorage.isValidPath(path)) {
            return error(BAD_REQUEST);
        }

        try {
            var file = toFile(path);
            Files.walk(file.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

        } catch (IOException e) {
            Log.severe("Failed to delete file: " + path);
            return error(INTERNAL_ERROR);
        }
        return ok();
    }

    /**
     * Convert a path to a file
     *
     * @param path Path to convert
     * @return File object
     */
    private File toFile(String path) {
        var res = new File(rootDir + path);

        var parent = res.getParentFile();
        if (!parent.exists()) {
            if (parent.mkdirs()) {
                Log.info("Created directory: " + parent);
            } else {
                Log.severe("Failed to create directory: " + parent);
            }
        }

        return res;
    }
}
