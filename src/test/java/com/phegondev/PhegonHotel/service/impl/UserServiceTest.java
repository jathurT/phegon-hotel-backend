package com.phegondev.PhegonHotel.service.impl;

import com.phegondev.PhegonHotel.dto.LoginRequest;
import com.phegondev.PhegonHotel.dto.Response;
import com.phegondev.PhegonHotel.entity.Booking;
import com.phegondev.PhegonHotel.entity.Room;
import com.phegondev.PhegonHotel.entity.User;
import com.phegondev.PhegonHotel.repo.UserRepository;
import com.phegondev.PhegonHotel.utils.JWTUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for UserService
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JWTUtils jwtUtils;

  @Mock
  private AuthenticationManager authenticationManager;

  @InjectMocks
  private UserService userService;

  private User testUser;
  private LoginRequest loginRequest;

  @BeforeEach
  public void setup() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setEmail("test@example.com");
    testUser.setName("Test User");
    testUser.setPhoneNumber("1234567890");
    testUser.setPassword("rawPassword");
    testUser.setRole("USER");

    loginRequest = new LoginRequest();
    loginRequest.setEmail("test@example.com");
    loginRequest.setPassword("password");
  }

//  @Test
//  public void testRegister_Success() {
//    // Arrange
//    when(userRepository.existsByEmail(anyString())).thenReturn(false);
//    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
//    when(userRepository.save(any(User.class))).thenReturn(testUser);
//
//    // Act
//    Response response = userService.register(testUser);
//
//    // Assert
//    assertEquals(200, response.getStatusCode());
//    assertNotNull(response.getUser());
//    assertEquals(testUser.getEmail(), response.getUser().getEmail());
//
//    verify(userRepository).existsByEmail(testUser.getEmail());
//    verify(passwordEncoder).encode(testUser.getPassword());
//    verify(userRepository).save(testUser);
//  }

  @Test
  public void testRegister_UserAlreadyExists() {
    // Arrange
    when(userRepository.existsByEmail(anyString())).thenReturn(true);

    // Act
    Response response = userService.register(testUser);

    // Assert
    assertEquals(400, response.getStatusCode());
    assertTrue(response.getMessage().contains("Already Exists"));

    verify(userRepository).existsByEmail(testUser.getEmail());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  public void testLogin_Success() {
    // Arrange
    Authentication authentication = mock(Authentication.class);
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
    when(jwtUtils.generateToken(any(User.class))).thenReturn("jwt-token-here");

    // Act
    Response response = userService.login(loginRequest);

    // Assert
    assertEquals(200, response.getStatusCode());
    assertEquals("successful", response.getMessage());
    assertEquals("jwt-token-here", response.getToken());
    assertEquals("USER", response.getRole());

    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    verify(userRepository).findByEmail(loginRequest.getEmail());
    verify(jwtUtils).generateToken(testUser);
  }

  @Test
  public void testLogin_UserNotFound() {
    // Arrange
    Authentication authentication = mock(Authentication.class);
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

    // Act
    Response response = userService.login(loginRequest);

    // Assert
    assertEquals(404, response.getStatusCode());
    assertTrue(response.getMessage().contains("user Not found"));

    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    verify(userRepository).findByEmail(loginRequest.getEmail());
    verify(jwtUtils, never()).generateToken(any(User.class));
  }

  @Test
  public void testGetAllUsers_Success() {
    // Arrange
    List<User> userList = new ArrayList<>();
    userList.add(testUser);

    when(userRepository.findAll()).thenReturn(userList);

    // Act
    Response response = userService.getAllUsers();

    // Assert
    assertEquals(200, response.getStatusCode());
    assertEquals("successful", response.getMessage());
    assertEquals(1, response.getUserList().size());
    assertEquals(testUser.getEmail(), response.getUserList().get(0).getEmail());

    verify(userRepository).findAll();
  }

  @Test
  public void testGetUserById_Success() {
    // Arrange
    when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));

    // Act
    Response response = userService.getUserById("1");

    // Assert
    assertEquals(200, response.getStatusCode());
    assertEquals("successful", response.getMessage());
    assertNotNull(response.getUser());
    assertEquals(testUser.getEmail(), response.getUser().getEmail());

    verify(userRepository).findById(1L);
  }

  @Test
  public void testGetUserById_UserNotFound() {
    // Arrange
    when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

    // Act
    Response response = userService.getUserById("1");

    // Assert
    assertEquals(404, response.getStatusCode());
    assertTrue(response.getMessage().contains("User Not Found"));

    verify(userRepository).findById(1L);
  }

//  @Test
//  public void testDeleteUser_Success() {
//    // Arrange
//    when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
//    doNothing().when(userRepository).deleteById(anyLong());
//
//    // Act
//    Response response = userService.deleteUser("1");
//
//    // Assert
//    assertEquals(200, response.getStatusCode());
//    assertEquals("successful", response.getMessage());
//
//    verify(userRepository).findById(1L);
//    verify(userRepository).deleteById(1L);
//  }

  @Test
  public void testDeleteUser_UserNotFound() {
    // Arrange
    when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

    // Act
    Response response = userService.deleteUser("1");

    // Assert
    assertEquals(404, response.getStatusCode());
    assertTrue(response.getMessage().contains("User Not Found"));

    verify(userRepository).findById(1L);
    verify(userRepository, never()).deleteById(anyLong());
  }

  @Test
  public void testGetMyInfo_Success() {
    // Arrange
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

    // Act
    Response response = userService.getMyInfo("test@example.com");

    // Assert
    assertEquals(200, response.getStatusCode());
    assertEquals("successful", response.getMessage());
    assertNotNull(response.getUser());
    assertEquals(testUser.getEmail(), response.getUser().getEmail());

    verify(userRepository).findByEmail("test@example.com");
  }

  @Test
  public void testGetMyInfo_UserNotFound() {
    // Arrange
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

    // Act
    Response response = userService.getMyInfo("nonexistent@example.com");

    // Assert
    assertEquals(404, response.getStatusCode());
    assertTrue(response.getMessage().contains("User Not Found"));

    verify(userRepository).findByEmail("nonexistent@example.com");
  }

  @Test
  public void testGetUserBookingHistory_Success() {
    // Arrange
    // Add some bookings to the test user
    List<Booking> bookings = new ArrayList<>();
    Booking booking = new Booking();
    booking.setId(1L);
    booking.setCheckInDate(LocalDate.now().plusDays(1));
    booking.setCheckOutDate(LocalDate.now().plusDays(3));
    booking.setUser(testUser);
    Room room = new Room();
    room.setId(1L);
    room.setRoomType("DELUXE");
    room.setRoomPrice(BigDecimal.valueOf(199.99));
    booking.setRoom(room);
    bookings.add(booking);
    testUser.setBookings(bookings);

    when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));

    // Act
    Response response = userService.getUserBookingHistory("1");

    // Assert
    assertEquals(200, response.getStatusCode());
    assertEquals("successful", response.getMessage());
    assertNotNull(response.getUser());
    assertNotNull(response.getUser().getBookings());
    assertEquals(1, response.getUser().getBookings().size());

    verify(userRepository).findById(1L);
  }
}