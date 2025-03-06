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

  private static User testUser;
  private static String authTOKEN;

  @BeforeEach
  public void setup() throws Exception {
      // Clear database + new user
      userRepository.deleteAll();
      userRepository.flush();
      User user = new User();
      user.setPassword("kahn");
      user.setUsername("king");
      user.setName("kong");
      user.setToken(UUID.randomUUID().toString());
      user.setStatus(UserStatus.ONLINE);
      user.setDate(LocalDate.now());
  
      // Save and also flush
      User createdUser = userRepository.saveAndFlush(user);
      authTOKEN = createdUser.getToken();
      testUser = createdUser;
  
      System.out.println("Setup completed!");
      System.out.println("User ID: " + testUser.getId());
      System.out.println("Admin Token: " + authTOKEN);
  }

  @Test
  void user_POST201() throws Exception {
      UserPostDTO userPostDTO = new UserPostDTO();
      userPostDTO.setUsername("testUser");
      userPostDTO.setName("testUser");
      userPostDTO.setPassword("testPassword");
      mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(asJsonString(userPostDTO))).andExpect(status().isCreated());
  }

  @Test
  void userId_GET200() throws Exception {
      // Check if the user is in the database
      List<User> users = userRepository.findAll();
      users.forEach(user -> System.out.println("DB User: " + user.getUsername() + ", Token: " + user.getToken()));
  
      System.out.println("Test User: " + testUser);
    
      // Use the stored test user for GET request
      mockMvc.perform(get("/users/{id}", testUser.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", authTOKEN.trim().replace("\"", "")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.username", is(testUser.getUsername())))
          .andExpect(jsonPath("$.status", is(testUser.getStatus().toString())));
  }

  @Test
  void user_POST409() throws Exception {
      // Create first user
      UserPostDTO userPostDTO = new UserPostDTO();
      userPostDTO.setUsername("testUser");
      userPostDTO.setName("testUser");
      userPostDTO.setPassword("testPassword");
      mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(asJsonString(userPostDTO))).andExpect(status().isCreated());
      //flushing
      userRepository.flush();
      User firstUser = userRepository.findByUsername("testUser");
      System.out.println("First User: " + firstUser);
  
      // duplicate user creation
      UserPostDTO copyUserPostDTO = new UserPostDTO(); // duplicate user
      copyUserPostDTO.setUsername("testUser");
      copyUserPostDTO.setPassword("testPassword");
  
      mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(asJsonString(copyUserPostDTO))).andExpect(status().isConflict());
  }
  @Test
  void userId_PUT404() throws Exception {
      UserPutDTO updatedDTO = new UserPutDTO();
      updatedDTO.setUsername("nonExistentUsername");
      mockMvc.perform(put("/users/99999").contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(updatedDTO))
            .header("Authorization", authTOKEN))
            .andExpect(status().isNotFound());
    // nonexistent user test
  }

  @Test
  void userId_GET404() throws Exception {
    mockMvc.perform(get("/users/99999")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", authTOKEN))
    .andExpect(status().isNotFound());
  }

  @Test
  void userId_PUT204() throws Exception {
      UserPutDTO updatedDTO = new UserPutDTO();
      updatedDTO.setUsername("updatedUsername");

      mockMvc.perform(put("/users/" + testUser.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(updatedDTO))
        .header("Authorization", authTOKEN)).andExpect(status().isNoContent());
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

      // flush user
      userRepository.saveAndFlush(user);

      // when
      MockHttpServletRequestBuilder getRequest = get("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", authTOKEN);

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