package tukano.impl.rest;

import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tukano.impl.rest.utils.Configuration;
import utils.IP;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;


public class TukanoRestServer extends Application {
    public static final int PORT = 8080;
    static final String INET_ADDR_ANY = "0.0.0.0";
    final private static Logger Log = Logger.getLogger(TukanoRestServer.class.getName());
    public static String serverURI;
    static String SERVER_BASE_URI = "http://%s:%s/rest";

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
    }

    private final Set<Object> singletons = new HashSet<>();
    private final Set<Class<?>> resources = new HashSet<>();

    protected TukanoRestServer() {
        serverURI = String.format(SERVER_BASE_URI, IP.hostAddress(), PORT);
        resources.add(RestBlobsResource.class);
        resources.add(RestUsersResource.class);
        resources.add(RestShortsResource.class);
    }


    public static void main(String[] args) throws Exception {
        Configuration.configureServices();

        new TukanoRestServer().start();
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

    protected void start() throws Exception {

        ResourceConfig config = new ResourceConfig();
        for (Class<?> resource : resources) {
            config.register(resource);
        }

        JdkHttpServerFactory.createHttpServer(URI.create(serverURI.replace(IP.hostname(), INET_ADDR_ANY)), config);

        Log.info(String.format("Tukano Server ready @ %s\n", serverURI));
    }
}
