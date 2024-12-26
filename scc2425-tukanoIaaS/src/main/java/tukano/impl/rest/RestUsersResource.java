package tukano.impl.rest;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import tukano.impl.cache.RedisCache;
import tukano.api.User;
import tukano.api.Users;
import tukano.api.rest.RestUsers;
import tukano.impl.JavaUsers;
import tukano.impl.rest.RestResource;
import utils.JSON;

@Singleton
public class RestUsersResource extends RestResource implements RestUsers {

	private static final String USER_KEY = "user:";
	private static final String SEARCH_KEY = "search:";
	private static final String COOKIE_KEY = "tukano:session";
	private static final int MAX_COOKIE_AGE = 7200;

	final Users impl;
	public RestUsersResource() {
		this.impl = JavaUsers.getInstance();
	}
	
	@Override
	public String createUser(User user) {
		return super.resultOrThrow( impl.createUser( user));
	}

	@Override
	public Response getUser(String name, String pwd) {
		if (!RedisCache.isCacheEnabled()) {
			User user = super.resultOrThrow(impl.getUser(name, pwd));
			if (user == null) {
				return Response.status(Response.Status.UNAUTHORIZED).build();
			}

			String sessionId = UUID.randomUUID().toString();
			NewCookie cookie = new NewCookie.Builder(COOKIE_KEY)
					.value(sessionId)
					.path("/")
					.comment("sessionid")
					.maxAge(MAX_COOKIE_AGE)
					.secure(false) // Use true for HTTPS
					.httpOnly(true)
					.build();

			return Response.ok(user)
					.cookie(cookie)
					.build();
		}

		try (var jedis = RedisCache.getCachePool().getResource()) {
			String userKey = USER_KEY + name;
			String cachedUser = jedis.get(userKey);

			User user;
			if (cachedUser != null) {
				user = JSON.decode(cachedUser, User.class);
			} else {
				user = super.resultOrThrow(impl.getUser(name, pwd));
				if (user == null) {
					return Response.status(Response.Status.UNAUTHORIZED).build();
				}

				jedis.set(userKey, JSON.encode(user));
			}

			String sessionKey = COOKIE_KEY + ":" + UUID.randomUUID();
			String cookieValue = sessionKey.substring(COOKIE_KEY.length() + 1);
            assert user != null;
            jedis.set(sessionKey, user.userId());
			jedis.expire(sessionKey, MAX_COOKIE_AGE);

			NewCookie cookie = new NewCookie.Builder(COOKIE_KEY)
					.value(cookieValue)
					.path("/")
					.comment("sessionid")
					.maxAge(MAX_COOKIE_AGE)
					.secure(false) // Use true for HTTPS
					.httpOnly(true)
					.build();

			return Response.ok(user)
					.cookie(cookie)
					.build();
		}
	}


	@Override
	public User updateUser(String name, String pwd, User user) {
		return super.resultOrThrow( impl.updateUser(name, pwd, user));
	}

	@Override
	public User deleteUser(String name, String pwd) {
		return super.resultOrThrow( impl.deleteUser(name, pwd));
	}

	@Override
	public List<User> searchUsers(String pattern) {
		return super.resultOrThrow( impl.searchUsers( pattern));
	}
}
