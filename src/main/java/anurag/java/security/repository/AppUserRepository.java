package anurag.java.security.repository;

import java.util.Optional;

import anurag.java.security.entity.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, String> {

    @EntityGraph(attributePaths = "authorities")
    Optional<AppUser> findByUsername(String username);
}
