package cloudFunctions;

import com.azure.cosmos.*;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import tukano.api.Short;

import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 * This is just the code of the cloud function, the actual deployment is done in a != project.
 * This function increments the view count of a short by downloads.
 */
public class ViewCounterFunction {
    // Function and trigger names
    private static final String FUNCTION_IDENTIFIER = "incrementViewCount";
    private static final String HTTP_TRIGGER_NAME = "httpTrigger";
    private static final String ROUTE_TEMPLATE = "short/{itemId}";

    // Environment variable keys
    private static final String DB_URL = "DB_URL";
    private static final String DB_KEY = "DB_KEY";
    private static final String DB_NAME = "DB_NAME";
    private static final String CONTAINER_TABLE_NAME = "shorts";

    private static final CosmosClient cosmosClient;
    private static final CosmosContainer cosmosContainer;

    private static final String endpoint = System.getenv(DB_URL);
    private static final String key = System.getenv(DB_KEY);
    private static final String databaseName = System.getenv(DB_NAME);

    // Initialize the Cosmos client and container
    static {
        System.out.println("What did Frankenstein say to the dog?");
        System.out.println("IT'S ALIVA!! :P");
        System.out.println("Connecting to Cosmos DB at " + endpoint);
        cosmosClient = new CosmosClientBuilder()
                .endpoint(endpoint)
                .key(key)
                .consistencyLevel(ConsistencyLevel.STRONG)
                .directMode() // alternatively, use gateway mode
                .buildClient();

        CosmosDatabase database = cosmosClient.getDatabase(databaseName);
        cosmosContainer = database.getContainer(CONTAINER_TABLE_NAME);
        System.out.println("KABOOM??");
    }

    @FunctionName(FUNCTION_IDENTIFIER)
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = HTTP_TRIGGER_NAME,
                    methods = {HttpMethod.GET},
                    route = ROUTE_TEMPLATE
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("itemId") String itemId,
            final ExecutionContext executionContext
    ) {
        try {
            // Retrieve the item from Cosmos DB
            Short item = cosmosContainer.readItem(itemId, new PartitionKey(itemId), Short.class).getItem();
            if (item == null) {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("Item not found")
                        .build();
            }

            // Increment the view count
            item.incrementViewCount();

            // Update the item in Cosmos DB
            CosmosItemResponse<Short> updateResponse = cosmosContainer.upsertItem(item);

            // Return the updated item
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(updateResponse.getItem())
                    .build();

        } catch (CosmosException cosmosException) {
            executionContext.getLogger().severe("Cosmos DB error: " + cosmosException.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database operation failed")
                    .build();
        } catch (Exception exception) {
            executionContext.getLogger().severe("Unhandled exception: " + exception.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred")
                    .build();
        }
    }
}
