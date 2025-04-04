package com.phegondev.PhegonHotel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phegondev.PhegonHotel.dto.Response;
import com.phegondev.PhegonHotel.dto.UserDTO;
import com.phegondev.PhegonHotel.service.interfac.IUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

  @Mock
  private Authentication authentication;

  @Mock
  private SecurityContext securityContext;

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders
            .standaloneSetup(userController)
            .build();
  }

  @AfterEach
  public void tearDown() {
    // Clean up the security context after each test
    SecurityContextHolder.clearContext();
  }

  @Test
  public void testGetAllUsers_Success() throws Exception {
    // Arrange
    List<UserDTO> userDTOList = Arrays.asList(
            createUserDTO(1L, "user1@example.com", "User One"),
            createUserDTO(2L, "user2@example.com", "User Two")
    );

    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("successful");
    mockResponse.setUserList(userDTOList);

    when(userService.getAllUsers()).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(get("/api/users/all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("successful"))
            .andExpect(jsonPath("$.userList").isArray())
            .andExpect(jsonPath("$.userList.length()").value(2))
            .andExpect(jsonPath("$.userList[0].email").value("user1@example.com"));

    verify(userService, times(1)).getAllUsers();
  }

  @Test
  public void testGetAllUsers_ServerError() throws Exception {
    // Arrange
    Response mockResponse = new Response();
    mockResponse.setStatusCode(500);
    mockResponse.setMessage("Error getting all users");

    when(userService.getAllUsers()).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(get("/api/users/all"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.statusCode").value(500))
            .andExpect(jsonPath("$.message").value("Error getting all users"));

    verify(userService, times(1)).getAllUsers();
  }

  @Test
  public void testGetUserById_Success() throws Exception {
    // Arrange
    String userId = "1";
    UserDTO userDTO = createUserDTO(1L, "user@example.com", "Test User");

    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("successful");
    mockResponse.setUser(userDTO);

    when(userService.getUserById(userId)).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(get("/api/users/get-by-id/" + userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("successful"))
            .andExpect(jsonPath("$.user.id").value(1))
            .andExpect(jsonPath("$.user.email").value("user@example.com"))
            .andExpect(jsonPath("$.user.name").value("Test User"));

    verify(userService, times(1)).getUserById(userId);
  }

  @Test
  public void testGetUserById_NotFound() throws Exception {
    // Arrange
    String userId = "999";

    Response mockResponse = new Response();
    mockResponse.setStatusCode(404);
    mockResponse.setMessage("User Not Found");

    when(userService.getUserById(userId)).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(get("/api/users/get-by-id/" + userId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.statusCode").value(404))
            .andExpect(jsonPath("$.message").value("User Not Found"));

    verify(userService, times(1)).getUserById(userId);
  }

  @Test
  public void testDeleteUser_Success() throws Exception {
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
  public void testDeleteUser_NotFound() throws Exception {
    // Arrange
    String userId = "999";

    Response mockResponse = new Response();
    mockResponse.setStatusCode(404);
    mockResponse.setMessage("User Not Found");

    when(userService.deleteUser(userId)).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(delete("/api/users/delete/" + userId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.statusCode").value(404))
            .andExpect(jsonPath("$.message").value("User Not Found"));

    verify(userService, times(1)).deleteUser(userId);
  }

  @Test
  public void testGetLoggedInUserProfile_Success() throws Exception {
    // Arrange
    String email = "test@example.com";
    UserDTO userDTO = createUserDTO(1L, email, "Test User");

    // Setup security context
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(email);
    SecurityContextHolder.setContext(securityContext);

    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("successful");
    mockResponse.setUser(userDTO);

    when(userService.getMyInfo(email)).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(get("/api/users/get-logged-in-profile-info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("successful"))
            .andExpect(jsonPath("$.user.email").value(email));

    verify(userService, times(1)).getMyInfo(email);
  }

  @Test
  public void testGetUserBookingHistory_Success() throws Exception {
    // Arrange
    String userId = "1";
    UserDTO userDTO = createUserDTO(1L, "user@example.com", "Test User");
    // Add booking history if needed

    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("successful");
    mockResponse.setUser(userDTO);

    when(userService.getUserBookingHistory(userId)).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(get("/api/users/get-user-bookings/" + userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("successful"))
            .andExpect(jsonPath("$.user.id").value(1))
            .andExpect(jsonPath("$.user.email").value("user@example.com"));

    verify(userService, times(1)).getUserBookingHistory(userId);
  }

  @Test
  public void testGetUserBookingHistory_UserNotFound() throws Exception {
    // Arrange
    String userId = "999";

    Response mockResponse = new Response();
    mockResponse.setStatusCode(404);
    mockResponse.setMessage("User Not Found");

    when(userService.getUserBookingHistory(userId)).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(get("/api/users/get-user-bookings/" + userId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.statusCode").value(404))
            .andExpect(jsonPath("$.message").value("User Not Found"));

    verify(userService, times(1)).getUserBookingHistory(userId);
  }

  // Helper method to create a UserDTO for testing
  private UserDTO createUserDTO(Long id, String email, String name) {
    UserDTO userDTO = new UserDTO();
    userDTO.setId(id);
    userDTO.setEmail(email);
    userDTO.setName(name);
    userDTO.setPhoneNumber("1234567890");
    userDTO.setRole("USER");
    userDTO.setBookings(new ArrayList<>());
    return userDTO;
  }
}