package anurag.java.security;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import anurag.java.security.entity.AppUser;
import anurag.java.security.entity.AppUserAuthority;
import anurag.java.security.entity.AppUserAuthorityId;
import anurag.java.security.repository.AppUserAuthorityRepository;
import anurag.java.security.repository.AppUserRepository;
import anurag.java.security.service.AuthRequest;
import anurag.java.security.service.AuthResponse;
import anurag.java.security.service.AuthService;
import anurag.java.security.service.JwtService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:security;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS security",
	"spring.datasource.username=sa",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"security.jwt.secret=VGhpc0lzQU5vdFJlYWxQcm9kdWN0aW9uS2V5Rm9yVGVzdHMxMjM0NQ=="
})
@AutoConfigureMockMvc
class SecurityApplicationTests {

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private AppUserAuthorityRepository appUserAuthorityRepository;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private AuthService authService;

	@Autowired
	private MockMvc mockMvc;
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

	@Test
	void issuesSignedTokensWithUserAuthorities() {
		String token = jwtService.createToken(
			"token-user", java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));

		Jwt jwt = jwtDecoder.decode(token);

		assertEquals("token-user", jwt.getSubject());
		assertTrue(jwt.getClaimAsStringList("authorities").contains("ROLE_USER"));
	}

	@Test
	void registersAndAuthenticatesUsersAgainstJpa() {
		AuthRequest request = new AuthRequest("jwt-user", "strong-password");
		AuthResponse registration = authService.register(request);
		AuthResponse login = authService.login(request);

		assertEquals("Bearer", registration.tokenType());
		assertEquals("jwt-user", jwtDecoder.decode(registration.accessToken()).getSubject());
		assertEquals("jwt-user", jwtDecoder.decode(login.accessToken()).getSubject());
	}

	@Test
	void protectsEndpointsUsingJwtAuthorities() throws Exception {
		String token = jwtService.createToken(
			"route-user", List.of(new SimpleGrantedAuthority("ROLE_USER")));

		mockMvc.perform(get("/user").header(AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk());
		mockMvc.perform(get("/admin").header(AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/user"))
			.andExpect(status().isUnauthorized());
	}
}
