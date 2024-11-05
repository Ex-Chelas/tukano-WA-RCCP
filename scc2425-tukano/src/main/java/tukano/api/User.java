package tukano.api;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user" )
public class User {
    public static final String NAME = "users";

    private String _rid; // Cosmos generated unique id of item
    private String _ts; // timestamp of the last update to the item

    @Id
    private String id;
    private String pwd;
    private String email;
    private String displayName;

    public User() {
    }

    public User(String id, String pwd, String email, String displayName) {
        this.pwd = pwd;
        this.email = email;
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public void setId(String userId) {
        this.id = userId;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String userId() {
        return id;
    }

    public String pwd() {
        return pwd;
    }

    public String email() {
        return email;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return "User [userId=" + id + ", pwd=" + pwd + ", email=" + email + ", displayName=" + displayName + "]";
    }

    public User fromString(String str) {
        String[] parts = str.split(",");
        return new User(
                parts[0].split("=")[1],
                parts[1].split("=")[1],
                parts[2].split("=")[1],
                parts[3].split("=")[1]
        );
    }

    public User copyWithoutPassword() {
        return new User(id, "", email, displayName);
    }

    public User updateFrom(User other) {
        return new User(id,
                other.pwd != null ? other.pwd : pwd,
                other.email != null ? other.email : email,
                other.displayName != null ? other.displayName : displayName);
    }
}
