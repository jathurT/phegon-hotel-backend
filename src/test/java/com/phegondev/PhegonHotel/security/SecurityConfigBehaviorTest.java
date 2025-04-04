package com.phegondev.PhegonHotel.security;

import com.phegondev.PhegonHotel.entity.User;
import com.phegondev.PhegonHotel.repo.UserRepository;
import com.phegondev.PhegonHotel.service.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityConfigBehaviorTest {

  @Mock
  private CustomUserDetailsService customUserDetailsService;

  @Mock
  private JWTAuthFilter jwtAuthFilter;

  @Mock
  private UserRepository userRepository;

  @Mock
  private HttpSecurity httpSecurity;

  @Mock
  private AuthenticationConfiguration authenticationConfiguration;

  @Captor
  private ArgumentCaptor<User> userCaptor;

  @InjectMocks
  private SecurityConfig securityConfig;

  @Test
  @DisplayName("Should add JWT filter before UsernamePasswordAuthenticationFilter")
  public void testJwtFilterAddedInCorrectOrder() throws Exception {
    // Setup mocks for fluent HttpSecurity API
    when(httpSecurity.csrf(any())).thenReturn(httpSecurity);
    when(httpSecurity.cors(any())).thenReturn(httpSecurity);
    when(httpSecurity.authorizeHttpRequests(any())).thenReturn(httpSecurity);
    when(httpSecurity.sessionManagement(any())).thenReturn(httpSecurity);
    when(httpSecurity.authenticationProvider(any())).thenReturn(httpSecurity);
    when(httpSecurity.addFilterBefore(any(), any())).thenReturn(httpSecurity);
    when(httpSecurity.build()).thenReturn((DefaultSecurityFilterChain) mock(SecurityFilterChain.class));

    // Execute
    securityConfig.securityFilterChain(httpSecurity);

    // Verify the JWT filter is added before the UsernamePasswordAuthenticationFilter
    verify(httpSecurity).addFilterBefore(eq(jwtAuthFilter), eq(UsernamePasswordAuthenticationFilter.class));
  }

  @Test
  @DisplayName("Should initialize admin user with provided credentials")
  public void testAdminUserInitialization() throws Exception {
    // Setup
    ReflectionTestUtils.setField(securityConfig, "adminEmail", "admin@example.com");
    ReflectionTestUtils.setField(securityConfig, "adminName", "Admin User");
    ReflectionTestUtils.setField(securityConfig, "adminPhone", "9876543210");
    ReflectionTestUtils.setField(securityConfig, "adminPassword", "adminPassword");
    ReflectionTestUtils.setField(securityConfig, "adminRole", "ADMIN");

    when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());

    // Execute
    securityConfig.initDatabase(userRepository).run();

    // Capture and verify
    verify(userRepository).save(userCaptor.capture());
    User savedUser = userCaptor.getValue();

    assertEquals("Admin User", savedUser.getName());
    assertEquals("admin@example.com", savedUser.getEmail());
    assertEquals("9876543210", savedUser.getPhoneNumber());
    assertEquals("ADMIN", savedUser.getRole());

    // Password should be encoded
    assertNotNull(savedUser.getPassword());
    assertNotEquals("adminPassword", savedUser.getPassword());
  }

  @Test
  @DisplayName("Should not create admin user if it already exists")
  public void testAdminUserNotCreatedWhenExists() throws Exception {
    // Setup
    ReflectionTestUtils.setField(securityConfig, "adminEmail", "admin@example.com");
    User existingUser = new User();
    when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existingUser));

    // Execute
    securityConfig.initDatabase(userRepository).run();

    // Verify
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("AuthenticationManager should be retrieved from configuration")
  public void testAuthenticationManagerRetrieval() throws Exception {
    // Setup
    AuthenticationManager mockAuthManager = mock(AuthenticationManager.class);
    when(authenticationConfiguration.getAuthenticationManager()).thenReturn(mockAuthManager);

    // Execute
    AuthenticationManager result = securityConfig.authenticationManager(authenticationConfiguration);

    // Verify
    assertSame(mockAuthManager, result);
    verify(authenticationConfiguration).getAuthenticationManager();
  }
}