package tukano.impl.rest;

import jakarta.inject.Singleton;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import tukano.api.Blobs;
import tukano.api.rest.RestBlobs;
import tukano.impl.JavaBlobs;
import tukano.impl.rest.utils.SessionUtils;

@Singleton
public class RestBlobsResource extends RestResource implements RestBlobs {

	private static final String COOKIE_KEY = "tukano:session";
	private static final String ADMIN_USER = "admin";

	final Blobs impl;

	public RestBlobsResource() {
		this.impl = JavaBlobs.getInstance();
	}

	private NewCookie buildCookie(String name, String value) {
		return new NewCookie.Builder(name)
				.value(value)
				.path("/")
				.build();
	}

	@Override
	public void upload(@CookieParam(COOKIE_KEY) String cookieValue, @PathParam(BLOB_ID) String blobId, byte[] bytes, @QueryParam(TOKEN) String token) {
		Cookie cookie = buildCookie(COOKIE_KEY, cookieValue);
		SessionUtils.validateSession(cookie, null);
		super.resultOrThrow(impl.upload(blobId, bytes, token));
	}

	@Override
	public byte[] download(@CookieParam(COOKIE_KEY) String cookieValue, @PathParam(BLOB_ID) String blobId, @QueryParam(TOKEN) String token) {
		Cookie cookie = buildCookie(COOKIE_KEY, cookieValue);
		SessionUtils.validateSession(cookie, null);
		return super.resultOrThrow(impl.download(blobId, token));
	}

	@Override
	public void delete(@CookieParam(COOKIE_KEY) String cookieValue, @PathParam(BLOB_ID) String blobId, @QueryParam(TOKEN) String token) {
		Cookie cookie = buildCookie(COOKIE_KEY, cookieValue);
		SessionUtils.validateSession(cookie, ADMIN_USER);
		super.resultOrThrow(impl.delete(blobId, token));
	}

	@Override
	public void deleteAllBlobs(@CookieParam(COOKIE_KEY) String cookieValue, @PathParam(USER_ID) String userId, @QueryParam(TOKEN) String token) {
		Cookie cookie = buildCookie(COOKIE_KEY, cookieValue);
		SessionUtils.validateSession(cookie, ADMIN_USER);
		super.resultOrThrow(impl.deleteAllBlobs(userId, token));
	}
}
