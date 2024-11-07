package tukano.impl.rest;

import jakarta.ws.rs.core.Application;
import tukano.impl.Token;
import tukano.impl.databse.CosmosDB;
import tukano.impl.databse.DB;
import tukano.impl.databse.Hibernate;
import tukano.impl.storage.CloudStorage;
import tukano.impl.storage.FilesystemStorage;
import tukano.impl.storage.Storage;
import utils.Args;

import java.util.HashSet;
import java.util.Set;

public class DeployApplication extends Application {
    private final Set<Object> singletons = new HashSet<>();
    private final Set<Class<?>> resources = new HashSet<>();

    public DeployApplication() {
        configureServices();

        resources.add(RestUsersResource.class);
        resources.add(RestShortsResource.class);
        resources.add(RestBlobsResource.class);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return resources;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Set<Object> getSingletons() {
        return singletons;
    }

    private void configureServices() {
        Args.use(System.getenv("ARGS").split(" "));
        System.getenv().forEach((key, value) -> System.out.println(key + " = " + value));
        Token.setSecret(Args.valueOf("-secret", ""));

        switch (Args.valueOf("-db", "cosmos")) {
            case "cosmos":
                DB.configureInstance(CosmosDB.getInstance());
                break;
            case "hibernate":
                DB.configureInstance(Hibernate.getInstance());
                break;
            default:
                throw new IllegalArgumentException("Invalid DB type");
        }

        switch (Args.valueOf("-storage", "azure")) {
            case "azure":
                Storage.configureInstance(CloudStorage.getInstance(System.getenv("STORAGE_CONNECTION_STRING")));
                break;
            case "local":
                Storage.configureInstance(FilesystemStorage.getInstance(System.getenv("STORAGE_CONNECTION_STRING")));
                break;
            default:
                throw new IllegalArgumentException("Invalid storage type");
        }
    }
}
