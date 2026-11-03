package anurag.java.security.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user_authorities", schema = "security")
public class AppUserAuthority {

    @EmbeddedId
    private AppUserAuthorityId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("username")
    @JoinColumn(name = "username", nullable = false)
    private AppUser user;

    protected AppUserAuthority() {
    }

    public AppUserAuthority(AppUserAuthorityId id, AppUser user) {
        this.id = id;
        this.user = user;
    }

    public AppUserAuthorityId getId() {
        return id;
    }
}
