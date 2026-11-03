package anurag.java.security.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AppUserAuthorityId implements Serializable {

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "authority", nullable = false, length = 100)
    private String authority;

    protected AppUserAuthorityId() {
    }

    public AppUserAuthorityId(String username, String authority) {
        this.username = username;
        this.authority = authority;
    }

    public String getUsername() {
        return username;
    }

    public String getAuthority() {
        return authority;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AppUserAuthorityId that)) {
            return false;
        }
        return Objects.equals(username, that.username)
            && Objects.equals(authority, that.authority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, authority);
    }
}
