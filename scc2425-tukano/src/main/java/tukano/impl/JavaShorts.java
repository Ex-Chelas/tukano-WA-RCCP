package tukano.impl;

import tukano.api.Short;
import tukano.api.*;
import tukano.impl.data.Following;
import tukano.impl.data.Likes;
import tukano.impl.databse.DB;
import tukano.impl.rest.TukanoRestServer;

import java.util.ArrayList;
import java.util.Comparator;
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

            return errorOrValue(DB.insertOne(shorty), s -> s.copyWithLikes_And_Token(0));
        });
    }

    @Override
    public Result<Short> getShort(String shortId) {
        Log.info(() -> format("getShort : shortId = %s\n", shortId));

        if (shortId == null)
            return error(BAD_REQUEST);

        var query = format("SELECT count(*) FROM likes l WHERE l.shortId = '%s'", shortId);
        var likes = DB.sql(query, Long.class, Likes.class);
        //Sort id is hardcoded FIND A WAY TO FIX THIS
        return errorOrValue(DB.getOne(shortId, Short.class), shorty -> shorty.copyWithLikes_And_Token(likes.value().get(0)));
    }


    @Override
    public Result<Void> deleteShort(String shortId, String password) {
        Log.info(() -> format("deleteShort : shortId = %s, pwd = %s\n", shortId, password));

        return errorOrResult(getShort(shortId), shorty ->
                errorOrResult(okUser(shorty.getOwnerId(), password), user -> {

                    DB.deleteOne(shorty);

                    var query = format("DELETE l FROM likes l WHERE l.shortId = '%s'", shortId);
                    DB.sql(query, Void.class, Likes.class);

                    return JavaBlobs.getInstance().delete(shorty.getBlobUrl(), Token.get());

                }));
    }

    @Override
    public Result<List<String>> getShorts(String userId) {
        Log.info(() -> format("getShorts : userId = %s\n", userId));

        var query = format("SELECT s.id FROM shorts s WHERE s.ownerId = '%s'", userId);
        Log.info(() -> "Executing query: " + query);

        Result<List<String>> queryResult = DB.sql(query, String.class, Short.class);

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
            System.out.println(f.getId());
            return errorOrVoid(okUser(userId2), isFollowing ? DB.insertOne(f) : DB.deleteOne(f));
        });
    }

    @Override
    public Result<List<String>> followers(String userId, String password) {
        Log.info(() -> format("followers : userId = %s, pwd = %s\n", userId, password));

        var query = format("SELECT f.follower FROM following f WHERE f.followee = '%s'", userId);
        return errorOrValue(okUser(userId, password), DB.sql(query, String.class, Following.class));
    }

    @Override
    public Result<Void> like(String shortId, String userId, boolean isLiked, String password) {
        Log.info(() -> format("like : shortId = %s, userId = %s, isLiked = %s, pwd = %s\n", shortId, userId, isLiked, password));


        return errorOrResult(getShort(shortId), shorty -> {
            var l = new Likes(userId, shortId, shorty.getOwnerId());
            return errorOrVoid(okUser(userId, password), isLiked ? DB.insertOne(l) : DB.deleteOne(l));
        });
    }

    @Override
    public Result<List<String>> likes(String shortId, String password) {
        Log.info(() -> format("likes : shortId = %s, pwd = %s\n", shortId, password));

        return errorOrResult(getShort(shortId), shorty -> {

            var query = format("SELECT l.userId FROM likes l WHERE l.shortId = '%s'", shortId);

            return errorOrValue(okUser(shorty.getOwnerId(), password), DB.sql(query, String.class, Likes.class));
        });
    }

