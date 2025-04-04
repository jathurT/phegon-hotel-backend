package com.phegondev.PhegonHotel.utils;

import com.phegondev.PhegonHotel.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JWTUtilsTest {

  private JWTUtils jwtUtils;
  private User testUser;

  @BeforeEach
  public void setup() {
    // Create a real instance of JWTUtils with our test secret key
    String TEST_SECRET_KEY = "843567893696976453275974432697R634976R738467TR678T34865R6834R8763T478378637664538745673865783678548735687R3";
    jwtUtils = new JWTUtils(TEST_SECRET_KEY);

    // Create a test user
    testUser = new User();
    testUser.setId(1L);
    testUser.setEmail("test@example.com");
    testUser.setName("Test User");
    testUser.setPassword("password");
    testUser.setRole("USER");
  }

  @Test
  public void testGenerateToken() {
    // Act
    String token = jwtUtils.generateToken(testUser);

    // Assert
    assertNotNull(token);
    assertTrue(token.length() > 0);

    // The token should have 3 parts (header.payload.signature)
    String[] parts = token.split("\\.");
    assertEquals(3, parts.length);
  }

  @Test
  public void testExtractUsername() {
    // Arrange
    String token = jwtUtils.generateToken(testUser);

    // Act
    String username = jwtUtils.extractUsername(token);

    // Assert
    assertEquals("test@example.com", username);
  }

  @Test
  public void testIsValidToken_ValidToken() {
    // Arrange
    String token = jwtUtils.generateToken(testUser);

    // Act
    boolean isValid = jwtUtils.isValidToken(token, testUser);

    // Assert
    assertTrue(isValid);
  }

  @Test
  public void testIsValidToken_InvalidUsername() {
    // Arrange
    String token = jwtUtils.generateToken(testUser);

    User differentUser = new User();
    differentUser.setEmail("different@example.com");
    differentUser.setPassword("password");

    // Act
    boolean isValid = jwtUtils.isValidToken(token, differentUser);

    // Assert
    assertFalse(isValid);
  }

  @Test
  public void testTokenConsistency() {
    // Generate a token
    String token1 = jwtUtils.generateToken(testUser);

    // Generate a second token for the same user
    String token2 = jwtUtils.generateToken(testUser);

    // In the current implementation, tokens for the same user generated in quick succession
    // appear to be identical. This is implementation-specific behavior.
    assertEquals(token1, token2, "Tokens for same user should be consistent in current implementation");

    // Both tokens should be valid
    assertTrue(jwtUtils.isValidToken(token1, testUser));
    assertTrue(jwtUtils.isValidToken(token2, testUser));
  }

  @Test
  public void testInvalidToken() {
    // Try to extract username from an invalid token format
    String invalidToken = "invalid.token.format";

    // This likely throws an exception with the current implementation
    assertThrows(Exception.class, () -> {
      jwtUtils.extractUsername(invalidToken);
    });
  }

  @Test
  public void testMalformedToken() {
    // A token with valid format but invalid signature
    String malformedToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNjE1MjM5MDIyLCJleHAiOjE2MTUyNDkwMjJ9.INVALID_SIGNATURE";

    // In this implementation, invalid signatures throw SignatureException
    assertThrows(io.jsonwebtoken.security.SignatureException.class, () -> {
      jwtUtils.isValidToken(malformedToken, testUser);
    });
  }

  @Test
  public void testTokenStructure() {
    // Generate a token
    String token = jwtUtils.generateToken(testUser);

    // Extract username and check it
    String username = jwtUtils.extractUsername(token);
    assertEquals(testUser.getEmail(), username);
  }

  @Test
  public void testWithDifferentSecretKey() {
    // Create a JWT utils with a different secret key
    JWTUtils differentJwtUtils = new JWTUtils("differentSecretKey000000000000000000000000000000000000000000000000000000000");

    // Generate a token with the different key
    String token = differentJwtUtils.generateToken(testUser);

    // The original jwtUtils should not validate it (different signing key)
    assertThrows(Exception.class, () -> {
      jwtUtils.isValidToken(token, testUser);
    });
  }

  /**
   * Note: We're skipping the expiration test as it would require a different approach
   * based on implementation details. In a real-world scenario, you would need either:
   * 1. Make JWTUtils more testable by adding method to set expiration time for tests
   * 2. Use a clock/time provider that can be mocked in tests
   */
}