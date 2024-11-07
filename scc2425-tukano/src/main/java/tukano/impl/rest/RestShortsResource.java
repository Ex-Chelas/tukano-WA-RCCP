package tukano.impl.rest;

import jakarta.inject.Singleton;
import tukano.api.Short;
import tukano.api.Shorts;
import tukano.api.rest.RestShorts;
import tukano.impl.JavaShorts;
import tukano.impl.cache.RedisCache;
import tukano.impl.data.Following;
import utils.JSON;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
public class RestShortsResource extends RestResource implements RestShorts {

    static final Shorts impl = JavaShorts.getInstance();
    private static final String USER_SHORTS_KEY = "userShorts:";
    private static final String SHORT_KEY = "short:";
    private static final String USER_FOLLOWERS_KEY = "followers:";
    private static final String SHORT_LIKES_KEY = "likes:";

    /**
     * Get all keys of users that are followed by userId
     *
     * @param userId user id
     * @return set of keys
     */
    private static Set<String> getFollowedKeys(String userId) {
        try (var jedis = RedisCache.getCachePool().getResource()) {
            Set<String> keys = jedis.keys(USER_FOLLOWERS_KEY + "*");
            Set<String> result = new HashSet<>();
            for (var key : keys) {
                var value = jedis.get(key);
                if (value != null) {
                    var followers = new Following().fromString(JSON.decode(value, String.class));
                    if (followers.getFollower() != null && followers.getFollower().equals(userId)) {
                        result.add(key);
                    }
                }
            }
            return result;
        }
    }

    @Override
    public Short createShort(String userId, String password) {
        if (!RedisCache.isCacheEnabled()) {
            return super.resultOrThrow(impl.createShort(userId, password));
        }

        try (var jedis = RedisCache.getCachePool().getResource()) {
            // creates a short unconditionally
            var createdShort = super.resultOrThrow(impl.createShort(userId, password));
            // shorts id belonging to the user
            var key = USER_SHORTS_KEY + userId;
            if (jedis.exists(key)) {
                jedis.lpush(key, JSON.encode(createdShort.getId()));
            } else {
                jedis.set(key, JSON.encode(createdShort.getId()));
            }

            //shorts on cache
            var shortKey = SHORT_KEY + createdShort.getId();
            jedis.set(shortKey, JSON.encode(createdShort));
            return createdShort;
        }
    }

    @Override
    public void deleteShort(String shortId, String password) {
        if (RedisCache.isCacheEnabled()) {
            try (var jedis = RedisCache.getCachePool().getResource()) {
                var key = SHORT_KEY + shortId;
                var value = jedis.get(key);
                if (value != null) {
                    Short decodedShort = new Short().fromString(JSON.decode(value, String.class));
                    jedis.del(key);
                    var userShortsKey = USER_SHORTS_KEY + decodedShort.getOwnerId();
                    jedis.lrem(userShortsKey, 0, shortId);
                }
            }
        }
        super.resultOrThrow(impl.deleteShort(shortId, password));
    }

    @Override
    public Short getShort(String shortId) {
        if (!RedisCache.isCacheEnabled()) {
            return super.resultOrThrow(impl.getShort(shortId));
        }
        try (var jedis = RedisCache.getCachePool().getResource()) {
            var key = SHORT_KEY + shortId;
            var value = jedis.get(key);
            if (value != null) {
                return new Short().fromString(JSON.decode(value, String.class));
            }
            var shorty = super.resultOrThrow(impl.getShort(shortId));
            jedis.set(key, JSON.encode(shorty));
            return shorty;
        }
    }

    @Override
    public List<String> getShorts(String userId) {
        if (!RedisCache.isCacheEnabled()) {
            return super.resultOrThrow(impl.getShorts(userId));
        }
        try (var jedis = RedisCache.getCachePool().getResource()) {
            var key = USER_SHORTS_KEY + userId;
            var value = jedis.get(key);
            if (value != null) {
                return JSON.decode(value, List.class);
            }
            var shorts = super.resultOrThrow(impl.getShorts(userId));
            jedis.set(key, JSON.encode(shorts));
            return shorts;
        }
    }

