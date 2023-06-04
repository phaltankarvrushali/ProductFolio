package io.swagger.api;

import io.swagger.database.UserRepository;
import io.swagger.model.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.*;

public class UserControllerTest
{
    @Mock
    UserRepository userRepository;

    @Test
    public void TestgetUser_Test() {
        UserRepository userRepository = mock(UserRepository.class);

        when(userRepository.get(123L)).thenReturn(getTestUser());

        ResponseEntity<User> user1 = new UserController().getUser("Basic Y2E6Y2Ex", "123");

        Assertions.assertNotNull(user1);
    }

    @Test
    public void TestcreateUser_Test(){
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.get(123L)).thenReturn(getTestUser());
        doNothing().when(userRepository).save(getTestUser());
        ResponseEntity<User> user1 = new UserController().createUser(getTestuser());

        Assertions.assertNotNull(user1);
    }

    @Test
    public void TestupdateUser_Test(){
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.get(123L)).thenReturn(getTestUser());
        doNothing().when(userRepository).save(getTestUser());
        ResponseEntity<User> user1 = new UserController().updateUser("Basic Y2E6Y2Ex","1",getTestuser());

        Assertions.assertNotNull(user1);
    }

    io.swagger.database.entities.User getTestUser()
    {
        io.swagger.database.entities.User user = new io.swagger.database.entities.User();

        user.setId(Long.valueOf(1));
        user.setAccountUpdated(String.valueOf(OffsetDateTime.now()));
        user.setAccountCreated(String.valueOf(OffsetDateTime.now()));
        user.setUsername("user123@gmail.com");
        user.setPassword("pass123");
        user.setLastName("Doe");
        user.setFirstName("Jane");
        return user;
    }

    User getTestuser()
    {
        User user = new User();

        user.setId(Long.valueOf(1));
        user.setAccountUpdated(String.valueOf(OffsetDateTime.now()));
        user.setAccountCreated(String.valueOf(OffsetDateTime.now()));
        user.setUsername("user123@gmail.com");
        user.setPassword("pass123");
        user.setLastName("Doe");
        user.setFirstName("Jane");
        return user;
    }
}
