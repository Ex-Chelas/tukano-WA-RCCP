package tukano.impl.rest.utils;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Cookie;
import tukano.api.Session;
import tukano.impl.cache.RedisCache;

import static tukano.api.rest.RestBlobs.COOKIE_KEY;

public class SessionUtils {

    public static Session validateSession(Cookie cookie, String requiredUser) throws NotAuthorizedException {
        if (cookie == null || cookie.getValue() == null) {
            throw new NotAuthorizedException("No session initialized");
        }

        String sessionId = cookie.getValue();

        if (!RedisCache.isCacheEnabled()) {
            return new Session(sessionId, requiredUser != null ? requiredUser : "unknown");
        }

        try (var jedis = RedisCache.getCachePool().getResource()) {
            String userId = jedis.get(COOKIE_KEY + ":" + sessionId);
            if (userId == null) {
                throw new NotAuthorizedException("Session expired or invalid");
            }

            if (requiredUser != null && !requiredUser.equals(userId)) {
                throw new NotAuthorizedException("Unauthorized user");
            }

            return new Session(sessionId, userId);
        }
    }

}

