package utils;

import com.azure.cosmos.*;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.util.CosmosPagedIterable;
import tukano.api.Result;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class CosmosDB {
    private static final Logger Log = Logger.getLogger(CosmosDB.class.getName());

    private static final String CONNECTION_URL = ""; // Replace with values from the Azure portal
    private static final String DB_KEY = "";
    private static final String DB_NAME = "scc70730n70731";

    static CosmosClient client = new CosmosClientBuilder()
            .endpoint(CONNECTION_URL)
            .key(DB_KEY)
            .directMode() // comment this if not to use direct mode
            .consistencyLevel(ConsistencyLevel.SESSION)
            .connectionSharingAcrossClientsEnabled(true)
            .contentResponseOnWriteEnabled(true) // On write, return the object written
            .buildClient();

    /**
     * Get a container from the database with a specific name
     *
     * @param containerName the name of the container
     * @return CosmosContainer - the container
     */
    public static CosmosContainer getContainer(String containerName) {
        return client.getDatabase(DB_NAME).getContainer(containerName);
    }

    /**
     * Get a container from the database with a specific class
     *
     * @param clazz the class of the container
     * @return CosmosContainer - the container
     */
    public static <T> CosmosContainer getContainer(Class<T> clazz) {
        return getContainer(clazz.getSimpleName().toLowerCase());
    }

    /**
     * Create an item in a container
     *
     * @param container the container to create the item in
     * @param item      the item to create
     * @return T - the created item
     * @throws Exception - if the item could not be created
     */
    public static <T> T create(CosmosContainer container, T item) throws Exception {
        CosmosItemResponse<T> response = container.createItem(item);
        if (response.getStatusCode() == 201) {
            return response.getItem();
        } else {
            Log.severe("Error: " + response.getStatusCode());
            throw new Exception("Error: " + response.getStatusCode());
        }
    }

    /**
     * Read all items from a container with a specific id and class
     *
     * @param container the container to read from
     * @param clazz     the class of the items
     * @return List - a list of items
     */
    public static <T> List<T> read(CosmosContainer container, String idName, String id, Class<T> clazz) {
        String query = createQuery(container.getId(), idName, id);
        CosmosPagedIterable<T> response = container.queryItems(query, new CosmosQueryRequestOptions(), clazz);
        return response.iterableByPage().iterator().next().getResults();
    }

    /**
     * Read all items from a container with a specific id, class and predicate
     *
     * @param container the container to read from
     * @param predicate the predicate
     * @param clazz     the class of the items
     * @return List - a list of items
     */
    public static <T> List<T> read(CosmosContainer container, String predicate, Class<T> clazz) {
        String query = createQueryWPredicate(container.getId(), predicate);
        CosmosPagedIterable<T> response = container.queryItems(query, new CosmosQueryRequestOptions(), clazz);
        return response.iterableByPage().iterator().next().getResults();
    }

    /**
     * Update an item in a container
     *
     * @param container the container to update the item in
     * @param item      the item to update
     * @return T - the updated item
     * @throws Exception - if the item could not be updated
     */
    public static <T> T update(CosmosContainer container, T item) throws Exception {
        CosmosItemResponse<T> response = container.upsertItem(item);
        if (response.getStatusCode() == 200) {
            return response.getItem();
        } else {
            Log.severe("Error: " + response.getStatusCode());
            throw new Exception("Error: " + response.getStatusCode());
        }
    }

    /**
     * Delete an item from a container
     *
     * @param container the container to delete the item from
     * @param item      the item to delete
     * @return T - the deleted item
     * @throws Exception - if the item could not be deleted
     */
    public static <T> T delete(CosmosContainer container, T item) throws Exception {
        CosmosItemResponse<?> response = container.deleteItem(item, new CosmosItemRequestOptions());
        if (response.getStatusCode() == 204) {
            return (T) response.getItem();
        } else {
            Log.severe("Error: " + response.getStatusCode());
            throw new Exception("Error: " + response.getStatusCode());
        }
    }

    /**
     * Get one item from a container with a specific id, class and predicate
     *
     * @param container the container to read from
     * @param idName    the id key
     * @param id        the id value
     * @param clazz     the class of the items
     * @return Result - the result
     */
    public static <T> Result<T> getOne(Class<T> container, String idName, String id, Class<T> clazz) {
        return read(getContainer(container), idName, id, clazz).stream().findFirst().map(Result::ok).orElse(Result.error(Result.ErrorCode.NOT_FOUND));
    }

    /**
     * Execute a SQL query on a container
     *
     * @param query the query
     * @param clazz the class of the items
     * @return List - a list of items
     */
    public static <T> List<T> selectSql(String query, Class<T> clazz) {
        return read(getContainer(clazz.getSimpleName()), query, clazz);
    }

    /**
     * Delete one item from a container
     *
     * @param container the container to delete the item from
     * @param obj       the item to delete
     * @return Result - the result
     */
    public static <T> Result<T> deleteOne(Class<T> container, T obj) {
        try {
            return Result.ok(delete(getContainer(container), obj));
        } catch (Exception e) {
            Log.severe("Error: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * Update one item in a container
     *
     * @param container the container to update the item in
     * @param obj       the item to update
     * @return Result - the result
     */
    public static <T> Result<T> updateOne(Class<T> container, T obj) {
        try {
            return Result.ok(update(getContainer(container), obj));
        } catch (Exception e) {
            Log.severe("Error: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * Insert one item in a container
     *
     * @param container the container to insert the item in
     * @param obj       the item to insert
     * @return Result - the result
     */
    public static <T> Result<T> insertOne(Class<T> container, T obj) {
        try {
            return Result.ok(create(getContainer(container), obj));
        } catch (Exception e) {
            Log.severe("Error: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * Query items from a container
     *
     * @param container the container to query from
     * @param queryStr  the query string
     * @param clazz     the class of the items
     * @return List - a list of items
     */
    public static <T> List<T> query(Class<T> container, String queryStr, Class<T> clazz) {
        try {
            var res = getContainer(container).queryItems(queryStr, new CosmosQueryRequestOptions(), clazz);
            return res.stream().toList();
        } catch (Exception e) {
            Log.severe("Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Execute a SQL query on a container
     *
     * @param query the query
     * @param clazz the class of the items
     * @return List - a list of items
     */
    public static <T, R> Result<List<R>> sql(String query, Class<R> returnClazz, Class<T> clazz) {

        return tryCatch(() -> {

            boolean isCountQuery = query.toLowerCase().contains("count(*)" );
            String parsedQuery = query.replace("count(*)", "count(1)" );

            if (!returnClazz.equals(clazz)) {
                var mapRes = getContainer(clazz).queryItems(parsedQuery, new CosmosQueryRequestOptions(), Map.class);

                return mapRes.stream().map(m -> {
                    try {
                        if (isCountQuery) {
                            return (R) m.get("$1" );
                        } else {
                            return (R) m.values();
                        }
                    } catch (Exception e) {
                        Log.severe("Error: " + e.getMessage());
                        throw new RuntimeException("Failed to cast map to return type", e);
                    }
                }).toList();
            } else {
                var res = getContainer(clazz).queryItems(query, new CosmosQueryRequestOptions(), returnClazz);
                return res.stream().toList();
            }
        });
    }

    private static <T> Result<T> tryCatch(Supplier<T> supplierFunc) {
        try {
            return Result.ok(supplierFunc.get());
        } catch (CosmosException ce) {
            Log.severe("Error: " + ce.getMessage());
            return Result.error(errorCodeFromStatus(ce.getStatusCode()));
        } catch (Exception x) {
            Log.severe("Error: " + x.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    static Result.ErrorCode errorCodeFromStatus(int status) {
        return switch (status) {
            case 200 -> Result.ErrorCode.OK;
            case 404 -> Result.ErrorCode.NOT_FOUND;
            case 409 -> Result.ErrorCode.CONFLICT;
            default -> Result.ErrorCode.INTERNAL_ERROR;
        };
    }


    /**
     * Opens a transaction equivalent in CosmosDB to a Hibernate transaction, and has access to all containers
     */
    public static <T> Result<T> transaction(Consumer<CosmosDatabase> c) {
        try {
            c.accept(client.getDatabase(DB_NAME));
            return Result.ok();
        } catch (Exception e) {
            Log.severe("Error: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }


    /**
     * Create a query string
     *
     * @param containerName the name of the container
     * @param idName        the name of the id
     * @param id            the id
     * @return String - the query string
     */
    private static String createQuery(String containerName, String idName, String id) {
        return "Select * from " + containerName + " where " + idName + " = " + id;
    }

    /**
     * Create a query string with a predicate
     *
     * @param containerName the name of the container
     * @param predicate     the predicate
     * @return String - the query string
     */
    private static String createQueryWPredicate(String containerName, String predicate) {
        return "Select * from " + containerName + " where " + predicate;
    }
}
