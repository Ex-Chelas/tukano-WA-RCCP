package tukano.api;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import tukano.impl.Token;

/**
 * Represents a Short video uploaded by a user.
 * <p>
 * A short has a unique shortId and is owned by a given user;
 * Comprises a short video, stored as a binary blob at some blob URL;
 * A post also has a number of likes, which can increase or decrease over time. It is the only piece of information that is mutable.
 * A short is timestamped when it is created.
 */
@Entity
@Table(name = "shorts")
public class Short {
    public static final String NAME = "shorts";
    @Id
    String id;
    String ownerId;
    String blobUrl;
    long timestamp;
    int totalLikes;
    private String _rid; // Cosmos generated unique id of item
    private String _ts; // timestamp of the last update to the item

    public Short() {
    }

    public Short(String id, String ownerId, String blobUrl, long timestamp, int totalLikes) {
        super();
        this.id = id;
        this.ownerId = ownerId;
        this.blobUrl = blobUrl;
        this.timestamp = timestamp;
        this.totalLikes = totalLikes;
    }

    public Short(String id, String ownerId, String blobUrl) {
        this(id, ownerId, blobUrl, System.currentTimeMillis(), 0);
    }

    public String getId() {
        return id;
    }

    public void setId(String shortId) {
        this.id = shortId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getBlobUrl() {
        return blobUrl;
    }

    public void setBlobUrl(String blobUrl) {
        this.blobUrl = blobUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getTotalLikes() {
        return totalLikes;
    }

    public void setTotalLikes(int totalLikes) {
        this.totalLikes = totalLikes;
    }

    @Override
    public String toString() {
        return "Short [shortId=" + id + ", ownerId=" + ownerId + ", blobUrl=" + blobUrl + ", timestamp="
                + timestamp + ", totalLikes=" + totalLikes + "]";
    }

    public Short fromString(String str) {
        String[] parts = str.split(",");
        return new Short(
                parts[0].split("=")[1],
                parts[1].split("=")[1],
                parts[2].split("=")[1],
                Long.parseLong(parts[3].split("=")[1]),
                Integer.parseInt(parts[4].split("=")[1])
        );
    }

    public Short copyWithLikes_And_Token(long totLikes) {
        var urlWithToken = String.format("%s?token=%s", blobUrl, Token.get(blobUrl));
        return new Short(id, ownerId, urlWithToken, timestamp, (int) totLikes);
    }
}