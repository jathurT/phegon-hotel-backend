package com.phegondev.PhegonHotel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phegondev.PhegonHotel.dto.LoginRequest;
import com.phegondev.PhegonHotel.dto.Response;
import com.phegondev.PhegonHotel.entity.User;
import com.phegondev.PhegonHotel.service.interfac.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

  private MockMvc mockMvc;

  private ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private IUserService userService;

  @InjectMocks
  private AuthController authController;

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders
            .standaloneSetup(authController)
            .build();
  }

  @Test
  public void testRegister_Success() throws Exception {
    // Create a DTO version of User instead of using the entity directly
    // This avoids the serialization issues with UserDetails implementation
    UserDTO userDTO = new UserDTO();
    userDTO.setName("Test User");
    userDTO.setEmail("test@example.com");
    userDTO.setPassword("password");
    userDTO.setPhoneNumber("1234567890");

    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("User registered successfully");

    // Configure the mock to accept any User object
    when(userService.register(any(User.class))).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(userDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("User registered successfully"));

    verify(userService, times(1)).register(any(User.class));
  }

  @Test
  public void testRegister_Failure() throws Exception {
    // Create a DTO version of User instead of using the entity directly
    UserDTO userDTO = new UserDTO();
    userDTO.setName("Test User");
    userDTO.setEmail("existing@example.com");
    userDTO.setPassword("password");
    userDTO.setPhoneNumber("1234567890");

    Response mockResponse = new Response();
    mockResponse.setStatusCode(400);
    mockResponse.setMessage("Email already exists");

    when(userService.register(any(User.class))).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(userDTO)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.message").value("Email already exists"));

    verify(userService, times(1)).register(any(User.class));
  }

  @Test
  public void testLogin_Success() throws Exception {
    // Arrange
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setEmail("test@example.com");
    loginRequest.setPassword("password");

    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setToken("jwt-token-here");
    mockResponse.setRole("USER");
    mockResponse.setMessage("successful");

    when(userService.login(any(LoginRequest.class))).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.token").value("jwt-token-here"))
            .andExpect(jsonPath("$.role").value("USER"));

    verify(userService, times(1)).login(any(LoginRequest.class));
  }

  @Test
  public void testLogin_Failure_InvalidCredentials() throws Exception {
    // Arrange
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setEmail("test@example.com");
    loginRequest.setPassword("wrong-password");

    Response mockResponse = new Response();
    mockResponse.setStatusCode(500);
    mockResponse.setMessage("Error Occurred During User Login");

    when(userService.login(any(LoginRequest.class))).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.statusCode").value(500));

    verify(userService, times(1)).login(any(LoginRequest.class));
  }

  @Test
  public void testHello() throws Exception {
    // Simple test for the hello endpoint
    mockMvc.perform(get("/api/auth/hello"))
            .andExpect(status().isOk())
            .andExpect(content().string("Hello World"));
  }

  // Simple DTO class to avoid Jackson serialization issues with User entity
  private static class UserDTO {
    private String name;
    private String email;
    private String password;
    private String phoneNumber;
    private String role = "USER"; // Default value to avoid null

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
  }
}