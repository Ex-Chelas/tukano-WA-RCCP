package tukano.impl.rest.utils;

import tukano.impl.Token;
import tukano.impl.cache.RedisCache;
import tukano.impl.databse.CosmosDB;
import tukano.impl.databse.DB;
import tukano.impl.databse.Hibernate;
import tukano.impl.storage.CloudStorage;
import tukano.impl.storage.FilesystemStorage;
import tukano.impl.storage.Storage;
import utils.Args;

/**
 * Configuration class to configure services based on environment variables
 */
public class Configuration {
    private static final String ARGS_KEY = "ARGS";
    private static final String STORAGE_CONNECTION_STRING_KEY = "STORAGE_CONNECTION_STRING";

    private static final String ARGS_SPLITTER = " ";

    private static final String SECRET_FLAG = "-secret";
    private static final String DB_FLAG = "-db";
    private static final String STORAGE_FLAG = "-storage";
    private static final String CACHE_FLAG = "-cache";

    public static void configureServices() {
        Args.use(System.getenv(ARGS_KEY).split(ARGS_SPLITTER));
//        System.getenv().forEach((key, value) -> System.out.println(key + " = " + value));
        Token.setSecret(Args.valueOf(SECRET_FLAG, ""));

        switch (Args.valueOf(DB_FLAG, "cosmos")) {
            case "cosmos":
                DB.configureInstance(CosmosDB.getInstance());
                break;
            case "hibernate":
                DB.configureInstance(Hibernate.getInstance());
                break;
            default:
                throw new IllegalArgumentException("Invalid DB type");
        }

        switch (Args.valueOf(STORAGE_FLAG, "azure")) {
            case "azure":
                Storage.configureInstance(CloudStorage.getInstance(System.getenv(STORAGE_CONNECTION_STRING_KEY)));
                break;
            case "local":
                Storage.configureInstance(FilesystemStorage.getInstance(System.getenv(STORAGE_CONNECTION_STRING_KEY)));
                break;
            default:
                throw new IllegalArgumentException("Invalid storage type");

        }

        switch (Args.valueOf(CACHE_FLAG, "none")) {
            case "redis":
                RedisCache.enableCache();
                break;
            case "none":
                // Cache.configureInstance(LocalCache.getInstance());
                break;
            default:
                throw new IllegalArgumentException("Invalid cache type");
        }
    }
}