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
import org.springframework.data.domain.Sort;
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
public class BookingRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private BookingRepository bookingRepository;

  private User testUser;
  private Room testRoom;
  private Booking testBooking;

  @BeforeEach
  public void setup() {
    // Create user
    testUser = new User();
    testUser.setEmail("test@example.com");
    testUser.setName("Test User");
    testUser.setPhoneNumber("1234567890");
    testUser.setPassword("password");
    testUser.setRole("USER");
    entityManager.persist(testUser);

    // Create room
    testRoom = new Room();
    testRoom.setRoomType("DELUXE");
    testRoom.setRoomPrice(new BigDecimal("199.99"));
    testRoom.setRoomPhotoUrl("https://example.com/deluxe.jpg");
    testRoom.setRoomDescription("Deluxe room with city view");
    entityManager.persist(testRoom);

    // Create booking
    testBooking = new Booking();
    testBooking.setCheckInDate(LocalDate.now().plusDays(1));
    testBooking.setCheckOutDate(LocalDate.now().plusDays(3));
    testBooking.setNumOfAdults(2);
    testBooking.setNumOfChildren(0);
    testBooking.setTotalNumOfGuest(2);
    testBooking.setBookingConfirmationCode("ABCD1234");
    testBooking.setRoom(testRoom);
    testBooking.setUser(testUser);
    entityManager.persist(testBooking);

    entityManager.flush();
  }

  @Test
  public void testFindById_Success() {
    // Act
    Optional<Booking> foundBooking = bookingRepository.findById(testBooking.getId());

    // Assert
    assertTrue(foundBooking.isPresent());
    assertEquals(LocalDate.now().plusDays(1), foundBooking.get().getCheckInDate());
    assertEquals(LocalDate.now().plusDays(3), foundBooking.get().getCheckOutDate());
    assertEquals("ABCD1234", foundBooking.get().getBookingConfirmationCode());
  }

  @Test
  public void testFindById_NotFound() {
    // Act
    Optional<Booking> foundBooking = bookingRepository.findById(999L);

    // Assert
    assertFalse(foundBooking.isPresent());
  }

  @Test
  public void testFindByBookingConfirmationCode_Success() {
    // Act
    Optional<Booking> foundBooking = bookingRepository.findByBookingConfirmationCode("ABCD1234");

    // Assert
    assertTrue(foundBooking.isPresent());
    assertEquals(testBooking.getId(), foundBooking.get().getId());
    assertEquals(LocalDate.now().plusDays(1), foundBooking.get().getCheckInDate());
    assertEquals(LocalDate.now().plusDays(3), foundBooking.get().getCheckOutDate());
  }

  @Test
  public void testFindByBookingConfirmationCode_NotFound() {
    // Act
    Optional<Booking> foundBooking = bookingRepository.findByBookingConfirmationCode("NONEXISTENT");

    // Assert
    assertFalse(foundBooking.isPresent());
  }

  @Test
  public void testSave_Success() {
    // Arrange
    Booking newBooking = new Booking();
    newBooking.setCheckInDate(LocalDate.now().plusDays(10));
    newBooking.setCheckOutDate(LocalDate.now().plusDays(15));
    newBooking.setNumOfAdults(1);
    newBooking.setNumOfChildren(1);
    newBooking.setTotalNumOfGuest(2);
    newBooking.setBookingConfirmationCode("XYZ9876");
    newBooking.setRoom(testRoom);
    newBooking.setUser(testUser);

    // Act
    Booking savedBooking = bookingRepository.save(newBooking);

    // Assert
    assertNotNull(savedBooking.getId());
    assertEquals("XYZ9876", savedBooking.getBookingConfirmationCode());
    assertEquals(LocalDate.now().plusDays(10), savedBooking.getCheckInDate());

    // Verify it can be found in the repository
    assertTrue(bookingRepository.findById(savedBooking.getId()).isPresent());
  }

  @Test
  public void testDeleteById_Success() {
    // Act
    bookingRepository.deleteById(testBooking.getId());
    entityManager.flush();

    // Assert
    Optional<Booking> deletedBooking = bookingRepository.findById(testBooking.getId());
    assertFalse(deletedBooking.isPresent());
  }

  @Test
  public void testFindAll_Success() {
    // Arrange
    Booking secondBooking = new Booking();
    secondBooking.setCheckInDate(LocalDate.now().plusDays(5));
    secondBooking.setCheckOutDate(LocalDate.now().plusDays(7));
    secondBooking.setNumOfAdults(2);
    secondBooking.setNumOfChildren(1);
    secondBooking.setTotalNumOfGuest(3);
    secondBooking.setBookingConfirmationCode("EFG5678");
    secondBooking.setRoom(testRoom);
    secondBooking.setUser(testUser);
    entityManager.persist(secondBooking);
    entityManager.flush();

    // Act
    List<Booking> bookings = bookingRepository.findAll();

    // Assert
    assertEquals(2, bookings.size());
    assertTrue(bookings.stream().anyMatch(b -> b.getBookingConfirmationCode().equals("ABCD1234")));
    assertTrue(bookings.stream().anyMatch(b -> b.getBookingConfirmationCode().equals("EFG5678")));
  }

  @Test
  public void testFindAll_WithSort() {
    // Arrange
    Booking secondBooking = new Booking();
    secondBooking.setCheckInDate(LocalDate.now().plusDays(5));
    secondBooking.setCheckOutDate(LocalDate.now().plusDays(7));
    secondBooking.setNumOfAdults(2);
    secondBooking.setNumOfChildren(1);
    secondBooking.setTotalNumOfGuest(3);
    secondBooking.setBookingConfirmationCode("EFG5678");
    secondBooking.setRoom(testRoom);
    secondBooking.setUser(testUser);
    entityManager.persist(secondBooking);
    entityManager.flush();

    // Act - Get bookings sorted by ID in descending order
    List<Booking> bookings = bookingRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

    // Assert
    assertEquals(2, bookings.size());
    // The most recently added booking should be first
    assertEquals("EFG5678", bookings.get(0).getBookingConfirmationCode());
    assertEquals("ABCD1234", bookings.get(1).getBookingConfirmationCode());
  }
}