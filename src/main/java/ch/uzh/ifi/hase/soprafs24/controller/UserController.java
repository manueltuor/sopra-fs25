package ch.uzh.ifi.hase.soprafs24.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LogOutDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class UserController {
  private static final Logger logger = LoggerFactory.getLogger(UserController.class);
  private final UserService userService;

  UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/users")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<UserGetDTO> getAllUsers(@RequestHeader (value = "Authorization", required = false) String authToken) {
    User authenticatedUser = userService.getUserByToken(authToken);
    if (authToken == null || authenticatedUser == null) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing token");
    }
    List<User> users = userService.getUsers();
    List<UserGetDTO> userGetDTOs = new ArrayList<>();
    for (User user : users) {
      userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
    }
    return userGetDTOs;
  }

  @PostMapping("/login/auth")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public UserGetDTO loginUser(@RequestBody UserPostDTO userPostDTO) {
    logger.info("Got request to login user: {}", userPostDTO.getUsername());
    User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
    User loggedInUser = userService.loginUser(userInput);
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(loggedInUser);
  }

  @PostMapping("/users")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
    User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
    User createdUser = userService.createUser(userInput);
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
  }

  @GetMapping("/users/{id}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public UserGetDTO getUserById(@PathVariable Long id, @RequestHeader(value="Authorization", required=false) String authToken ) {
      User authenticatedUser = userService.getUserByToken(authToken);
      if (authToken == null) {
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
      }
      if ( authenticatedUser == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Missing token");
      }
      System.out.println(String.format("GET request for user with ID: %d", id));
      User user = userService.getUserById(id);
      return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
  }

  @PutMapping("/users/logout")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<Void> logoutUser(@RequestBody LogOutDTO logOutDTO) {
    try {
        User user = userService.getUserById(logOutDTO.getId());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (!user.getToken().equals(logOutDTO.getToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.logoutUser(user);

        return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().build();
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

  @PutMapping("/users/{id}")
  public ResponseEntity<?> editUser(@PathVariable Long id, @RequestBody UserPutDTO userPutDTO,
  @RequestHeader(value="Authorization", required=false) String authToken) {
    User authenticatedUser = userService.getUserByToken(authToken);
    if (authToken == null) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
    }
    if ( authenticatedUser == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Missing token");
    }
    User user = userService.getUserById(id);

    if (user == null) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body("User not found");
    }
      try {
          userService.editUser(user, userPutDTO);
          return ResponseEntity.noContent().build();
      } catch (IllegalArgumentException e) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
      } catch (Exception e) {
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
      }
  }
}
