package com.phegondev.PhegonHotel.service.impl;

import com.phegondev.PhegonHotel.dto.Response;
import com.phegondev.PhegonHotel.entity.Booking;
import com.phegondev.PhegonHotel.entity.Room;
import com.phegondev.PhegonHotel.entity.User;
import com.phegondev.PhegonHotel.repo.BookingRepository;
import com.phegondev.PhegonHotel.repo.RoomRepository;
import com.phegondev.PhegonHotel.repo.UserRepository;
import com.phegondev.PhegonHotel.service.interfac.IRoomService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

  @Mock
  private BookingRepository bookingRepository;

  @Mock
  private RoomRepository roomRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private IRoomService roomService;

  @Mock
  private Counter createBookingCounter;

  @Mock
  private Counter createBookingErrorCounter;

  @Mock
  private Timer createBookingTimer;

  @Mock
  private Timer.Sample timerSample;

  @InjectMocks
  private BookingService bookingService;

  private Room testRoom;
  private User testUser;
  private Booking testBooking;

  @BeforeEach
  public void setup() {
    testRoom = new Room();
    testRoom.setId(1L);
    testRoom.setRoomType("STANDARD");
    testRoom.setRoomPrice(new BigDecimal("99.99"));
    testRoom.setBookings(new ArrayList<>());

    testUser = new User();
    testUser.setId(1L);
    testUser.setEmail("test@example.com");
    testUser.setName("Test User");

    testBooking = new Booking();
    testBooking.setId(1L);
    testBooking.setCheckInDate(LocalDate.now().plusDays(1));
    testBooking.setCheckOutDate(LocalDate.now().plusDays(3));
    testBooking.setNumOfAdults(2);
    testBooking.setNumOfChildren(0);
    testBooking.setTotalNumOfGuest(2);
    testBooking.setBookingConfirmationCode("ABCD1234");
    testBooking.setRoom(testRoom);
    testBooking.setUser(testUser);
  }

  @Test
  public void testSaveBooking_Success() {
    // Arrange
    when(roomRepository.findById(anyLong())).thenReturn(Optional.of(testRoom));
    when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
    when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

    // Mock Timer.Sample for metrics
    try (MockedStatic<Timer> timerMock = mockStatic(Timer.class)) {
      timerMock.when(Timer::start).thenReturn(timerSample);

      // Act
      Response response = bookingService.saveBooking(1L, 1L, testBooking);

      // Assert
      assertEquals(200, response.getStatusCode());
      assertEquals("successful", response.getMessage());
      assertNotNull(response.getBookingConfirmationCode());

      verify(roomRepository).findById(1L);
      verify(userRepository).findById(1L);
      verify(bookingRepository).save(any(Booking.class));
      verify(createBookingCounter).increment();
      verify(createBookingErrorCounter, never()).increment();
      verify(timerSample).stop(createBookingTimer);
    }
  }

  @Test
  public void testSaveBooking_RoomNotFound() {
    // Arrange
    when(roomRepository.findById(anyLong())).thenReturn(Optional.empty());

    // Mock Timer.Sample for metrics
    try (MockedStatic<Timer> timerMock = mockStatic(Timer.class)) {
      timerMock.when(Timer::start).thenReturn(timerSample);

      // Act
      Response response = bookingService.saveBooking(1L, 1L, testBooking);

      // Assert
      assertEquals(404, response.getStatusCode());
      assertEquals("Room Not Found", response.getMessage());

      verify(roomRepository).findById(1L);
      verify(userRepository, never()).findById(anyLong());
      verify(bookingRepository, never()).save(any(Booking.class));
      verify(createBookingCounter, never()).increment();
      verify(createBookingErrorCounter).increment();
      verify(timerSample).stop(createBookingTimer);
    }
  }

