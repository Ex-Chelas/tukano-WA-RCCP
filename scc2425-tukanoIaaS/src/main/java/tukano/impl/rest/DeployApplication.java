package tukano.impl.rest;

import jakarta.ws.rs.core.Application;

import java.util.HashSet;
import java.util.Set;

public class DeployApplication extends Application {
    private final Set<Object> singletons = new HashSet<>();
    private final Set<Class<?>> resources = new HashSet<>();


    public DeployApplication() {
        resources.add(RestBlobsResource.class);
        resources.add(RestUsersResource.class);
        resources.add(RestShortsResource.class);
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
}
