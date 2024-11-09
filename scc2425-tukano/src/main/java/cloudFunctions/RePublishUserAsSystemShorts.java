package cloudFunctions;

import com.azure.cosmos.*;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import tukano.api.Short;

import java.util.Collections;
import java.util.List;

/**
 * Azure Functions with Timer Trigger.
 * This is just the code of the cloud function, the actual deployment is done in a != project.
 * This function republishes shorts that are not owned by the system user.
 */
public class RePublishUserAsSystemShorts {

    private static final String TIMER_FUNCTION_NAME = "republishUserAsSystemShorts";
    private static final String TIMER_TRIGGER_NAME = TIMER_FUNCTION_NAME + "Trigger";
    private static final String TIMER_TRIGGER_CHRON = "0 */60 * * * *";

    private static final String SYSTEM_USER_ID = "0";

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
        System.out.println("HARVEY: Have you ever head my nazi knock-knock joke Lois?");
        System.out.println("LOIS: No.");
        System.out.println("HARVEY: Knock-knock");
        System.out.println("LOIS: Who's the... *slaps mid sentence* re?");
        System.out.println("HARVEY: WI WILL ASK SE KWESTIONS!!!");
        System.out.println("HARVEY: Now get out of here.");

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


    @FunctionName(TIMER_FUNCTION_NAME)
    public void run(
            @TimerTrigger(name = TIMER_TRIGGER_NAME, schedule = TIMER_TRIGGER_CHRON) String timerInfo,
            ExecutionContext context
    ) {
        context.getLogger().info("Republishing was triggered: " + timerInfo);

        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
        String query = "SELECT * FROM Shorts s WHERE (s.ownerId <> @systemUser) order by s.timestamp DESC LIMIT 10";
        List<SqlParameter> parameters = Collections.singletonList(new SqlParameter("@systemUser", SYSTEM_USER_ID));
        SqlQuerySpec sqlQuerySpec = new SqlQuerySpec(query, parameters);


        List<Short> shorts = cosmosContainer.queryItems(sqlQuerySpec, options, Short.class)
                .iterableByPage()
                .iterator()
                .next()
                .getResults();

        if (shorts.isEmpty()) {
            context.getLogger().info("No shorts to republish");
            return;
        }

        for (Short shortEntity : shorts) {
            try {
                String oldShortId = shortEntity.getId();
                String blobId = oldShortId.split("\\+")[1]; // last part of the shortId == blob URL
                String newShortId = SYSTEM_USER_ID + "+" + blobId; // 0+{blobId}

                String newBlobUrl = shortEntity.getBlobUrl().replace(oldShortId, newShortId);

                Short tukanoRecShort = new Short(
                        newShortId,
                        SYSTEM_USER_ID,
                        newBlobUrl
                );

                cosmosContainer.createItem(tukanoRecShort);
                context.getLogger().info("Republishing created: " + tukanoRecShort);
            } catch (Exception x) {
                context.getLogger().warning("run : " + x);
            }
        }

        context.getLogger().info("Republishing finished");
    }

}
