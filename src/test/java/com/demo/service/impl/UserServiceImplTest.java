package com.demo.service.impl;

import com.demo.dao.UserDao;
import com.demo.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setUserID("test");
        user.setUserName("root");
        user.setPassword("123456");
    }

    @Test
    void testFindByUserID_Success() {
        when(userDao.findByUserID("test")).thenReturn(user);

        User result = userService.findByUserID("test");

        assertNotNull(result);
        assertEquals("root", result.getUserName());
    }

    @Test
    void testFindByUserID_NotFound() {
        when(userDao.findByUserID("Unknown")).thenReturn(null);

        assertNull(userService.findByUserID("Unknown"));
    }

    @Test
    void testFindById_Success() {
        when(userDao.findById(1)).thenReturn(user);

        User result = userService.findById(1);

        assertNotNull(result);
        assertEquals("test", result.getUserID());
    }

    @Test
    void testFindById_NotFound() {
        when(userDao.findById(999)).thenReturn(null);

        assertNull(userService.findById(999));
    }

    @Test
    void testFindByUserID_Pageable_Success() {
        User u1 = new User();
        u1.setId(1);
        u1.setUserID("U001");
        u1.setUserName("Alice");

        User u2 = new User();
        u2.setId(2);
        u2.setUserID("U002");
        u2.setUserName("Bob");

        User u3 = new User();
        u3.setId(3);
        u3.setUserID("U003");
        u3.setUserName("Charlie");

        Page<User> page = new PageImpl<>(
                Arrays.asList(u1, u2, u3),
                PageRequest.of(0, 10),
                3
        );

        when(userDao.findAllByIsadmin(eq(0), any(Pageable.class)))
                .thenReturn(page);

        Page<User> result = userService.findByUserID(PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getTotalPages());

        List<User> content = result.getContent();
        assertEquals("Alice", content.get(0).getUserName());
        assertEquals("Bob", content.get(1).getUserName());
        assertEquals("Charlie", content.get(2).getUserName());
    }

    @Test
    void testCheckLogin_Success() {
        when(userDao.findByUserIDAndPassword("test", "123456")).thenReturn(user);

        User result = userService.checkLogin("test", "123456");

        assertNotNull(result);
        assertEquals("root", result.getUserName());
    }

    @Test
    void testCheckLogin_WrongPassword() {
        when(userDao.findByUserIDAndPassword("test", "1234567")).thenReturn(null);

        User result = userService.checkLogin("test", "1234567");

        assertNull(result);
    }

    @Test
    void testCreate_Success() {
        when(userDao.findAll()).thenReturn(Arrays.asList(new User(), new User()));

        int count = userService.create(user);

        assertEquals(2, count);
        verify(userDao).save(user);
    }

    @Test
    void testUpdateUser_Success() {
        when(userDao.save(user)).thenReturn(user);

        assertDoesNotThrow(() -> userService.updateUser(user));
        verify(userDao).save(user);
    }

    @Test
    void testDelByID_Success() {
        doNothing().when(userDao).deleteById(1);

        assertDoesNotThrow(() -> userService.delByID(1));
        verify(userDao).deleteById(1);
    }

    @Test
    void testDelByID_NotFound() {
        doThrow(new EmptyResultDataAccessException(1))
                .when(userDao).deleteById(999);

        assertThrows(EmptyResultDataAccessException.class,
                () -> userService.delByID(999));
    }

    @Test
    void testCountUserID_Success() {
        when(userDao.countByUserID("test")).thenReturn(1);

        int count = userService.countUserID("test");

        assertEquals(1, count);
    }

    @Test
    void testCountUserID_NotFound() {
        when(userDao.countByUserID("Unknown")).thenReturn(0);

        int count = userService.countUserID("Unknown");

        assertEquals(0, count);
    }
}