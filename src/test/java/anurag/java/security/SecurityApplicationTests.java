package anurag.java.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:security;DB_CLOSE_DELAY=-1",
	"spring.datasource.username=sa",
	"spring.datasource.driver-class-name=org.h2.Driver"
})
class SecurityApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void contextLoads() {
	}

	@Test
	void loadsHashedUsersAndAuthoritiesFromDatabase() {
		jdbcTemplate.execute("CREATE SCHEMA security");
		jdbcTemplate.execute("""
			CREATE TABLE security.app_users (
				username VARCHAR(100) PRIMARY KEY,
				password_hash VARCHAR(100) NOT NULL,
				enabled BOOLEAN NOT NULL DEFAULT TRUE
			)
			""");
		jdbcTemplate.execute("""
			CREATE TABLE security.app_user_authorities (
				username VARCHAR(100) NOT NULL,
				authority VARCHAR(100) NOT NULL,
				PRIMARY KEY (username, authority)
			)
			""");

		String password = "test-password";
		jdbcTemplate.update(
			"INSERT INTO security.app_users (username, password_hash) VALUES (?, ?)",
			"test-user",
			passwordEncoder.encode(password));
		jdbcTemplate.update(
			"INSERT INTO security.app_user_authorities (username, authority) VALUES (?, ?)",
			"test-user",
			"ROLE_USER");

		UserDetails user = userDetailsService.loadUserByUsername("test-user");

		assertTrue(passwordEncoder.matches(password, user.getPassword()));
		assertTrue(user.getAuthorities().stream()
			.anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
	}
}
