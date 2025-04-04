//package com.phegondev.PhegonHotel.utils;
//
//import com.phegondev.PhegonHotel.entity.User;
//import io.jsonwebtoken.security.Keys;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import javax.crypto.SecretKey;
//import java.nio.charset.StandardCharsets;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//public class JWTUtilsTest {
//
//  private JWTUtils jwtUtils;
//  private User testUser;
//
//  @BeforeEach
//  public void setup() {
//    // Mock the jwtSecret value
//    String mockSecret = "mySuperSecretKey1234567890123456"; // Ensure this key is at least 32 characters long
//
//    // Create a mock of JWTUtils
//    jwtUtils = Mockito.spy(new JWTUtils());
//
//    // Inject the secret key manually
//    ReflectionTestUtils.setField(jwtUtils, "jwtSecret", mockSecret);
//
//    // Reinitialize the SecretKey manually
//    SecretKey secretKey = Keys.hmacShaKeyFor(mockSecret.getBytes(StandardCharsets.UTF_8));
//    ReflectionTestUtils.setField(jwtUtils, "Key", secretKey);
//
//    testUser = new User();
//    testUser.setEmail("test@example.com");
//    testUser.setPassword("password");
//  }
//
//  @Test
//  public void testGenerateToken() {
//    String token = jwtUtils.generateToken(testUser);
//    assertNotNull(token);
//    assertFalse(token.isEmpty());
//  }
//
//  @Test
//  public void testExtractUsername() {
//    String token = jwtUtils.generateToken(testUser);
//    String username = jwtUtils.extractUsername(token);
//    assertEquals("test@example.com", username);
//  }
//
//  @Test
//  public void testIsValidToken_Valid() {
//    String token = jwtUtils.generateToken(testUser);
//    boolean isValid = jwtUtils.isValidToken(token, testUser);
//    assertTrue(isValid);
//  }
//
//  @Test
//  public void testIsValidToken_InvalidUsername() {
//    String token = jwtUtils.generateToken(testUser);
//    User differentUser = new User();
//    differentUser.setEmail("different@example.com");
//
//    boolean isValid = jwtUtils.isValidToken(token, differentUser);
//    assertFalse(isValid);
//  }
//}
