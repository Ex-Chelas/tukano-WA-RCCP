package tukano.impl.data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "likes")
public class Likes {
    public static final String NAME = "likes";
    private String _rid; // Cosmos generated unique id of item
    private String _ts; // timestamp of the last update to the item

    @Id
    private String id; // Composite ID

    private String userId;
    private String shortId;
    private String ownerId;

    public Likes() {
    }

    public Likes(String userId, String shortId, String ownerId) {
        this.id = generateId(userId, shortId);
        this.userId = userId;
        this.shortId = shortId;
        this.ownerId = ownerId;
    }

    private String generateId(String userId, String shortId) {
        return userId + ":" + shortId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getShortId() {
        return shortId;
    }

    public void setShortId(String shortId) {
        this.shortId = shortId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Likes [userId=" + userId + ", shortId=" + shortId + ", ownerId=" + ownerId + "]";
    }

    public Likes fromString(String str) {
        String[] parts = str.split(",");
        return new Likes(
                parts[0].split("=")[1],
                parts[1].split("=")[1],
                parts[2].split("=")[1]
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerId, shortId, userId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Likes other = (Likes) obj;
        return Objects.equals(ownerId, other.ownerId) && Objects.equals(shortId, other.shortId)
                && Objects.equals(userId, other.userId);
    }

}
