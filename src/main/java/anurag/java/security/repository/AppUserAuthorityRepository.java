package anurag.java.security.repository;

import anurag.java.security.entity.AppUserAuthority;
import anurag.java.security.entity.AppUserAuthorityId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserAuthorityRepository
        extends JpaRepository<AppUserAuthority, AppUserAuthorityId> {
}
