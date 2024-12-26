package tukano.api.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path(RestBlobs.PATH)
public interface RestBlobs {

	String PATH = "/blobs";
	String BLOB_ID = "blobId";
	String TOKEN = "token";
	String BLOBS = "blobs";
	String USER_ID = "userId";
	String COOKIE_KEY = "tukano:session";

	/**
	 * Upload a blob.
	 * Only authenticated users are allowed.
	 *
	 * @param cookie   The session cookie.
	 * @param blobId   The ID of the blob.
	 * @param bytes    The content of the blob.
	 * @param token    The authorization token.
	 */
	@POST
	@Path("/{" + BLOB_ID + "}")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	void upload(@CookieParam(COOKIE_KEY) String cookie,
				@PathParam(BLOB_ID) String blobId,
				byte[] bytes,
				@QueryParam(TOKEN) String token);

	/**
	 * Download a blob.
	 * Only authenticated users are allowed.
	 *
	 * @param cookie   The session cookie.
	 * @param blobId   The ID of the blob.
	 * @param token    The authorization token.
	 * @return         The content of the blob.
	 */
	@GET
	@Path("/{" + BLOB_ID + "}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	byte[] download(@CookieParam(COOKIE_KEY) String cookie,
					@PathParam(BLOB_ID) String blobId,
					@QueryParam(TOKEN) String token);

	/**
	 * Delete a blob.
	 * Only the admin user is allowed.
	 *
	 * @param cookie   The session cookie.
	 * @param blobId   The ID of the blob.
	 * @param token    The authorization token.
	 */
	@DELETE
	@Path("/{" + BLOB_ID + "}")
	void delete(@CookieParam(COOKIE_KEY) String cookie,
				@PathParam(BLOB_ID) String blobId,
				@QueryParam(TOKEN) String token);

	/**
	 * Delete all blobs for a user.
	 * Only the admin user is allowed.
	 *
	 * @param cookie   The session cookie.
	 * @param userId   The user ID whose blobs are to be deleted.
	 * @param token    The authorization token.
	 */
	@DELETE
	@Path("/{" + USER_ID + "}/" + BLOBS)
	void deleteAllBlobs(@CookieParam(COOKIE_KEY) String cookie,
						@PathParam(USER_ID) String userId,
						@QueryParam(TOKEN) String token);
}
