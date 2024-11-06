package tukano.impl;

import tukano.api.Short;
import tukano.api.*;
import tukano.impl.data.Following;
import tukano.impl.data.Likes;
import tukano.impl.rest.TukanoRestServer;
import utils.CosmosDB;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static java.lang.String.format;
import static tukano.api.Result.ErrorCode.BAD_REQUEST;
import static tukano.api.Result.ErrorCode.FORBIDDEN;
import static tukano.api.Result.*;

public class JavaShorts implements Shorts {

    private static final Logger Log = Logger.getLogger(JavaShorts.class.getName());

    private static Shorts instance;

    private JavaShorts() {
    }

    synchronized public static Shorts getInstance() {
        if (instance == null)
            instance = new JavaShorts();
        return instance;
    }

    @Override
    public Result<Short> createShort(String userId, String password) {
        Log.info(() -> format("createShort : userId = %s, pwd = %s\n", userId, password));

        return errorOrResult(okUser(userId, password), user -> {

            var shortId = format("%s+%s", userId, UUID.randomUUID());
            var blobUrl = format("%s/%s/%s", TukanoRestServer.serverURI, Blobs.NAME, shortId);
            var shorty = new Short(shortId, userId, blobUrl);

            return errorOrValue(CosmosDB.insertOne(Short.class, shorty), s -> s.copyWithLikes_And_Token(0));
        });
    }

    @Override
    public Result<Short> getShort(String shortId) {
        Log.info(() -> format("getShort : shortId = %s\n", shortId));

        if (shortId == null)
            return error(BAD_REQUEST);

        var query = format("SELECT count(*) FROM Likes l WHERE l.shortId = '%s'", shortId);
        var likes = CosmosDB.sql(query, Long.class, Likes.class);
        //Sort id is hardcoded FIND A WAY TO FIX THIS
        return errorOrValue(CosmosDB.getOne(Short.class, "shortId", shortId, Short.class), shorty -> shorty.copyWithLikes_And_Token(likes.value().get(0)));
    }


    @Override
    public Result<Void> deleteShort(String shortId, String password) {
        Log.info(() -> format("deleteShort : shortId = %s, pwd = %s\n", shortId, password));

        return errorOrResult(getShort(shortId), shorty ->
                errorOrResult(okUser(shorty.getOwnerId(), password), user ->
                        CosmosDB.transaction(tr -> {

                            CosmosDB.deleteOne(Short.class, shorty);

                            var query = format("DELETE Likes FROM Likes l WHERE l.shortId = '%s'", shortId);
                            CosmosDB.sql(query, Void.class, Likes.class);

                            JavaBlobs.getInstance().delete(shorty.getBlobUrl(), Token.get());
                        })));
    }

    @Override
    public Result<List<String>> getShorts(String userId) {
        Log.info(() -> format("getShorts : userId = %s\n", userId));

        var query = format("SELECT s.id FROM Shorts s WHERE s.ownerId = '%s'", userId);
        Log.info(() -> "Executing query: " + query);

        Result<List<String>> queryResult = CosmosDB.sql(query, String.class, Short.class);

        if (queryResult.isOK()) {
            Log.info(() -> "Query returned " + queryResult.value().size() + " shortId(s).");
        } else {
            Log.warning(() -> "Query failed with error: " + queryResult.error());
        }

        return queryResult;
    }


    @Override
    public Result<Void> follow(String userId1, String userId2, boolean isFollowing, String password) {
        Log.info(() -> format("follow : userId1 = %s, userId2 = %s, isFollowing = %s, pwd = %s\n", userId1, userId2, isFollowing, password));


        return errorOrResult(okUser(userId1, password), user -> {
            var f = new Following(userId1, userId2);
            return errorOrVoid(okUser(userId2), isFollowing ? CosmosDB.insertOne(Following.class, f) : CosmosDB.deleteOne(Following.class, f));
        });
    }

    @Override
    public Result<List<String>> followers(String userId, String password) {
        Log.info(() -> format("followers : userId = %s, pwd = %s\n", userId, password));

        var query = format("SELECT f.follower FROM Following f WHERE f.followee = '%s'", userId);
        return errorOrValue(okUser(userId, password), CosmosDB.sql(query, String.class, Following.class));
    }

    @Override
    public Result<Void> like(String shortId, String userId, boolean isLiked, String password) {
        Log.info(() -> format("like : shortId = %s, userId = %s, isLiked = %s, pwd = %s\n", shortId, userId, isLiked, password));


        return errorOrResult(getShort(shortId), shorty -> {
            var l = new Likes(userId, shortId, shorty.getOwnerId());
            return errorOrVoid(okUser(userId, password), isLiked ? CosmosDB.insertOne(Likes.class, l) : CosmosDB.deleteOne(Likes.class, l));
        });
    }

    @Override
    public Result<List<String>> likes(String shortId, String password) {
        Log.info(() -> format("likes : shortId = %s, pwd = %s\n", shortId, password));

        return errorOrResult(getShort(shortId), shorty -> {

            var query = format("SELECT l.userId FROM Likes l WHERE l.shortId = '%s'", shortId);

            return errorOrValue(okUser(shorty.getOwnerId(), password), CosmosDB.sql(query, String.class, Likes.class));
        });
    }

    @Override
    public Result<List<String>> getFeed(String userId, String password) {
        Log.info(() -> format("getFeed : userId = %s, pwd = %s\n", userId, password));

        final var QUERY_FMT = """
                SELECT s.shortId, s.timestamp FROM Shorts s WHERE s.ownerId = '%s'
                UNION
                SELECT s.shortId, s.timestamp FROM Shorts s, Following f
                	WHERE
                		f.followee = s.ownerId AND f.follower = '%s'
                ORDER BY s.timestamp DESC""";

        return errorOrValue(okUser(userId, password), CosmosDB.sql(format(QUERY_FMT, userId, userId), String.class, Short.class));
    }

    protected Result<User> okUser(String userId, String pwd) {
        return JavaUsers.getInstance().getUser(userId, pwd);
    }

    private Result<Void> okUser(String userId) {
        var res = okUser(userId, "" );
        if (res.error() == FORBIDDEN)
            return ok();
        else
            return error(res.error());
    }

    @Override
    public Result<Void> deleteAllShorts(String userId, String password, String token) {
        Log.info(() -> format("deleteAllShorts : userId = %s, password = %s, token = %s\n", userId, password, token));

        if (!Token.isValid(token, userId))
            return error(FORBIDDEN);

        return CosmosDB.transaction(tr -> {

            //delete shorts
            var query1 = format("DELETE Short s WHERE s.ownerId = '%s'", userId);
            CosmosDB.sql(query1, Void.class, Short.class);

            //delete follows
            var query2 = format("DELETE Following f WHERE f.follower = '%s' OR f.followee = '%s'", userId, userId);
            CosmosDB.sql(query2, Void.class, Following.class);

            //delete likes
            var query3 = format("DELETE Likes l WHERE l.ownerId = '%s' OR l.userId = '%s'", userId, userId);
            CosmosDB.sql(query3, Void.class, Likes.class);
        });
    }

}