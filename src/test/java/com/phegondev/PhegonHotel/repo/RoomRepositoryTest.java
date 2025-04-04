package com.phegondev.PhegonHotel.repo;

import com.phegondev.PhegonHotel.entity.Booking;
import com.phegondev.PhegonHotel.entity.Room;
import com.phegondev.PhegonHotel.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "SPRING_APPLICATION_NAME=test-app",
        "aws.s3.bucket.name=test-bucket",
        "aws.s3.access.key=test-key",
        "aws.s3.secret.key=test-secret",
        "aws.region.static=us-east-1"
})
@Import(RepositoryTestConfig.class)
public class RoomRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private RoomRepository roomRepository;

  @Autowired
  private BookingRepository bookingRepository;

  @Autowired
  private UserRepository userRepository;

  private Room standardRoom;
  private Room deluxeRoom;
  private Room suiteRoom;
  private User testUser;

  @BeforeEach
  public void setup() {
    // Create rooms
    standardRoom = new Room();
    standardRoom.setRoomType("STANDARD");
    standardRoom.setRoomPrice(new BigDecimal("99.99"));
    standardRoom.setRoomPhotoUrl("https://example.com/standard.jpg");
    standardRoom.setRoomDescription("Standard room");

    deluxeRoom = new Room();
    deluxeRoom.setRoomType("DELUXE");
    deluxeRoom.setRoomPrice(new BigDecimal("199.99"));
    deluxeRoom.setRoomPhotoUrl("https://example.com/deluxe.jpg");
    deluxeRoom.setRoomDescription("Deluxe room with city view");

    suiteRoom = new Room();
    suiteRoom.setRoomType("SUITE");
    suiteRoom.setRoomPrice(new BigDecimal("299.99"));
    suiteRoom.setRoomPhotoUrl("https://example.com/suite.jpg");
    suiteRoom.setRoomDescription("Suite with sea view");

    // Persist rooms
    entityManager.persist(standardRoom);
    entityManager.persist(deluxeRoom);
    entityManager.persist(suiteRoom);

    // Create a user for bookings
    testUser = new User();
    testUser.setEmail("test@example.com");
    testUser.setName("Test User");
    testUser.setPhoneNumber("1234567890");
    testUser.setPassword("password");
    testUser.setRole("USER");
    entityManager.persist(testUser);

    // Flush to ensure all entities are saved
    entityManager.flush();
  }

  @Test
  public void testFindById_Success() {
    // Act
    Optional<Room> foundRoom = roomRepository.findById(standardRoom.getId());

    // Assert
    assertTrue(foundRoom.isPresent());
    assertEquals("STANDARD", foundRoom.get().getRoomType());
    assertEquals(new BigDecimal("99.99"), foundRoom.get().getRoomPrice());
  }

  @Test
  public void testFindById_NotFound() {
    // Act
    Optional<Room> foundRoom = roomRepository.findById(999L);

    // Assert
    assertFalse(foundRoom.isPresent());
  }

  @Test
  public void testFindAll_Success() {
    // Act
    List<Room> rooms = roomRepository.findAll();

    // Assert
    assertEquals(3, rooms.size());
    assertTrue(rooms.stream().anyMatch(r -> r.getRoomType().equals("STANDARD")));
    assertTrue(rooms.stream().anyMatch(r -> r.getRoomType().equals("DELUXE")));
    assertTrue(rooms.stream().anyMatch(r -> r.getRoomType().equals("SUITE")));
  }

  @Test
  public void testFindDistinctRoomTypes_Success() {
    // Act
    List<String> roomTypes = roomRepository.findDistinctRoomTypes();

    // Assert
    assertEquals(3, roomTypes.size());
    assertTrue(roomTypes.contains("STANDARD"));
    assertTrue(roomTypes.contains("DELUXE"));
    assertTrue(roomTypes.contains("SUITE"));
  }

  @Test
  public void testFindAvailableRoomsByDatesAndTypes_NoBookings() {
    // Arrange
    LocalDate checkInDate = LocalDate.now().plusDays(1);
    LocalDate checkOutDate = LocalDate.now().plusDays(3);
    String roomType = "DELUXE";

    // Act
    List<Room> availableRooms = roomRepository.findAvailableRoomsByDatesAndTypes(
            checkInDate, checkOutDate, roomType);

    // Assert
    assertEquals(1, availableRooms.size());
    assertEquals("DELUXE", availableRooms.get(0).getRoomType());
  }

  @Test
  public void testFindAvailableRoomsByDatesAndTypes_WithConflictingBooking() {
    // Arrange
    LocalDate checkInDate = LocalDate.now().plusDays(1);
    LocalDate checkOutDate = LocalDate.now().plusDays(3);
    String roomType = "DELUXE";

    // Create a booking that conflicts with the search dates
    Booking booking = new Booking();
    booking.setCheckInDate(LocalDate.now().plusDays(2));
    booking.setCheckOutDate(LocalDate.now().plusDays(4));
    booking.setNumOfAdults(2);
    booking.setNumOfChildren(0);
    booking.setBookingConfirmationCode("ABCD1234");
    booking.setRoom(deluxeRoom);
    booking.setUser(testUser);
    entityManager.persist(booking);
    entityManager.flush();

    // Act
    List<Room> availableRooms = roomRepository.findAvailableRoomsByDatesAndTypes(
            checkInDate, checkOutDate, roomType);

    // Assert
    assertEquals(0, availableRooms.size());
  }

  @Test
  public void testFindAvailableRoomsByDatesAndTypes_WithNonConflictingBooking() {
    // Arrange
    LocalDate checkInDate = LocalDate.now().plusDays(10);
    LocalDate checkOutDate = LocalDate.now().plusDays(12);
    String roomType = "DELUXE";

    // Create a booking that doesn't conflict with the search dates
    Booking booking = new Booking();
    booking.setCheckInDate(LocalDate.now().plusDays(1));
    booking.setCheckOutDate(LocalDate.now().plusDays(3));
    booking.setNumOfAdults(2);
    booking.setNumOfChildren(0);
    booking.setBookingConfirmationCode("ABCD1234");
    booking.setRoom(deluxeRoom);
    booking.setUser(testUser);
    entityManager.persist(booking);
    entityManager.flush();

    // Act
    List<Room> availableRooms = roomRepository.findAvailableRoomsByDatesAndTypes(
            checkInDate, checkOutDate, roomType);

    // Assert
    assertEquals(1, availableRooms.size());
    assertEquals("DELUXE", availableRooms.get(0).getRoomType());
  }

  @Test
  public void testGetAllAvailableRooms_Success() {
    // Arrange
    // Create a booking for the deluxe room
    Booking booking = new Booking();
    booking.setCheckInDate(LocalDate.now().plusDays(1));
    booking.setCheckOutDate(LocalDate.now().plusDays(3));
    booking.setNumOfAdults(2);
    booking.setNumOfChildren(0);
    booking.setBookingConfirmationCode("ABCD1234");
    booking.setRoom(deluxeRoom);
    booking.setUser(testUser);
    entityManager.persist(booking);
    entityManager.flush();

    // Act
    List<Room> availableRooms = roomRepository.getAllAvailableRooms();

    // Assert
    assertEquals(2, availableRooms.size());
    assertTrue(availableRooms.stream().anyMatch(r -> r.getRoomType().equals("STANDARD")));
    assertTrue(availableRooms.stream().anyMatch(r -> r.getRoomType().equals("SUITE")));
    assertFalse(availableRooms.stream().anyMatch(r -> r.getRoomType().equals("DELUXE")));
  }

  @Test
  public void testSave_Success() {
    // Arrange
    Room newRoom = new Room();
    newRoom.setRoomType("FAMILY");
    newRoom.setRoomPrice(new BigDecimal("249.99"));
    newRoom.setRoomPhotoUrl("https://example.com/family.jpg");
    newRoom.setRoomDescription("Family room with extra beds");

    // Act
    Room savedRoom = roomRepository.save(newRoom);

    // Assert
    assertNotNull(savedRoom.getId());
    assertEquals("FAMILY", savedRoom.getRoomType());
    assertEquals(new BigDecimal("249.99"), savedRoom.getRoomPrice());

    // Verify it can be found in the repository
    assertTrue(roomRepository.findById(savedRoom.getId()).isPresent());
  }

  @Test
  public void testDeleteById_Success() {
    // Act
    roomRepository.deleteById(standardRoom.getId());
    entityManager.flush();

    // Assert
    Optional<Room> deletedRoom = roomRepository.findById(standardRoom.getId());
    assertFalse(deletedRoom.isPresent());
  }
}