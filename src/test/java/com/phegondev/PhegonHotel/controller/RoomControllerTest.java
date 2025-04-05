package com.phegondev.PhegonHotel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.phegondev.PhegonHotel.dto.Response;
import com.phegondev.PhegonHotel.service.interfac.IBookingService;
import com.phegondev.PhegonHotel.service.interfac.IRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class RoomControllerTest {

  private MockMvc mockMvc;

  private ObjectMapper objectMapper;

  @Mock
  private IRoomService roomService;

  @Mock
  private IBookingService bookingService;

  @InjectMocks
  private RoomController roomController;

  @BeforeEach
  public void setup() {
    // Configure ObjectMapper to handle Java 8 date/time types
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    // Use standalone setup - no security, no application context
    mockMvc = MockMvcBuilders
            .standaloneSetup(roomController)
            .build();
  }

  @Test
  public void testAddNewRoom_Success() throws Exception {
    // Arrange
    MockMultipartFile photoFile = new MockMultipartFile(
            "photo", "room.jpg", MediaType.IMAGE_JPEG_VALUE, "photo content".getBytes());

    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("successful");

    when(roomService.addNewRoom(any(), anyString(), any(BigDecimal.class), anyString())).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.multipart("/api/rooms/add")
                    .file(photoFile)
                    .param("roomType", "DELUXE")
                    .param("roomPrice", "199.99")
                    .param("roomDescription", "Luxury room with sea view"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("successful"));

    verify(roomService, times(1)).addNewRoom(any(), eq("DELUXE"), any(BigDecimal.class), eq("Luxury room with sea view"));
  }

  @Test
  public void testAddNewRoom_MissingRequiredFields() throws Exception {
    // Arrange - empty photo file
    MockMultipartFile emptyPhoto = new MockMultipartFile(
            "photo", "", MediaType.IMAGE_JPEG_VALUE, new byte[0]);

    // Act & Assert - Missing photo
    mockMvc.perform(MockMvcRequestBuilders.multipart("/api/rooms/add")
                    .file(emptyPhoto)
                    .param("roomType", "DELUXE")
                    .param("roomPrice", "199.99")
                    .param("roomDescription", "Luxury room with sea view"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.message").value("Please provide values for all fields(photo, roomType,roomPrice)"));

    // Act & Assert - Missing roomType
    MockMultipartFile photoFile = new MockMultipartFile(
            "photo", "room.jpg", MediaType.IMAGE_JPEG_VALUE, "photo content".getBytes());

    mockMvc.perform(MockMvcRequestBuilders.multipart("/api/rooms/add")
                    .file(photoFile)
                    .param("roomPrice", "199.99")
                    .param("roomDescription", "Luxury room with sea view"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.message").value("Please provide values for all fields(photo, roomType,roomPrice)"));

    // Act & Assert - Missing roomPrice
    mockMvc.perform(MockMvcRequestBuilders.multipart("/api/rooms/add")
                    .file(photoFile)
                    .param("roomType", "DELUXE")
                    .param("roomDescription", "Luxury room with sea view"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.message").value("Please provide values for all fields(photo, roomType,roomPrice)"));

    // Verify service was never called
    verify(roomService, never()).addNewRoom(any(), anyString(), any(BigDecimal.class), anyString());
  }

  @Test
  public void testGetAllRooms_Success() throws Exception {
    // Arrange
    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("successful");
    mockResponse.setRoomList(new ArrayList<>());

    when(roomService.getAllRooms()).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(get("/api/rooms/all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("successful"));

    verify(roomService, times(1)).getAllRooms();
  }

  @Test
  public void testGetAllAvailableRooms_Success() throws Exception {
    // Arrange
    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("successful");
    mockResponse.setRoomList(new ArrayList<>());

    when(roomService.getAllAvailableRooms()).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(get("/api/rooms/all-available-rooms"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("successful"));

    verify(roomService, times(1)).getAllAvailableRooms();
  }

  @Test
  public void testGetRoomTypes_Success() throws Exception {
    // Arrange
    List<String> roomTypes = List.of("STANDARD", "DELUXE", "SUITE");

    when(roomService.getAllRoomTypes()).thenReturn(roomTypes);

    // Act & Assert
    mockMvc.perform(get("/api/rooms/types"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").value("STANDARD"))
            .andExpect(jsonPath("$[1]").value("DELUXE"))
            .andExpect(jsonPath("$[2]").value("SUITE"));

    verify(roomService, times(1)).getAllRoomTypes();
  }

  @Test
  public void testGetRoomById_Success() throws Exception {
    // Arrange
    Long roomId = 1L;

    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("successful");
    // Set room details in response as needed

    when(roomService.getRoomById(roomId)).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(get("/api/rooms/room-by-id/" + roomId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("successful"));

    verify(roomService, times(1)).getRoomById(roomId);
  }

  @Test
  public void testGetAvailableRoomsByDateAndType_Success() throws Exception {
    // Arrange
    LocalDate checkInDate = LocalDate.now().plusDays(1);
    LocalDate checkOutDate = LocalDate.now().plusDays(3);
    String roomType = "DELUXE";

    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("successful");
    mockResponse.setRoomList(new ArrayList<>());

    when(roomService.getAvailableRoomsByDataAndType(any(LocalDate.class), any(LocalDate.class), anyString()))
            .thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(get("/api/rooms/available-rooms-by-date-and-type")
                    .param("checkInDate", checkInDate.toString())
                    .param("checkOutDate", checkOutDate.toString())
                    .param("roomType", roomType))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("successful"));

    verify(roomService, times(1)).getAvailableRoomsByDataAndType(eq(checkInDate), eq(checkOutDate), eq(roomType));
  }

  @Test
  public void testGetAvailableRoomsByDateAndType_MissingParameters() throws Exception {
    // Arrange
    LocalDate checkInDate = LocalDate.now().plusDays(1);
    LocalDate checkOutDate = LocalDate.now().plusDays(3);
    String roomType = "DELUXE";

    // Act & Assert - Missing checkInDate
    mockMvc.perform(get("/api/rooms/available-rooms-by-date-and-type")
                    .param("checkOutDate", checkOutDate.toString())
                    .param("roomType", roomType))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.message").value("Please provide values for all fields(checkInDate, roomType,checkOutDate)"));

    // Act & Assert - Missing checkOutDate
    mockMvc.perform(get("/api/rooms/available-rooms-by-date-and-type")
                    .param("checkInDate", checkInDate.toString())
                    .param("roomType", roomType))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.message").value("Please provide values for all fields(checkInDate, roomType,checkOutDate)"));

    // Act & Assert - Missing roomType
    mockMvc.perform(get("/api/rooms/available-rooms-by-date-and-type")
                    .param("checkInDate", checkInDate.toString())
                    .param("checkOutDate", checkOutDate.toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.message").value("Please provide values for all fields(checkInDate, roomType,checkOutDate)"));

    // Act & Assert - Empty roomType
    mockMvc.perform(get("/api/rooms/available-rooms-by-date-and-type")
                    .param("checkInDate", checkInDate.toString())
                    .param("checkOutDate", checkOutDate.toString())
                    .param("roomType", ""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.message").value("Please provide values for all fields(checkInDate, roomType,checkOutDate)"));

    // Verify service was never called
    verify(roomService, never()).getAvailableRoomsByDataAndType(any(), any(), anyString());
  }

  @Test
  public void testUpdateRoom_Success() throws Exception {
    // Arrange
    Long roomId = 1L;
    MockMultipartFile photoFile = new MockMultipartFile(
            "photo", "updated-room.jpg", MediaType.IMAGE_JPEG_VALUE, "updated photo content".getBytes());

    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("successful");

    when(roomService.updateRoom(anyLong(), anyString(), anyString(), any(BigDecimal.class), any()))
            .thenReturn(mockResponse);

    // Act & Assert - Use PUT method explicitly for multipart
    MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
            .multipart("/api/rooms/update/" + roomId)
            .file(photoFile)
            .param("roomType", "SUITE")
            .param("roomPrice", "299.99")
            .param("roomDescription", "Updated luxury suite");

    // Change to PUT method
    builder.with(request -> {
      request.setMethod("PUT");
      return request;
    });

    mockMvc.perform(builder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("successful"));

    verify(roomService, times(1)).updateRoom(eq(roomId), eq("Updated luxury suite"), eq("SUITE"), any(BigDecimal.class), any());
  }

//  @Test
//  public void testUpdateRoom_WithoutPhoto() throws Exception {
//    // Arrange
//    Long roomId = 1L;
//    MockMultipartFile emptyPhoto = new MockMultipartFile(
//            "photo", "", MediaType.IMAGE_JPEG_VALUE, new byte[0]);
//
//    // Create a default response if service returns null
//    Response mockResponse = new Response();
//    mockResponse.setStatusCode(200);
//    mockResponse.setMessage("successful");
//
//    // Ensure a non-null response is always returned
//    when(roomService.updateRoom(
//            eq(roomId),
//            eq("Updated luxury suite"),
//            eq("SUITE"),
//            eq(BigDecimal.valueOf(299.99)),
//            isNull()
//    )).thenReturn(mockResponse);
//
//    // Act & Assert - Use PUT method explicitly for multipart
//    MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
//            .multipart("/api/rooms/update/" + roomId)
//            .file(emptyPhoto)
//            .param("roomType", "SUITE")
//            .param("roomPrice", "299.99")
//            .param("roomDescription", "Updated luxury suite");
//
//    // Change to PUT method
//    builder.with(request -> {
//      request.setMethod("PUT");
//      return request;
//    });
//
//    mockMvc.perform(builder)
//            .andExpect(status().isOk())
//            .andExpect(jsonPath("$.statusCode").value(200))
//            .andExpect(jsonPath("$.message").value("successful"));
//
//    // Verify that updateRoom is called with the correct parameters
//    verify(roomService, times(1)).updateRoom(
//            eq(roomId),
//            eq("Updated luxury suite"),
//            eq("SUITE"),
//            eq(BigDecimal.valueOf(299.99)),
//            isNull()
//    );
//  }

  @Test
  public void testDeleteRoom_Success() throws Exception {
    // Arrange
    Long roomId = 1L;

    Response mockResponse = new Response();
    mockResponse.setStatusCode(200);
    mockResponse.setMessage("successful");

    when(roomService.deleteRoom(roomId)).thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(delete("/api/rooms/delete/" + roomId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("successful"));

    verify(roomService, times(1)).deleteRoom(roomId);
  }
}