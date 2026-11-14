package anurag.java.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFException;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

@SpringBootTest
class SecurityApplicationTests {

	private static InMemoryDirectoryServer ldapServer;

	@Autowired
	private AuthenticationManager authenticationManager;

	@DynamicPropertySource
	static void ldapProperties(DynamicPropertyRegistry registry) {
		try {
			int port;
			try (ServerSocket socket = new ServerSocket(0)) {
				port = socket.getLocalPort();
			}

			InMemoryDirectoryServerConfig config =
				new InMemoryDirectoryServerConfig("dc=example,dc=org");
			config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", port));
			ldapServer = new InMemoryDirectoryServer(config);
			ldapServer.add(
				"dn: dc=example,dc=org",
				"objectClass: top",
				"objectClass: domain",
				"dc: example");
			ldapServer.add(
				"dn: ou=people,dc=example,dc=org",
				"objectClass: top",
				"objectClass: organizationalUnit",
				"ou: people");
			ldapServer.add(
				"dn: ou=groups,dc=example,dc=org",
				"objectClass: top",
				"objectClass: organizationalUnit",
				"ou: groups");
			ldapServer.add(
				"dn: uid=ldap-user,ou=people,dc=example,dc=org",
				"objectClass: top",
				"objectClass: person",
				"objectClass: organizationalPerson",
				"objectClass: inetOrgPerson",
				"cn: LDAP User",
				"sn: User",
				"uid: ldap-user",
				"userPassword: ldap-password");
			ldapServer.add(
				"dn: cn=USER,ou=groups,dc=example,dc=org",
				"objectClass: top",
				"objectClass: groupOfNames",
				"cn: USER",
				"member: uid=ldap-user,ou=people,dc=example,dc=org");
			ldapServer.startListening();

			registry.add("spring.ldap.urls", () -> "ldap://localhost:" + port);
			registry.add("spring.ldap.base", () -> "dc=example,dc=org");
			registry.add("spring.ldap.username", () -> "");
			registry.add("spring.ldap.password", () -> "");
		} catch (IOException | LDAPException | LDIFException exception) {
			throw new IllegalStateException("Unable to start embedded LDAP for tests", exception);
		}
	}

	@AfterAll
	static void stopLdapServer() {
		if (ldapServer != null) {
			ldapServer.shutDown(true);
		}
	}

	@Test
	void authenticatesLdapUserAndLoadsAuthorities() {
		Authentication authentication = authenticationManager.authenticate(
			UsernamePasswordAuthenticationToken.unauthenticated("ldap-user", "ldap-password"));

		assertTrue(authentication.isAuthenticated());
		assertEquals("ldap-user", authentication.getName());
		assertTrue(authentication.getAuthorities().stream()
			.anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
	}

	@Test
	void rejectsInvalidLdapPassword() {
		assertThrows(AuthenticationException.class, () -> authenticationManager.authenticate(
			UsernamePasswordAuthenticationToken.unauthenticated("ldap-user", "wrong-password")));
	}
}