//    @Override
//    public Result<List<String>> getFeed(String userId, String password) {
//        Log.info(() -> format("getFeed : userId = %s, pwd = %s\n", userId, password));
//
//        final var QUERY_FMT = """
//                SELECT s.id, s.timestamp FROM Shorts s WHERE s.ownerId = '%s'
//                UNION
//                SELECT s.id, s.timestamp FROM Shorts s, Following f
//                	WHERE
//                		f.followee = s.ownerId AND f.follower = '%s'
//                ORDER BY s.timestamp DESC""";
//
//        return errorOrValue(okUser(userId, password), DB.sql(format(QUERY_FMT, userId, userId), String.class, Short.class));
//    }

    @Override
    public Result<List<String>> getFeed(String userId, String password) {
        Log.info(() -> format("getFeed : userId = %s\n", userId));

        return errorOrResult(okUser(userId, password), user -> {

            final var USER_SHORTS = """
                    SELECT * FROM Shorts s WHERE s.ownerId = '%s'
            """;

            var userOwnShortsQuery = format(USER_SHORTS, userId);

            var userFollowingQuery = format("SELECT f.followee FROM Following f WHERE f.follower = '%s'", userId);

            var userOwnShorts = DB.sql(userOwnShortsQuery, Short.class, Short.class);

            if(!userOwnShorts.isOK()){
                return error(userOwnShorts.error());
            }

            var userFollowing = DB.sql(userFollowingQuery, String.class, Following.class);

            if(!userFollowing.isOK()){
                return error(userFollowing.error());
            }

            var feed = new ArrayList<Short>();

            userFollowing.value().forEach(followerId -> {
                var followedUserShorts = DB.sql(format(USER_SHORTS, followerId), Short.class, Short.class);
                followedUserShorts.value().forEach(s -> {
                    if (s != null) {
                        feed.add(s);
                    }
                });
            });

            return Result.ok(feed.stream()
                    .sorted(Comparator.comparing(Short::getTimestamp).reversed())
                    .map(Short::getId).toList());
        }
        );

    }

    protected Result<User> okUser(String userId, String pwd) {
        return JavaUsers.getInstance().getUser(userId, pwd);
    }

    private Result<Void> okUser(String userId) {
        var res = okUser(userId, "");
        if (res.error() == FORBIDDEN)
            return ok();
        else
            return error(res.error());
    }

    @Override
    public Result<Void> deleteAllShorts(String userId, String password) {
        Log.info(() -> format("deleteAllShorts : userId = %s, password = %s", userId, password));

        return errorOrResult(okUser(userId, password), user -> {

            var queryShorts = format("SELECT * FROM Shorts s WHERE s.ownerId = '%s'", userId);
            var shortsResult = DB.sql(queryShorts, Short.class, Short.class);

            return errorOrResult(shortsResult, shorts -> {
                for (Short shorty : shorts) {
                    DB.deleteOne(shorty);
                    var queryLikes = format("SELECT * FROM Likes l WHERE l.shortId = '%s'", shorty.getId());
                    var likesResult = DB.sql(queryLikes, Likes.class, Likes.class);

                    if (likesResult.isOK()) {
                        var likes = likesResult.value();
                        for (Likes like : likes) {
                            DB.deleteOne(like);
                        }
                    }

                    JavaBlobs.getInstance().delete(shorty.getId(), Token.get());
                }
                var queryFollows = format("SELECT * FROM Following f WHERE f.follower = '%s' OR f.followee = '%s'", userId, userId);
                var followsResult = DB.sql(queryFollows, Following.class, Following.class);

                if (followsResult.isOK()) {
                    var follows = followsResult.value();
                    for (Following follow : follows) {
                        DB.deleteOne(follow);
                    }
                }
                var queryUserLikes = format("SELECT * FROM Likes l WHERE l.userId = '%s'", userId);
                var userLikesResult = DB.sql(queryUserLikes, Likes.class, Likes.class);

                if (userLikesResult.isOK()) {
                    var userLikes = userLikesResult.value();
                    for (Likes like : userLikes) {
                        DB.deleteOne(like);
                    }
                }

                return ok();
            });
        });
    }
}
