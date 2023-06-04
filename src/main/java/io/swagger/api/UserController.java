package io.swagger.api;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import io.swagger.annotations.ApiParam;
import io.swagger.database.UserRepository;
import io.swagger.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class UserController implements HealthzApi
{
    private static final StatsDClient statsd = new NonBlockingStatsDClient("webapp", "localhost", 8125);

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    @Autowired
    UserRepository userRepository;

    @RequestMapping(value = "/v4/user/{userId}",
            produces = { "application/json" },
            method = RequestMethod.GET)
    public ResponseEntity<User> getUser(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,@ApiParam(value = "Unique identifier of the User ", required = true) @PathVariable("userId") String userId) {
        statsd.increment("getUser");
        try {
            io.swagger.database.entities.User retrievedUser = null;
            retrievedUser = userRepository.get(Long.valueOf(userId));

            User responseUser = null;
            if (null != retrievedUser) {
                responseUser = responseMapper(retrievedUser);
            } else {
                logger.debug("No User exist with given Id {}", userId);
                return new ResponseEntity("No User exists with given Id", HttpStatus.BAD_REQUEST);
            }

            if (!StringUtils.hasText(authorization)) {
                return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
            }

            Map<String, String> creds = getCredentials(authorization);

            String pass = creds.get(retrievedUser.getUsername());

            if (!StringUtils.hasText(pass)) {
                return new ResponseEntity("Forbidden", HttpStatus.FORBIDDEN);
            }

            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

            if (!passwordEncoder.matches(pass, retrievedUser.getPassword())) {
                return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
            }

            logger.info("Retrieved User {}", responseUser.getId());
            return new ResponseEntity<User>(responseUser, HttpStatus.OK);
        }
        catch(Exception e)
        {
            return new ResponseEntity("Bad Gateway", HttpStatus.BAD_GATEWAY);
        }
    }

    @RequestMapping(value = "/v4/user/{userId}",
            method = RequestMethod.PUT)
    public ResponseEntity<User> updateUser(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @ApiParam(value = "Unique identifier of the User ", required = true) @PathVariable("userId") String userId, @ApiParam(value = "User to be Created or Updated", required = true) @Valid @RequestBody User user) {
        statsd.increment("updateUser");
        try {
            io.swagger.database.entities.User retrieveUser = null;
            retrieveUser = userRepository.get(Long.valueOf(userId));

            if (null == retrieveUser) {
                logger.debug("No User with Id {} exist in the system",userId);
                return new ResponseEntity("No User exists with given Id", HttpStatus.BAD_REQUEST);
            }

            if (!StringUtils.hasText(authorization)) {
                return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
            }

            Map<String, String> creds = getCredentials(authorization);

            String pass = creds.get(retrieveUser.getUsername());

            if (!StringUtils.hasText(pass)) {
                return new ResponseEntity("Forbidden", HttpStatus.FORBIDDEN);
            }

            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

            if (!passwordEncoder.matches(pass, retrieveUser.getPassword())) {
                return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
            }

            if (user.getAccountCreated() == null && user.getAccountCreated() == null && user.getId() == null) {

            } else {
                logger.debug("Invalid request body");
                return new ResponseEntity("Invalid Request Body.", HttpStatus.BAD_REQUEST);
            }

            if (!user.getUsername().equalsIgnoreCase(retrieveUser.getUsername())) {
                logger.debug("Username cannot be updated");
                return new ResponseEntity("cannot update Username", HttpStatus.BAD_REQUEST);
            }

            if (!user.getFirstName().isEmpty()) {
                retrieveUser.setFirstName(user.getFirstName());
            }
            if (!user.getLastName().isEmpty()) {
                retrieveUser.setLastName(user.getLastName());
            }
            if (!user.getPassword().isEmpty()) {
                retrieveUser.setPassword(passwordEncoder.encode(user.getPassword()));
            }

            retrieveUser.setAccountUpdated(String.valueOf(OffsetDateTime.now()));
            io.swagger.database.entities.User updatedUser = null;
            userRepository.save(retrieveUser);
            updatedUser = userRepository.get(retrieveUser.getId());

            logger.info("User with Id {} updated successfully", updatedUser.getId());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        catch(Exception e)
        {
            return new ResponseEntity("Bad Gateway", HttpStatus.BAD_GATEWAY);
        }
    }

    @RequestMapping(value = "/v4/user",
            method = RequestMethod.POST)
    public ResponseEntity<User> createUser(@ApiParam(value = "User to be Created or Updated", required = true) @Valid @RequestBody User user) {
        statsd.increment("createUser");
        try {
            if (user.getAccountCreated() == null && user.getAccountCreated() == null && user.getId() == null) {

            } else {
                return new ResponseEntity("Invalid Request Body.", HttpStatus.BAD_REQUEST);
            }


            if (null != userRepository.getUserWithUsername(user.getUsername())) {
                return new ResponseEntity("User already exists", HttpStatus.BAD_REQUEST);
            }


            String regex = "^[A-Za-z0-9+_.-]+@(.+)$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(user.getUsername());

            if (!matcher.matches()) {
                logger.debug("Invalid username");
                return new ResponseEntity("Invalid username", HttpStatus.BAD_REQUEST);
            }


            io.swagger.database.entities.User saveUser = new io.swagger.database.entities.User();

            saveUser.setFirstName(user.getFirstName());
            saveUser.setLastName(user.getLastName());
            saveUser.setUsername(user.getUsername());
            saveUser.setAccountCreated(String.valueOf(OffsetDateTime.now()));
            saveUser.setAccountUpdated(String.valueOf(OffsetDateTime.now()));

            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            saveUser.setPassword(passwordEncoder.encode(user.getPassword()));
            logger.debug("User validated successfully");
            userRepository.save(saveUser);

            logger.debug("New User created successfully. User Id: {}", saveUser.getId());

            return new ResponseEntity<User>(responseMapper(saveUser), HttpStatus.CREATED);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return new ResponseEntity("Bad Gateway", HttpStatus.BAD_GATEWAY);
        }
    }

    private User responseMapper(io.swagger.database.entities.User user) {
        User responseUser = new User();

        responseUser.setId(user.getId());
        responseUser.setUsername(user.getUsername());
        responseUser.setLastName(user.getLastName());
        responseUser.setFirstName(user.getFirstName());
        responseUser.setAccountUpdated(user.getAccountUpdated());
        responseUser.setAccountCreated(user.getAccountCreated());

        return responseUser;
    }

    private Map<String, String> getCredentials(String encodedString)
    {
        Map<String, String> credentials = new HashMap<>();
        String[] auth = encodedString.split(" ");

        byte[] decodedBytes = Base64.getDecoder().decode(auth[1]);
        String decodedString = new String(decodedBytes);
        String[] creds = decodedString.split(":");

        credentials.put(creds[0],creds[1]);

        return credentials;
    }
}
