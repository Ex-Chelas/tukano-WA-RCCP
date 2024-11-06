package tukano.impl.data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "following")
public class Following {
    public static final String NAME = "following";
    private String _rid; // Cosmos generated unique id of item
    private String _ts; // timestamp of the last update to the item

    @Id
    private String id; // Composite ID

    private String follower;
    private String followee;

    public Following() {
    }

    public Following(String follower, String followee) {
        this.id = generateId(follower, followee);
        this.follower = follower;
        this.followee = followee;
    }

    private String generateId(String follower, String followee) {
        return follower + ":" + followee;
    }

    public String getFollower() {
        return follower;
    }

    public void setFollower(String follower) {
        this.follower = follower;
    }

    public String getFollowee() {
        return followee;
    }

    public void setFollowee(String followee) {
        this.followee = followee;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(followee, follower);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Following other = (Following) obj;
        return Objects.equals(followee, other.followee) && Objects.equals(follower, other.follower);
    }

    @Override
    public String toString() {
        return "Following [follower=" + follower + ", followee=" + followee + "]";
    }

    public Following fromString(String str) {
        String[] parts = str.split(",");
        return new Following(parts[0].split("=")[1], parts[1].split("=")[1]);
    }
}