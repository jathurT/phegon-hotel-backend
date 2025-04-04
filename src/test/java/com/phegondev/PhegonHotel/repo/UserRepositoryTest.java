package com.phegondev.PhegonHotel.repo;

import com.phegondev.PhegonHotel.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "SPRING_APPLICATION_NAME=test-app",
        "aws.s3.bucket.name=test-bucket",
        "aws.s3.access.key=test-key",
        "aws.s3.secret.key=test-secret",
        "aws.region.static=us-east-1"
})
@Import(RepositoryTestConfig.class)
public class UserRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private UserRepository userRepository;

  @Test
  public void testFindByEmail_Success() {
    // Arrange
    User testUser = new User();
    testUser.setEmail("test@example.com");
    testUser.setName("Test User");
    testUser.setPhoneNumber("1234567890");
    testUser.setPassword("password");
    testUser.setRole("USER");
    entityManager.persist(testUser);
    entityManager.flush();

    // Act
    Optional<User> foundUser = userRepository.findByEmail("test@example.com");

    // Assert
    assertTrue(foundUser.isPresent());
    assertEquals("Test User", foundUser.get().getName());
    assertEquals("test@example.com", foundUser.get().getEmail());
  }

  @Test
  public void testFindByEmail_NotFound() {
    // Act
    Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

    // Assert
    assertFalse(foundUser.isPresent());
  }

  @Test
  public void testExistsByEmail_True() {
    // Arrange
    User testUser = new User();
    testUser.setEmail("exists@example.com");
    testUser.setName("Exists User");
    testUser.setPhoneNumber("1234567890");
    testUser.setPassword("password");
    testUser.setRole("USER");
    entityManager.persist(testUser);
    entityManager.flush();

    // Act
    boolean exists = userRepository.existsByEmail("exists@example.com");

    // Assert
    assertTrue(exists);
  }

  @Test
  public void testExistsByEmail_False() {
    // Act
    boolean exists = userRepository.existsByEmail("nonexistent@example.com");

    // Assert
    assertFalse(exists);
  }

  @Test
  public void testSave_Success() {
    // Arrange
    User newUser = new User();
    newUser.setEmail("new@example.com");
    newUser.setName("New User");
    newUser.setPhoneNumber("9876543210");
    newUser.setPassword("newpassword");
    newUser.setRole("USER");

    // Act
    User savedUser = userRepository.save(newUser);

    // Assert
    assertNotNull(savedUser.getId());
    assertEquals("new@example.com", savedUser.getEmail());
    assertEquals("New User", savedUser.getName());

    // Verify it can be found in the repository
    assertTrue(userRepository.findById(savedUser.getId()).isPresent());
  }

  @Test
  public void testFindById_Success() {
    // Arrange
    User testUser = new User();
    testUser.setEmail("findbyid@example.com");
    testUser.setName("FindById User");
    testUser.setPhoneNumber("1234567890");
    testUser.setPassword("password");
    testUser.setRole("USER");
    User persistedUser = entityManager.persist(testUser);
    entityManager.flush();

    // Act
    Optional<User> foundUser = userRepository.findById(persistedUser.getId());

    // Assert
    assertTrue(foundUser.isPresent());
    assertEquals("FindById User", foundUser.get().getName());
    assertEquals("findbyid@example.com", foundUser.get().getEmail());
  }

  @Test
  public void testFindById_NotFound() {
    // Act
    Optional<User> foundUser = userRepository.findById(999L);

    // Assert
    assertFalse(foundUser.isPresent());
  }

  @Test
  public void testDeleteById_Success() {
    // Arrange
    User testUser = new User();
    testUser.setEmail("delete@example.com");
    testUser.setName("Delete User");
    testUser.setPhoneNumber("1234567890");
    testUser.setPassword("password");
    testUser.setRole("USER");
    User persistedUser = entityManager.persist(testUser);
    entityManager.flush();
    Long userId = persistedUser.getId();

    // Act
    userRepository.deleteById(userId);
    entityManager.flush();

    // Assert
    Optional<User> deletedUser = userRepository.findById(userId);
    assertFalse(deletedUser.isPresent());
  }

  @Test
  public void testFindAll_Success() {
    // Arrange - Clear any existing users and add two new ones
    userRepository.deleteAll();
    entityManager.flush();

    User firstUser = new User();
    firstUser.setEmail("first@example.com");
    firstUser.setName("First User");
    firstUser.setPhoneNumber("1234567890");
    firstUser.setPassword("password1");
    firstUser.setRole("USER");
    entityManager.persist(firstUser);

    User secondUser = new User();
    secondUser.setEmail("second@example.com");
    secondUser.setName("Second User");
    secondUser.setPhoneNumber("5555555555");
    secondUser.setPassword("password2");
    secondUser.setRole("USER");
    entityManager.persist(secondUser);
    entityManager.flush();

    // Act
    List<User> users = userRepository.findAll();

    // Assert
    assertEquals(2, users.size());
    assertTrue(users.stream().anyMatch(u -> u.getEmail().equals("first@example.com")));
    assertTrue(users.stream().anyMatch(u -> u.getEmail().equals("second@example.com")));
  }
}