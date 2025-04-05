package com.phegondev.PhegonHotel.service;

import com.phegondev.PhegonHotel.entity.User;
import com.phegondev.PhegonHotel.exception.OurException;
import com.phegondev.PhegonHotel.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private CustomUserDetailsService customUserDetailsService;

  private User testUser;

  @BeforeEach
  public void setup() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setEmail("test@example.com");
    testUser.setPassword("password");
    testUser.setRole("USER");
  }

  @Test
  public void testLoadUserByUsername_Success() {
    // Arrange
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

    // Act
    UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

    // Assert
    assertNotNull(userDetails);
    assertEquals("test@example.com", userDetails.getUsername());
    assertEquals("password", userDetails.getPassword());

    verify(userRepository).findByEmail("test@example.com");
  }

  @Test
  public void testLoadUserByUsername_UserNotFound() {
    // Arrange
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(OurException.class, () -> {
      customUserDetailsService.loadUserByUsername("nonexistent@example.com");
    });

    verify(userRepository).findByEmail("nonexistent@example.com");
  }
}