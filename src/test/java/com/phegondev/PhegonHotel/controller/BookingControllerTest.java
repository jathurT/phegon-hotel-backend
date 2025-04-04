package com.phegondev.PhegonHotel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.phegondev.PhegonHotel.dto.Response;
import com.phegondev.PhegonHotel.entity.Booking;
import com.phegondev.PhegonHotel.service.interfac.IBookingService;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class BookingControllerTest {

  private MockMvc mockMvc;

  private ObjectMapper objectMapper;

  @Mock
  private IBookingService bookingService;

  @InjectMocks
  private BookingController bookingController;

  @BeforeEach
  public void setup() {
    // Configure ObjectMapper to handle Java 8 date/time types
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    mockMvc = MockMvcBuilders
            .standaloneSetup(bookingController)
            .build();
  }

  @Test
  public void testGetAllBookings_Admin() throws Exception {
    // Arrange
    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("successful");
    mockResponse.setBookingList(new ArrayList<>());

    when(bookingService.getAllBookings()).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(get("/api/bookings/all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("successful"));

    verify(bookingService, times(1)).getAllBookings();
  }

  @Test
  public void testBookRoom_Success() throws Exception {
    // Arrange
    BookingDTO bookingDTO = new BookingDTO();
    bookingDTO.setCheckInDate(LocalDate.now().plusDays(1));
    bookingDTO.setCheckOutDate(LocalDate.now().plusDays(3));
    bookingDTO.setNumOfAdults(2);
    bookingDTO.setNumOfChildren(1);

    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("successful");
    mockResponse.setBookingConfirmationCode("ABCD1234");

    when(bookingService.saveBooking(anyLong(), anyLong(), any(Booking.class))).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(post("/api/bookings/book-room/1/2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bookingDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.bookingConfirmationCode").value("ABCD1234"));

    verify(bookingService, times(1)).saveBooking(anyLong(), anyLong(), any(Booking.class));
  }

  @Test
  public void testGetBookingByConfirmationCode_Success() throws Exception {
    // Arrange
    String confirmationCode = "ABCD1234";

    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("successful");
    // Add booking details to response as needed

    when(bookingService.findBookingByConfirmationCode(confirmationCode)).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(get("/api/bookings/get-by-confirmation-code/" + confirmationCode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("successful"));

    verify(bookingService, times(1)).findBookingByConfirmationCode(confirmationCode);
  }

  @Test
  public void testCancelBooking_Success() throws Exception {
    // Arrange
    Long bookingId = 1L;

    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("successful");

    when(bookingService.cancelBooking(bookingId)).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(delete("/api/bookings/cancel/" + bookingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("successful"));

    verify(bookingService, times(1)).cancelBooking(bookingId);
  }

  // Simple DTO to avoid Jackson serialization issues with Booking entity
  private static class BookingDTO {
    @Setter
    private LocalDate checkInDate;
    @Setter
    private LocalDate checkOutDate;
    private int numOfAdults;
    private int numOfChildren;
    private int totalNumOfGuest;

    // Getters and setters
    public LocalDate getCheckInDate() { return checkInDate; }

    public LocalDate getCheckOutDate() { return checkOutDate; }

    public int getNumOfAdults() { return numOfAdults; }
    public void setNumOfAdults(int numOfAdults) {
      this.numOfAdults = numOfAdults;
      calculateTotalNumOfGuest();
    }
    public int getNumOfChildren() { return numOfChildren; }
    public void setNumOfChildren(int numOfChildren) {
      this.numOfChildren = numOfChildren;
      calculateTotalNumOfGuest();
    }
    public int getTotalNumOfGuest() { return totalNumOfGuest; }

    private void calculateTotalNumOfGuest() {
      this.totalNumOfGuest = this.numOfAdults + this.numOfChildren;
    }
  }
}