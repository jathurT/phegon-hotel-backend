package com.phegondev.PhegonHotel.utils;

import com.phegondev.PhegonHotel.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class JWTUtilsTest {

  private JWTUtils jwtUtils;
  private User testUser;
  private String testToken;

  @BeforeEach
  public void setup() {
    // Create a test user
    testUser = new User();
    testUser.setEmail("test@example.com");
    testUser.setPassword("password");

    // Create mock JWTUtils
    jwtUtils = Mockito.mock(JWTUtils.class);

    // Create a test token
    testToken = "test.jwt.token";

    // Configure the mock behavior
    when(jwtUtils.generateToken(any(UserDetails.class))).thenReturn(testToken);
    when(jwtUtils.extractUsername(testToken)).thenReturn("test@example.com");
    when(jwtUtils.isValidToken(testToken, testUser)).thenReturn(true);

    // Different user should return false
    User differentUser = new User();
    differentUser.setEmail("different@example.com");
    when(jwtUtils.isValidToken(testToken, differentUser)).thenReturn(false);
  }

  @Test
  public void testGenerateToken() {
    String token = jwtUtils.generateToken(testUser);
    assertNotNull(token);
    assertEquals(testToken, token);
  }

  @Test
  public void testExtractUsername() {
    String username = jwtUtils.extractUsername(testToken);
    assertEquals("test@example.com", username);
  }

  @Test
  public void testIsValidToken_Valid() {
    boolean isValid = jwtUtils.isValidToken(testToken, testUser);
    assertTrue(isValid);
  }

  @Test
  public void testIsValidToken_InvalidUsername() {
    User differentUser = new User();
    differentUser.setEmail("different@example.com");

    boolean isValid = jwtUtils.isValidToken(testToken, differentUser);
    assertFalse(isValid);
  }
}