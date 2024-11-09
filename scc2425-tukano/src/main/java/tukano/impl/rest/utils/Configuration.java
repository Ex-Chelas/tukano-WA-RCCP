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

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration class to configure services based on environment variables
 * <p>
 * Assumptions:
 * <p>
 * - The number of regions is provided in the environment variable NR_OF_REGIONS
 * <p>
 * - The connection strings for the storage and database services are provided in the environment variables
 * <code>STORAGE_CONNECTION_STRING_{region}</code>
 * and <code>DB_CONNECTION_URL_{region}</code> when there are multiple connection strings for that
 * service based on region
 * <p>
 * - The connection string index 0 is the primary connection string for multi-region services
 * <p>
 * - The arguments are provided in the environment variable ARGS
 */
public class Configuration {
    private static final String ARGS_KEY = "ARGS";
    private static final String STORAGE_CONNECTION_STRING_KEY = "STORAGE_CONNECTION_STRING_{region}";
    //    private static final String DB_CONNECTION_UR_TEMPLATE_KEY = "DB_CONNECTION_URL_{region}";
    private static final String DB_CONNECTION_URL_KEY = "DB_CONNECTION_URL";
    private static final String NR_OF_REGIONS_KEY = "NR_OF_REGIONS";
    private static final String PRIMARY_REGION_KEY = "PRIMARY_REGION";

    private static final String ARGS_SPLITTER = " ";

    private static final String SECRET_FLAG = "-secret";
    private static final String DB_FLAG = "-db";
    private static final String STORAGE_FLAG = "-storage";
    private static final String CACHE_FLAG = "-cache";

    public static void configureServices() {
        Args.use(System.getenv(ARGS_KEY).split(ARGS_SPLITTER));
        Token.setSecret(Args.valueOf(SECRET_FLAG, ""));
        var regions = System.getenv(NR_OF_REGIONS_KEY);
        var primaryRegion = System.getenv(PRIMARY_REGION_KEY);

        var dbConnectionStrings = System.getenv(DB_CONNECTION_URL_KEY);
//        List<String> dbConnectionStrings = new ArrayList<>();
        List<String> storageConnectionStrings = new ArrayList<>();

        for (int i = 1; i < Integer.parseInt(regions) + 1; i++) {
//            dbConnectionStrings.add(System.getenv(DB_CONNECTION_UR_TEMPLATE_KEY.replace("{region}", String.valueOf(i))));

            //storage connection strings
            var key = STORAGE_CONNECTION_STRING_KEY.replace("{region}", String.valueOf(i));
            var storageConnectionString = System.getenv(key);
            storageConnectionStrings.add(storageConnectionString);
        }

        switch (Args.valueOf(DB_FLAG, "cosmos")) {
            case "cosmos": // has region
                DB.configureInstance(CosmosDB.getInstance(dbConnectionStrings, primaryRegion));
                break;
            case "hibernate": // doesnt have region
                DB.configureInstance(Hibernate.getInstance(dbConnectionStrings, primaryRegion));
                break;
            default:
                throw new IllegalArgumentException("Invalid DB type");
        }

        switch (Args.valueOf(STORAGE_FLAG, "azure")) {
            case "azure": // has region
                Storage.configureInstance(CloudStorage.getInstance(storageConnectionStrings));
                break;
            case "local": // doesnt have region
                Storage.configureInstance(FilesystemStorage.getInstance(storageConnectionStrings));
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