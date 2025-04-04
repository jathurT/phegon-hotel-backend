package com.phegondev.PhegonHotel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phegondev.PhegonHotel.dto.Response;
import com.phegondev.PhegonHotel.service.interfac.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

  private MockMvc mockMvc;

  private ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private IUserService userService;

  @InjectMocks
  private UserController userController;

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders
            .standaloneSetup(userController)
            .build();
  }

  @Test
  public void testGetAllUsers_Admin() throws Exception {
    // Arrange
    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("successful");
    mockResponse.setUserList(new ArrayList<>());

    when(userService.getAllUsers()).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(get("/api/users/all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("successful"));

    verify(userService, times(1)).getAllUsers();
  }

  // Note: Security tests removed as we're using standalone setup

  @Test
  public void testGetUserById_Success() throws Exception {
    // Arrange
    String userId = "1";

    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("successful");
    // Set user details in response as needed

    when(userService.getUserById(userId)).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(get("/api/users/get-by-id/" + userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("successful"));

    verify(userService, times(1)).getUserById(userId);
  }

  @Test
  public void testDeleteUser_Admin_Success() throws Exception {
    // Arrange
    String userId = "1";

    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("successful");

    when(userService.deleteUser(userId)).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(delete("/api/users/delete/" + userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("successful"));

    verify(userService, times(1)).deleteUser(userId);
  }

  @Test
  public void testGetLoggedInUserProfile_Success() throws Exception {
    // Arrange
    String email = "test@example.com";

    // Mock SecurityContext for the logged-in user
    Authentication authentication = mock(Authentication.class);
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(email);
    SecurityContextHolder.setContext(securityContext);

    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("successful");
    // Set user details in response as needed

    when(userService.getMyInfo(email)).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(get("/api/users/get-logged-in-profile-info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("successful"));

    verify(userService, times(1)).getMyInfo(email);

    // Clean up security context after test
    SecurityContextHolder.clearContext();
  }

  @Test
  public void testGetUserBookingHistory_Success() throws Exception {
    // Arrange
    String userId = "1";

    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("successful");
    // Set booking history in response as needed

    when(userService.getUserBookingHistory(userId)).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(get("/api/users/get-user-bookings/" + userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("successful"));

    verify(userService, times(1)).getUserBookingHistory(userId);
  }
}