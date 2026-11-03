package anurag.java.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import anurag.java.security.entity.AppUser;
import anurag.java.security.entity.AppUserAuthority;
import anurag.java.security.entity.AppUserAuthorityId;
import anurag.java.security.repository.AppUserAuthorityRepository;
import anurag.java.security.repository.AppUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:security;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS security",
	"spring.datasource.username=sa",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.jpa.hibernate.ddl-auto=create-drop"
})
class SecurityApplicationTests {

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private AppUserAuthorityRepository appUserAuthorityRepository;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void loadsHashedUsersAndAuthoritiesFromDatabase() {
		String password = "test-password";
		AppUser appUser = appUserRepository.save(
			new AppUser("test-user", passwordEncoder.encode(password), true));
		appUserAuthorityRepository.save(new AppUserAuthority(
			new AppUserAuthorityId("test-user", "ROLE_USER"),
			appUser));
		entityManager.flush();
		entityManager.clear();

		UserDetails user = userDetailsService.loadUserByUsername("test-user");

		assertTrue(passwordEncoder.matches(password, user.getPassword()));
		assertTrue(user.isEnabled());
		assertTrue(user.getAuthorities().stream()
			.anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
	}
}
