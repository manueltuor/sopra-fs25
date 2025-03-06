package ch.uzh.ifi.hase.soprafs24.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserService userService;

  @Autowired
  private UserRepository userRepository;

  private static String ADMIN_TOKEN;
  private static User testUser;

  // Setup method to create a user for testing
  @BeforeEach
  public void setup() throws Exception {
      // Clear the database
      userRepository.deleteAll();
      userRepository.flush(); // Ensure deleteAll() is committed
  
      // Create new user
      User user = new User();
      user.setUsername("admin");
      user.setName("admin");
      user.setPassword("admin");
      user.setToken(UUID.randomUUID().toString()); // Set the token
      user.setStatus(UserStatus.ONLINE);
      user.setDate(LocalDate.now());
      user.setBirthday(null);
  
      // Save the user
      User createdUser = userRepository.saveAndFlush(user); // Force immediate commit
      ADMIN_TOKEN = createdUser.getToken(); // Store token for authentication
      testUser = createdUser;
  
      System.out.println("Setup completed!");
      System.out.println("User ID: " + testUser.getId());
      System.out.println("Admin Token: " + ADMIN_TOKEN);
  }

  @Test
  void POST_user_201() throws Exception {
      UserPostDTO userPostDTO = new UserPostDTO();
      userPostDTO.setUsername("testUser");
      userPostDTO.setName("testUser");
      userPostDTO.setPassword("testPassword");

      mockMvc.perform(post("/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(asJsonString(userPostDTO)))
          .andExpect(status().isCreated());
  }

  @Test
  void POST_user_409() throws Exception {
      // Step 1: Create the first user
      UserPostDTO userPostDTO = new UserPostDTO();
      userPostDTO.setUsername("testUser");
      userPostDTO.setName("testUser");
      userPostDTO.setPassword("testPassword");
  
      mockMvc.perform(post("/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(asJsonString(userPostDTO)))
          .andExpect(status().isCreated());

      userRepository.flush();
  
      // Verify that the first user was created
      User firstUser = userRepository.findByUsername("testUser");
      System.out.println("First User: " + firstUser);
  
      // Step 2: Attempt to create a duplicate user
      UserPostDTO copyUserPostDTO = new UserPostDTO();
      copyUserPostDTO.setUsername("testUser");
      copyUserPostDTO.setPassword("testPassword");
  
      mockMvc.perform(post("/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(asJsonString(copyUserPostDTO)))
          .andExpect(status().isConflict());
  }

  @Test
  void GET_user_id_200() throws Exception {
      // Check if the user is in the database
      List<User> users = userRepository.findAll();
      users.forEach(user -> System.out.println("DB User: " + user.getUsername() + ", Token: " + user.getToken()));
  
      System.out.println("Test User: " + testUser);
      
      // Verify the token
      System.out.println("Sent Token: " + ADMIN_TOKEN);   
      System.out.println("User: " + userRepository.findByToken(ADMIN_TOKEN.trim().replace("\"", "")));
    
      // Use the stored test user for GET request
      mockMvc.perform(get("/users/{id}", testUser.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", ADMIN_TOKEN.trim().replace("\"", ""))) // Use the token
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.username", is(testUser.getUsername())))
          .andExpect(jsonPath("$.status", is(testUser.getStatus().toString())));
  }
  @Test
  void PUT_user_id_204() throws Exception {
      // Perform a PUT request to update the user profile
      UserPutDTO updatedDTO = new UserPutDTO();
      updatedDTO.setUsername("updatedUsername");

      mockMvc.perform(put("/users/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .content(asJsonString(updatedDTO))
              .header("Authorization", ADMIN_TOKEN)) // Use the token
          .andExpect(status().isNoContent());
  }

  @Test
  void PUT_user_id_404() throws Exception {
      // Test case where user doesn't exist
      UserPutDTO updatedDTO = new UserPutDTO();
      updatedDTO.setUsername("nonExistentUsername");

      mockMvc.perform(put("/users/99999")
              .contentType(MediaType.APPLICATION_JSON)
              .content(asJsonString(updatedDTO))
              .header("Authorization", ADMIN_TOKEN)) // Use the token
          .andExpect(status().isNotFound());
  }

  @Test
  void GET_user_id_404() throws Exception {
    mockMvc.perform(get("/users/99999")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", ADMIN_TOKEN)) // Use the token
    .andExpect(status().isNotFound());
  }


  @Test
  public void createUser_validInput_userCreated() throws Exception {
      // given
      UserPostDTO userPostDTO = new UserPostDTO();
      userPostDTO.setUsername("testUsername");
      userPostDTO.setName("testName");
      userPostDTO.setPassword("testPassword");
  
      // when/then -> do the request + validate the result
      mockMvc.perform(post("/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(asJsonString(userPostDTO)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.username", is("testUsername")))
          .andExpect(jsonPath("$.status", is(UserStatus.ONLINE.toString())));
  }
  @Test
  public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
      // given
      User user = new User();
      user.setUsername("firstname@lastname");
      user.setName("name");
      user.setPassword("password");
      user.setStatus(UserStatus.OFFLINE);
      user.setBirthday(null);
      user.setDate(LocalDate.now());
      user.setToken(UUID.randomUUID().toString());

      // Persist the user in the database
      userRepository.saveAndFlush(user);

      // when
      MockHttpServletRequestBuilder getRequest = get("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", ADMIN_TOKEN);

      // then
      mockMvc.perform(getRequest)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(2))) // There's also the admin in there
          .andExpect(jsonPath("$[1].username", is(user.getUsername()))) // Verify the username
          .andExpect(jsonPath("$[1].status", is(user.getStatus().toString()))) // Verify the status
          .andExpect(jsonPath("$[1].date", is(user.getDate().toString())));
  }
  /**
   * Helper Method to convert userPostDTO into a JSON string such that the input
   * can be processed
   * Input will look like this: {"name": "Test User", "username": "testUsername"}
   * 
   * @param object
   * @return string
   */
  private String asJsonString(final Object object) {
      try {
          return new ObjectMapper().writeValueAsString(object);
      } catch (JsonProcessingException e) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
              String.format("The request body could not be created.%s", e.toString()));
      }
  }
}