//  @Test
//  public void testSaveBooking_CheckOutBeforeCheckIn() {
//    // Arrange
//    testBooking.setCheckInDate(LocalDate.now().plusDays(3));
//    testBooking.setCheckOutDate(LocalDate.now().plusDays(1));
//
//    // Mock Timer.Sample for metrics
//    try (MockedStatic<Timer> timerMock = mockStatic(Timer.class)) {
//      timerMock.when(Timer::start).thenReturn(timerSample);
//
//      // Act
//      Response response = bookingService.saveBooking(1L, 1L, testBooking);
//
//      // Assert
//      assertEquals(500, response.getStatusCode());
//      assertTrue(response.getMessage().contains("Error Saving a booking"));
//
//      verify(createBookingErrorCounter).increment();
//      verify(timerSample).stop(createBookingTimer);
//    }
//  }

  @Test
  public void testFindBookingByConfirmationCode_Success() {
    // Arrange
    String confirmationCode = "ABCD1234";
    when(bookingRepository.findByBookingConfirmationCode(anyString())).thenReturn(Optional.of(testBooking));

    // Act
    Response response = bookingService.findBookingByConfirmationCode(confirmationCode);

    // Assert
    assertEquals(200, response.getStatusCode());
    assertEquals("successful", response.getMessage());
    assertNotNull(response.getBooking());

    verify(bookingRepository).findByBookingConfirmationCode(confirmationCode);
  }

  @Test
  public void testFindBookingByConfirmationCode_NotFound() {
    // Arrange
    String confirmationCode = "NONEXISTENT";
    when(bookingRepository.findByBookingConfirmationCode(anyString())).thenReturn(Optional.empty());

    // Act
    Response response = bookingService.findBookingByConfirmationCode(confirmationCode);

    // Assert
    assertEquals(404, response.getStatusCode());
    assertEquals("Booking Not Found", response.getMessage());

    verify(bookingRepository).findByBookingConfirmationCode(confirmationCode);
  }

  @Test
  public void testGetAllBookings_Success() {
    // Arrange
    List<Booking> bookings = new ArrayList<>();
    bookings.add(testBooking);
    when(bookingRepository.findAll(any(Sort.class))).thenReturn(bookings);

    // Act
    Response response = bookingService.getAllBookings();

    // Assert
    assertEquals(200, response.getStatusCode());
    assertEquals("successful", response.getMessage());
    assertNotNull(response.getBookingList());
    assertEquals(1, response.getBookingList().size());

    verify(bookingRepository).findAll(any(Sort.class));
  }

  @Test
  public void testCancelBooking_Success() {
    // Arrange
    Long bookingId = 1L;
    when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(testBooking));
    doNothing().when(bookingRepository).delete(any(Booking.class));

    // Act
    Response response = bookingService.cancelBooking(bookingId);

    // Assert
    assertEquals(200, response.getStatusCode());
    assertEquals("successful", response.getMessage());

    verify(bookingRepository).findById(bookingId);
    verify(bookingRepository).delete(any(Booking.class));
  }

  @Test
  public void testCancelBooking_NotFound() {
    // Arrange
    Long bookingId = 999L;
    when(bookingRepository.findById(anyLong())).thenReturn(Optional.empty());

    // Act
    Response response = bookingService.cancelBooking(bookingId);

    // Assert
    assertEquals(404, response.getStatusCode());
    assertEquals("Booking Does Not Exist", response.getMessage());

    verify(bookingRepository).findById(bookingId);
    verify(bookingRepository, never()).deleteById(anyLong());
  }

  @Test
  public void testRoomIsAvailable_True() {
    // Create a new booking with no conflicts
    Booking newBooking = new Booking();
    newBooking.setCheckInDate(LocalDate.now().plusDays(10));
    newBooking.setCheckOutDate(LocalDate.now().plusDays(12));

    // Create an existing booking with different dates
    Booking existingBooking = new Booking();
    existingBooking.setCheckInDate(LocalDate.now().plusDays(1));
    existingBooking.setCheckOutDate(LocalDate.now().plusDays(3));

    List<Booking> existingBookings = new ArrayList<>();
    existingBookings.add(existingBooking);

    // Using reflection to test private method
    boolean result = invokeRoomIsAvailable(bookingService, newBooking, existingBookings);

    // Assert
    assertTrue(result);
  }

  @Test
  public void testRoomIsAvailable_False_SameCheckInDate() {
    // Create a new booking with the same check-in date as an existing booking
    Booking newBooking = new Booking();
    newBooking.setCheckInDate(LocalDate.now().plusDays(1));
    newBooking.setCheckOutDate(LocalDate.now().plusDays(5));

    // Create an existing booking
    Booking existingBooking = new Booking();
    existingBooking.setCheckInDate(LocalDate.now().plusDays(1));
    existingBooking.setCheckOutDate(LocalDate.now().plusDays(3));

    List<Booking> existingBookings = new ArrayList<>();
    existingBookings.add(existingBooking);

    // Using reflection to test private method
    boolean result = invokeRoomIsAvailable(bookingService, newBooking, existingBookings);

    // Assert
    assertFalse(result);
  }

  // Helper method to invoke private roomIsAvailable method using reflection
  private boolean invokeRoomIsAvailable(BookingService service, Booking booking, List<Booking> existingBookings) {
    try {
      var method = BookingService.class.getDeclaredMethod("roomIsAvailable", Booking.class, List.class);
      method.setAccessible(true);
      return (boolean) method.invoke(service, booking, existingBookings);
    } catch (Exception e) {
      fail("Failed to invoke private method: " + e.getMessage());
      return false;
    }
  }
}