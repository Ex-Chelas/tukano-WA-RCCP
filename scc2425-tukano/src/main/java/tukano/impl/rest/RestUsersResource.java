package tukano.impl.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.inject.Singleton;
import tukano.api.User;
import tukano.api.Users;
import tukano.api.rest.RestUsers;
import tukano.impl.JavaUsers;
import tukano.impl.cache.RedisCache;
import utils.JSON;

import java.util.List;
import java.util.Set;

@Singleton
public class RestUsersResource extends RestResource implements RestUsers {

    private static final String USER_KEY = "user:";
    private static final String SEARCH_KEY = "search:";

    final Users impl;

    public RestUsersResource() {
        this.impl = JavaUsers.getInstance();
    }

    /**
     * Helper method to get all cache keys related with user
     *
     * @return set of keys
     */
    private static Set<String> getCachedUserKeys() {
        try (var jedis = RedisCache.getCachePool().getResource()) {
            return jedis.keys(USER_KEY + "*");
        }
    }

    /**
     * Helper method to get all cache keys related with search
     *
     * @return set of keys
     */
    private static Set<String> getCachedSearchKeys() {
        try (var jedis = RedisCache.getCachePool().getResource()) {
            return jedis.keys(SEARCH_KEY + "*");
        }
    }

    /**
     * Helper method to invalidate search cache related to a user
     *
     * @param name user name
     */
    private static void invalidateSearchCacheByUser(String name) {
        try (var jedis = RedisCache.getCachePool().getResource()) {
            var keys = getCachedSearchKeys();
            for (var key : keys) {
                var value = jedis.get(key);
                if (value != null) {
                    var users = JSON.decode(value, new TypeReference<List<User>>() {
                    });
                    assert users != null;
                    for (var user : users) {
                        if (user.displayName().equals(name)) {
                            jedis.del(key);
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public String createUser(User user) {
//        try (var jedis = RedisCache.getCachePool().getResource()) {
//            var key = USER_KEY + user.displayName();
//            var value = jedis.get(key);
//            if (value != null) {
//                return "CONFLICT";
//            }
        var createdUser = super.resultOrThrow(impl.createUser(user));
//            jedis.set(key, JSON.encode(user));
//            invalidateSearchCacheByUser(user.displayName());
        return createdUser;
//        }
    }

    @Override
    public User getUser(String name, String pwd) {
//        try (var jedis = RedisCache.getCachePool().getResource()) {
//            var key = USER_KEY + name;
//            var value = jedis.get(key);
//            if (value != null) {
//                return JSON.decode(value, User.class);
//            }
        var user = super.resultOrThrow(impl.getUser(name, pwd));
//            jedis.set(key, JSON.encode(user));
        return user;
//        }
    }

    @Override
    public User updateUser(String name, String pwd, User user) {
//        try (var jedis = RedisCache.getCachePool().getResource()) {
        var updatedUser = super.resultOrThrow(impl.updateUser(name, pwd, user));
//            var key = USER_KEY + name;
//            var oldValue = jedis.get(key);
//            if (oldValue != null) {
//                jedis.del(oldValue);
//            }
//            jedis.set(USER_KEY + updatedUser.displayName(), JSON.encode(user));
//            invalidateSearchCacheByUser(name);
        return updatedUser;
//        }
    }

    @Override
    public User deleteUser(String name, String pwd) {
        var deletedUer = super.resultOrThrow(impl.deleteUser(name, pwd));
//        try (var jedis = RedisCache.getCachePool().getResource()) {
//            var key = USER_KEY + name;
//            var value = jedis.get(key);
//            if (value != null) {
//                jedis.del(key);
//            }
//            invalidateSearchCacheByUser(name);
//        }
        return deletedUer;
    }

    /**
     * Initially, we were thinking on implementing cache on searchUsers.
     * Upon further discussions, we thought that for a search in the database, in order to ALWAYS provide the accurate
     * stored values, we would always need to get them from the database, therefore eliminating the need to try to cache
     * search results.
     *
     * @param pattern
     * @return
     */
    @Override
    public List<User> searchUsers(String pattern) {
        return super.resultOrThrow(impl.searchUsers(pattern));

// Initial cache approach on search.
//        try (var jedis = RedisCache.getCachePool().getResource()) {
//            var key = SEARCH_KEY + pattern;
//            var value = jedis.get(key);
//
//            List<User> users = null;
//            if (value != null) {
//                users = JSON.decode(value, List.class );
//                // Due to type erasure, we will decode this into a list, if's the values are not users, an exception will be thrown
//            }
//            var dbUsers = super.resultOrThrow(impl.searchUsers(pattern));
//            // merge both lists
//            var allUsers = new ArrayList<>(users != null ? users : List.of());
//            allUsers.addAll(dbUsers);
//            if (value != null) {
//                jedis.del(key);
//            }
//            jedis.set(key, JSON.encode(users));
//            return allUsers;
//        }
    }
}