    @Override
    public void follow(String userId1, String userId2, boolean isFollowing, String password) {
        super.resultOrThrow(impl.follow(userId1, userId2, isFollowing, password));
        if (!RedisCache.isCacheEnabled()) {
            return;
        }
        try (var jedis = RedisCache.getCachePool().getResource()) {
            var key = USER_FOLLOWERS_KEY + userId2;
            if (isFollowing) {
                jedis.lpush(key, userId1);
            } else {
                jedis.lrem(key, 0, userId1);
            }
        }
    }

    @Override
    public List<String> followers(String userId, String password) {
        if (!RedisCache.isCacheEnabled()) {
            return super.resultOrThrow(impl.followers(userId, password));
        }
        try (var jedis = RedisCache.getCachePool().getResource()) {
            var key = USER_FOLLOWERS_KEY + userId;
            var value = jedis.get(key);
            if (value != null) {
                return JSON.decode(value, List.class);
            }
            var followers = super.resultOrThrow(impl.followers(userId, password));
            jedis.set(key, JSON.encode(followers));
            return followers;
        }
    }

    @Override
    public void like(String shortId, String userId, boolean isLiked, String password) {
        super.resultOrThrow(impl.like(shortId, userId, isLiked, password));
        if (!RedisCache.isCacheEnabled()) {
            return;
        }
        try (var jedis = RedisCache.getCachePool().getResource()) {
            var key = SHORT_LIKES_KEY + shortId;
            if (isLiked) {
                jedis.lpush(key, userId);
            } else {
                jedis.lrem(key, 0, userId);
            }
        }
    }

    @Override
    public List<String> likes(String shortId, String password) {
        if (!RedisCache.isCacheEnabled()) {
            return super.resultOrThrow(impl.likes(shortId, password));
        }
        try (var jedis = RedisCache.getCachePool().getResource()) {
            var key = SHORT_LIKES_KEY + shortId;
            var value = jedis.get(key);
            if (value != null) {
                return JSON.decode(value, List.class);
            }
            var likes = super.resultOrThrow(impl.likes(shortId, password));
            jedis.set(key, JSON.encode(likes));
            return likes;
        }
    }

    @Override
    public List<String> getFeed(String userId, String password) {
        if (!RedisCache.isCacheEnabled()) {
            return super.resultOrThrow(impl.getFeed(userId, password));
        }
        try (var jedis = RedisCache.getCachePool().getResource()) {
            var followedKeys = getFollowedKeys(userId);
            List<String> feed = new ArrayList<>();
            for (var followedKey : followedKeys) {
                var followed = jedis.get(USER_SHORTS_KEY + followedKey);
                if (followed != null) {
                    var shorts = JSON.decode(followed, List.class);
                    assert shorts != null;
                    feed.addAll(shorts);
                }
            }
            // if the list is empty, then gets the feed from the database and then caches the shorts
            if (feed.isEmpty()) {
                feed = super.resultOrThrow(impl.getFeed(userId, password));
                for (var shortId : feed) {
                    var shortKey = SHORT_KEY + shortId;
                    var shorty = jedis.get(shortKey);
                    if (shorty == null) {
                        var toCacheShort = super.resultOrThrow(impl.getShort(shortId));
                        jedis.set(shortKey, JSON.encode(toCacheShort));
                    }
                }
            }
            return feed;
        }
    }

    @Override
    public void deleteAllShorts(String userId, String password) {
        super.resultOrThrow(impl.deleteAllShorts(userId, password));
        if (!RedisCache.isCacheEnabled()) {
            return;
        }
        try (var jedis = RedisCache.getCachePool().getResource()) {
            var key = USER_SHORTS_KEY + userId;
            var userShorts = jedis.get(key);
            if (userShorts != null) {
                var shorts = JSON.decode(userShorts, List.class);
                assert shorts != null;
                for (var shortId : shorts) {
                    var shortKey = SHORT_KEY + shortId;
                    var likesKey = SHORT_LIKES_KEY + shortId;
                    jedis.del(shortKey);
                    jedis.del(likesKey);
                }
                jedis.del(USER_FOLLOWERS_KEY + userId);
                jedis.del(key);

            }
        }
    }
}
