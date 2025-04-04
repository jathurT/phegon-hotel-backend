package com.phegondev.PhegonHotel.repo;

import com.phegondev.PhegonHotel.service.AwsS3Service;
import com.phegondev.PhegonHotel.service.CustomUserDetailsService;
import com.phegondev.PhegonHotel.utils.JWTUtils;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class RepositoryTestConfig {

  @Bean
  public JWTUtils jwtUtils() {
    return mock(JWTUtils.class);
  }

  @Bean
  public AwsS3Service awsS3Service() {
    return mock(AwsS3Service.class);
  }

  @Bean
  public CustomUserDetailsService customUserDetailsService() {
    return mock(CustomUserDetailsService.class);
  }

  @Bean
  public AuthenticationManager authenticationManager() {
    return mock(AuthenticationManager.class);
  }
}