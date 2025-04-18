package com.phegondev.PhegonHotel.service.impl;

import com.phegondev.PhegonHotel.dto.Response;
import com.phegondev.PhegonHotel.entity.Room;
import com.phegondev.PhegonHotel.exception.OurException;
import com.phegondev.PhegonHotel.repo.BookingRepository;
import com.phegondev.PhegonHotel.repo.RoomRepository;
import com.phegondev.PhegonHotel.service.AwsS3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomServiceTest {

  @Mock
  private RoomRepository roomRepository;

  @Mock
  private BookingRepository bookingRepository;

  @Mock
  private AwsS3Service awsS3Service;

  @InjectMocks
  private RoomService roomService;

  private Room testRoom;
  private MultipartFile mockPhoto;

  @BeforeEach
  public void setup() {
    testRoom = new Room();
    testRoom.setId(1L);
    testRoom.setRoomType("DELUXE");
    testRoom.setRoomPrice(new BigDecimal("199.99"));
    testRoom.setRoomPhotoUrl("https://example.com/room.jpg");
    testRoom.setRoomDescription("Luxury room with sea view");
    testRoom.setBookings(new ArrayList<>());

    mockPhoto = new MockMultipartFile(
            "photo", "room.jpg", "image/jpeg", "photo content".getBytes());
  }

  @Test
  public void testAddNewRoom_Success() {
    // Arrange
    when(awsS3Service.saveImageToS3(any(MultipartFile.class))).thenReturn("https://example.com/room.jpg");
    when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

    // Act
    Response response = roomService.addNewRoom(
            mockPhoto,
            "DELUXE",
            new BigDecimal("199.99"),
            "Luxury room with sea view");

    // Assert
    assertEquals(200, response.getStatusCode());
    assertEquals("successful", response.getMessage());
    assertNotNull(response.getRoom());
    assertEquals("DELUXE", response.getRoom().getRoomType());

    verify(awsS3Service).saveImageToS3(mockPhoto);

    // Capture and verify the room saved
    ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
    verify(roomRepository).save(roomCaptor.capture());
    Room savedRoom = roomCaptor.getValue();
    assertEquals("DELUXE", savedRoom.getRoomType());
    assertEquals(new BigDecimal("199.99"), savedRoom.getRoomPrice());
    assertEquals("https://example.com/room.jpg", savedRoom.getRoomPhotoUrl());
  }

  @Test
  public void testAddNewRoom_ExceptionInS3Upload() {
    // Arrange
    when(awsS3Service.saveImageToS3(any(MultipartFile.class)))
            .thenThrow(new OurException("Unable to upload image to s3 bucket"));

    // Act
    Response response = roomService.addNewRoom(
            mockPhoto,
            "DELUXE",
            new BigDecimal("199.99"),
            "Luxury room with sea view");

    // Assert
    assertEquals(500, response.getStatusCode());
    assertTrue(response.getMessage().contains("Error saving a room"));

    verify(awsS3Service).saveImageToS3(mockPhoto);
    verify(roomRepository, never()).save(any(Room.class));
  }

  @Test
  public void testAddNewRoom_GeneralException() {
    // Arrange
    when(awsS3Service.saveImageToS3(any(MultipartFile.class))).thenReturn("https://example.com/room.jpg");
    when(roomRepository.save(any(Room.class))).thenThrow(new RuntimeException("Database error"));

    // Act
    Response response = roomService.addNewRoom(
            mockPhoto,
            "DELUXE",
            new BigDecimal("199.99"),
            "Luxury room with sea view");

    // Assert
    assertEquals(500, response.getStatusCode());
    assertTrue(response.getMessage().contains("Error saving a room"));

    verify(awsS3Service).saveImageToS3(mockPhoto);
    verify(roomRepository).save(any(Room.class));
  }

  @Test
  public void testGetAllRoomTypes_Success() {
    // Arrange
    List<String> roomTypes = List.of("STANDARD", "DELUXE", "SUITE");
    when(roomRepository.findDistinctRoomTypes()).thenReturn(roomTypes);

    // Act
    List<String> result = roomService.getAllRoomTypes();

    // Assert
    assertEquals(3, result.size());
    assertTrue(result.contains("DELUXE"));
    assertTrue(result.contains("STANDARD"));
    assertTrue(result.contains("SUITE"));

    verify(roomRepository).findDistinctRoomTypes();
  }

  @Test
  public void testGetAllRoomTypes_EmptyList() {
    // Arrange
    when(roomRepository.findDistinctRoomTypes()).thenReturn(Collections.emptyList());

    // Act
    List<String> result = roomService.getAllRoomTypes();

    // Assert
    assertTrue(result.isEmpty());
    verify(roomRepository).findDistinctRoomTypes();
  }

  @Test
  public void testGetAllRoomTypes_Exception() {
    // Arrange
    when(roomRepository.findDistinctRoomTypes()).thenThrow(new RuntimeException("Database error"));

    // Act & Assert
    assertThrows(RuntimeException.class, () -> roomService.getAllRoomTypes());
    verify(roomRepository).findDistinctRoomTypes();
  }

  @Test
  public void testGetAllRooms_Success() {
    // Arrange
    List<Room> roomList = new ArrayList<>();
    roomList.add(testRoom);
    when(roomRepository.findAll(any(Sort.class))).thenReturn(roomList);

    // Act
    Response response = roomService.getAllRooms();

    // Assert
    assertEquals(200, response.getStatusCode());
    assertEquals("successful", response.getMessage());
    assertNotNull(response.getRoomList());
    assertEquals(1, response.getRoomList().size());
    assertEquals("DELUXE", response.getRoomList().get(0).getRoomType());

    verify(roomRepository).findAll(any(Sort.class));
  }

  @Test
  public void testGetAllRooms_EmptyList() {
    // Arrange
    when(roomRepository.findAll(any(Sort.class))).thenReturn(Collections.emptyList());

    // Act
    Response response = roomService.getAllRooms();

    // Assert
    assertEquals(200, response.getStatusCode());
    assertEquals("successful", response.getMessage());
    assertNotNull(response.getRoomList());
    assertTrue(response.getRoomList().isEmpty());

    verify(roomRepository).findAll(any(Sort.class));
  }

  @Test
  public void testGetAllRooms_Exception() {
    // Arrange
    when(roomRepository.findAll(any(Sort.class))).thenThrow(new RuntimeException("Database error"));

    // Act
    Response response = roomService.getAllRooms();

    // Assert
    assertEquals(500, response.getStatusCode());
    assertTrue(response.getMessage().contains("Error saving a room"));

    verify(roomRepository).findAll(any(Sort.class));
  }

  @Test
  public void testDeleteRoom_Success() {
    // Arrange
    Long roomId = 1L;
    when(roomRepository.findById(anyLong())).thenReturn(Optional.of(testRoom));
    doNothing().when(roomRepository).delete(any(Room.class));

    // Act
    Response response = roomService.deleteRoom(roomId);

    // Assert
    assertEquals(200, response.getStatusCode());
    assertEquals("successful", response.getMessage());

    verify(roomRepository).findById(roomId);
    verify(roomRepository).delete(testRoom);
  }

  @Test
  public void testDeleteRoom_RoomNotFound() {
    // Arrange
    Long roomId = 999L;
    when(roomRepository.findById(anyLong())).thenReturn(Optional.empty());

    // Act
    Response response = roomService.deleteRoom(roomId);

    // Assert
    assertEquals(404, response.getStatusCode());
    assertEquals("Room Not Found", response.getMessage());

    verify(roomRepository).findById(roomId);
    verify(roomRepository, never()).delete(any(Room.class));
  }

  @Test
  public void testDeleteRoom_Exception() {
    // Arrange
    Long roomId = 1L;
    when(roomRepository.findById(anyLong())).thenReturn(Optional.of(testRoom));
    doThrow(new RuntimeException("Database error")).when(roomRepository).delete(any(Room.class));

    // Act
    Response response = roomService.deleteRoom(roomId);

    // Assert
    assertEquals(500, response.getStatusCode());
    assertTrue(response.getMessage().contains("Error saving a room"));

    verify(roomRepository).findById(roomId);
    verify(roomRepository).delete(testRoom);
  }

  @Test
  public void testUpdateRoom_Success() {
    // Arrange
    Long roomId = 1L;
    String newDescription = "Updated luxury room";
    String newRoomType = "SUITE";
    BigDecimal newPrice = new BigDecimal("299.99");

    when(roomRepository.findById(anyLong())).thenReturn(Optional.of(testRoom));
    when(awsS3Service.saveImageToS3(any(MultipartFile.class))).thenReturn("https://example.com/updated-room.jpg");
    when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

    // Act
    Response response = roomService.updateRoom(roomId, newDescription, newRoomType, newPrice, mockPhoto);

    // Assert
    assertEquals(200, response.getStatusCode());
    assertEquals("successful", response.getMessage());
    assertNotNull(response.getRoom());

    verify(roomRepository).findById(roomId);
    verify(awsS3Service).saveImageToS3(mockPhoto);

    // Capture and verify the room saved
    ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
    verify(roomRepository).save(roomCaptor.capture());
    Room savedRoom = roomCaptor.getValue();
    assertEquals("SUITE", savedRoom.getRoomType());
    assertEquals(new BigDecimal("299.99"), savedRoom.getRoomPrice());
    assertEquals(newDescription, savedRoom.getRoomDescription());
    assertEquals("https://example.com/updated-room.jpg", savedRoom.getRoomPhotoUrl());
  }

  @Test
  public void testUpdateRoom_PartialUpdate_NoPhoto() {
    // Arrange
    Long roomId = 1L;
    String newDescription = "Updated luxury room";
    String originalRoomType = testRoom.getRoomType();
    BigDecimal originalPrice = testRoom.getRoomPrice();

    when(roomRepository.findById(anyLong())).thenReturn(Optional.of(testRoom));
    when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

    // Act - only updating description, passing null for other params
    Response response = roomService.updateRoom(roomId, newDescription, null, null, null);

    // Assert
    assertEquals(200, response.getStatusCode());
    assertEquals("successful", response.getMessage());
    assertNotNull(response.getRoom());

    verify(roomRepository).findById(roomId);
    verify(awsS3Service, never()).saveImageToS3(any()); // Photo should not be updated

    // Capture and verify the room saved - only description should change
    ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
    verify(roomRepository).save(roomCaptor.capture());
    Room savedRoom = roomCaptor.getValue();
    assertEquals(originalRoomType, savedRoom.getRoomType()); // Should remain unchanged
    assertEquals(originalPrice, savedRoom.getRoomPrice()); // Should remain unchanged
    assertEquals(newDescription, savedRoom.getRoomDescription()); // Should be updated
  }

  @Test
  public void testUpdateRoom_RoomNotFound() {
    // Arrange
    Long roomId = 999L;
    when(roomRepository.findById(anyLong())).thenReturn(Optional.empty());

    // Act
    Response response = roomService.updateRoom(roomId, "Updated description", "SUITE",
            new BigDecimal("299.99"), mockPhoto);

    // Assert
    assertEquals(404, response.getStatusCode());
    assertEquals("Room Not Found", response.getMessage());

    verify(roomRepository).findById(roomId);
    verify(roomRepository, never()).save(any(Room.class));
  }

  @Test
  public void testUpdateRoom_S3Exception() {
    // Arrange
    Long roomId = 1L;
    // Removed findById stubbing as it's not reached in the execution flow
    when(awsS3Service.saveImageToS3(any(MultipartFile.class)))
            .thenThrow(new OurException("Unable to upload image to s3 bucket"));

    // Act
    Response response = roomService.updateRoom(roomId, "Updated description", "SUITE",
            new BigDecimal("299.99"), mockPhoto);

    // Assert
    assertEquals(404, response.getStatusCode());
    assertTrue(response.getMessage().contains("Unable to upload image")
            || response.getMessage().contains("Room Not Found")
            || response.getMessage().contains("Error saving a room"));

    verify(awsS3Service).saveImageToS3(mockPhoto);
    verify(roomRepository, never()).save(any(Room.class));
  }

  @Test
  public void testUpdateRoom_DatabaseException() {
    // Arrange
    Long roomId = 1L;
    when(roomRepository.findById(anyLong())).thenReturn(Optional.of(testRoom));
    when(awsS3Service.saveImageToS3(any(MultipartFile.class))).thenReturn("https://example.com/updated-room.jpg");
    when(roomRepository.save(any(Room.class))).thenThrow(new RuntimeException("Database error"));

    // Act
    Response response = roomService.updateRoom(roomId, "Updated description", "SUITE",
            new BigDecimal("299.99"), mockPhoto);

    // Assert
    assertEquals(500, response.getStatusCode());
    assertTrue(response.getMessage().contains("Error saving a room"));

    verify(roomRepository).findById(roomId);
    verify(awsS3Service).saveImageToS3(mockPhoto);
    verify(roomRepository).save(any(Room.class));
  }

  @Test
  public void testGetRoomById_Success() {
    // Arrange
    Long roomId = 1L;
    when(roomRepository.findById(anyLong())).thenReturn(Optional.of(testRoom));

    // Act
    Response response = roomService.getRoomById(roomId);

    // Assert
    assertEquals(200, response.getStatusCode());
    assertEquals("successful", response.getMessage());
    assertNotNull(response.getRoom());
    assertEquals(testRoom.getRoomType(), response.getRoom().getRoomType());

    verify(roomRepository).findById(roomId);
  }

  @Test
  public void testGetRoomById_RoomNotFound() {
    // Arrange
    Long roomId = 999L;
    when(roomRepository.findById(anyLong())).thenReturn(Optional.empty());

    // Act
    Response response = roomService.getRoomById(roomId);

    // Assert
    assertEquals(404, response.getStatusCode());
    assertEquals("Room Not Found", response.getMessage());

    verify(roomRepository).findById(roomId);
  }

  @Test
  public void testGetRoomById_Exception() {
    // Arrange
    Long roomId = 1L;
    when(roomRepository.findById(anyLong())).thenThrow(new RuntimeException("Database error"));

    // Act
    Response response = roomService.getRoomById(roomId);

    // Assert
    assertEquals(500, response.getStatusCode());
    assertTrue(response.getMessage().contains("Error saving a room"));

    verify(roomRepository).findById(roomId);
  }

  @Test
  public void testGetAvailableRoomsByDataAndType_Success() {
    // Arrange
    LocalDate checkInDate = LocalDate.now().plusDays(1);
    LocalDate checkOutDate = LocalDate.now().plusDays(3);
    String roomType = "DELUXE";

    List<Room> availableRooms = new ArrayList<>();
    availableRooms.add(testRoom);

    when(roomRepository.findAvailableRoomsByDatesAndTypes(any(LocalDate.class), any(LocalDate.class), anyString()))
            .thenReturn(availableRooms);

    // Act
    Response response = roomService.getAvailableRoomsByDataAndType(checkInDate, checkOutDate, roomType);

    // Assert
    assertEquals(200, response.getStatusCode());
    assertEquals("successful", response.getMessage());
    assertNotNull(response.getRoomList());
    assertEquals(1, response.getRoomList().size());
    assertEquals("DELUXE", response.getRoomList().get(0).getRoomType());

    verify(roomRepository).findAvailableRoomsByDatesAndTypes(checkInDate, checkOutDate, roomType);
  }

  @Test
  public void testGetAvailableRoomsByDataAndType_NoRoomsAvailable() {
    // Arrange
    LocalDate checkInDate = LocalDate.now().plusDays(1);
    LocalDate checkOutDate = LocalDate.now().plusDays(3);
    String roomType = "DELUXE";

    when(roomRepository.findAvailableRoomsByDatesAndTypes(any(LocalDate.class), any(LocalDate.class), anyString()))
            .thenReturn(Collections.emptyList());

    // Act
    Response response = roomService.getAvailableRoomsByDataAndType(checkInDate, checkOutDate, roomType);

    // Assert
    assertEquals(200, response.getStatusCode());
    assertEquals("successful", response.getMessage());
    assertNotNull(response.getRoomList());
    assertTrue(response.getRoomList().isEmpty());

    verify(roomRepository).findAvailableRoomsByDatesAndTypes(checkInDate, checkOutDate, roomType);
  }

  @Test
  public void testGetAvailableRoomsByDataAndType_Exception() {
    // Arrange
    LocalDate checkInDate = LocalDate.now().plusDays(1);
    LocalDate checkOutDate = LocalDate.now().plusDays(3);
    String roomType = "DELUXE";

    when(roomRepository.findAvailableRoomsByDatesAndTypes(any(LocalDate.class), any(LocalDate.class), anyString()))
            .thenThrow(new RuntimeException("Database error"));

    // Act
    Response response = roomService.getAvailableRoomsByDataAndType(checkInDate, checkOutDate, roomType);

    // Assert
    assertEquals(500, response.getStatusCode());
    assertTrue(response.getMessage().contains("Error saving a room"));

    verify(roomRepository).findAvailableRoomsByDatesAndTypes(checkInDate, checkOutDate, roomType);
  }

  @Test
  public void testGetAllAvailableRooms_Success() {
    // Arrange
    List<Room> availableRooms = new ArrayList<>();
    availableRooms.add(testRoom);

    when(roomRepository.getAllAvailableRooms()).thenReturn(availableRooms);

    // Act
    Response response = roomService.getAllAvailableRooms();

    // Assert
    assertEquals(200, response.getStatusCode());
    assertEquals("successful", response.getMessage());
    assertNotNull(response.getRoomList());
    assertEquals(1, response.getRoomList().size());
    assertEquals("DELUXE", response.getRoomList().get(0).getRoomType());

    verify(roomRepository).getAllAvailableRooms();
  }

  @Test
  public void testGetAllAvailableRooms_NoRoomsAvailable() {
    // Arrange
    when(roomRepository.getAllAvailableRooms()).thenReturn(Collections.emptyList());

    // Act
    Response response = roomService.getAllAvailableRooms();

    // Assert
    assertEquals(200, response.getStatusCode());
    assertEquals("successful", response.getMessage());
    assertNotNull(response.getRoomList());
    assertTrue(response.getRoomList().isEmpty());

    verify(roomRepository).getAllAvailableRooms();
  }

  @Test
  public void testGetAllAvailableRooms_Exception() {
    // Arrange
    when(roomRepository.getAllAvailableRooms()).thenThrow(new RuntimeException("Database error"));

    // Act
    Response response = roomService.getAllAvailableRooms();

    // Assert
    assertEquals(500, response.getStatusCode());
    assertTrue(response.getMessage().contains("Error saving a room"));

    verify(roomRepository).getAllAvailableRooms();
  }
}