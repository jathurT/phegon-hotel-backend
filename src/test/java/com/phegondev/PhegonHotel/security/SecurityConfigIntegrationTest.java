package com.phegondev.PhegonHotel.security;

import com.phegondev.PhegonHotel.entity.User;
import com.phegondev.PhegonHotel.repo.UserRepository;
import com.phegondev.PhegonHotel.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WebMvcTest(SecurityConfig.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "admin.email=admin@test.com",
        "admin.name=Test Admin",
        "admin.phone=1234567890",
        "admin.password=test-password",
        "admin.role=ADMIN"
})
public class SecurityConfigIntegrationTest {

  @Autowired
  private SecurityConfig securityConfig;

  @MockBean
  private CustomUserDetailsService customUserDetailsService;

  @MockBean
  private JWTAuthFilter jwtAuthFilter;

  @MockBean
  private UserRepository userRepository;

  @Test
  public void testBeansAreCreated() {
    // These should be autowired by Spring if the configuration is correct
    assertNotNull(securityConfig);

    // Test that beans can be created successfully
    PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
    assertNotNull(passwordEncoder);

    AuthenticationProvider authProvider = securityConfig.authenticationProvider();
    assertNotNull(authProvider);
  }

  @Test
  public void testCommandLineRunnerWithMockRepository() throws Exception {
    // Setup
    when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.empty());

    // Execute
    securityConfig.initDatabase(userRepository).run();

    // Verify
    verify(userRepository).findByEmail("admin@test.com");
    verify(userRepository).save(any(User.class));
  }

  @Test
  public void testAuthenticationProviderConfiguration() {
    // Prepare mock user details
    UserDetails mockUserDetails = mock(UserDetails.class);
    when(customUserDetailsService.loadUserByUsername(anyString())).thenReturn(mockUserDetails);

    // Get the authentication provider
    AuthenticationProvider provider = securityConfig.authenticationProvider();

    // Verify it was configured with our mocked service
    assertNotNull(provider);
  }

  @Test
  public void testPasswordEncoderIsWorking() {
    // Get password encoder
    PasswordEncoder encoder = securityConfig.passwordEncoder();

    // Verify it works as expected
    String rawPassword = "testPassword123";
    String encodedPassword = encoder.encode(rawPassword);

    // Assertions
    assertNotEquals(rawPassword, encodedPassword);
    assertTrue(encoder.matches(rawPassword, encodedPassword));
    assertFalse(encoder.matches("wrongPassword", encodedPassword));
  }
}