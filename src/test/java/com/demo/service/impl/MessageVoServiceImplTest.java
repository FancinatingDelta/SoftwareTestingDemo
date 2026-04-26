package com.demo.service.impl;

import com.demo.dao.MessageDao;
import com.demo.dao.UserDao;
import com.demo.entity.Message;
import com.demo.entity.User;
import com.demo.entity.vo.MessageVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.demo.service.MessageService.STATE_NO_AUDIT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageVoServiceImplTest {

    @Mock
    private MessageDao messageDao;

    @Mock
    private UserDao userDao;

    @InjectMocks
    private MessageVoServiceImpl messageVoService;

    private Message message;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserID("U001");
        user.setUserName("Alice");
        user.setPicture("avatar.jpg");

        message = new Message();
        message.setMessageID(101);
        message.setUserID("U001");
        message.setContent("Hello World");
        message.setTime(LocalDateTime.now());
        message.setState(STATE_NO_AUDIT);
    }

    @Test
    void testReturnMessageVoByMessageID_Success() {
        when(messageDao.findByMessageID(101)).thenReturn(message);
        when(userDao.findByUserID("U001")).thenReturn(user);

        MessageVo vo = messageVoService.returnMessageVoByMessageID(101);

        assertNotNull(vo);
        assertEquals(101, vo.getMessageID());
        assertEquals("U001", vo.getUserID());
        assertEquals("Alice", vo.getUserName());
        assertEquals("Hello World", vo.getContent());
    }

    @Test
    void testReturnMessageVoByMessageID_MessageNotFound() {
        when(messageDao.findByMessageID(999)).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> messageVoService.returnMessageVoByMessageID(999));
    }

    @Test
    void testReturnMessageVoByMessageID_UserNotFound() {
        when(messageDao.findByMessageID(101)).thenReturn(message);
        when(userDao.findByUserID("U001")).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> messageVoService.returnMessageVoByMessageID(101));
    }

    // ==================== 批量 MessageVo ====================

    @Test
    void testReturnVo_Success() {
        Message m1 = new Message();
        m1.setMessageID(101);
        m1.setUserID("U001");

        Message m2 = new Message();
        m2.setMessageID(102);
        m2.setUserID("U001");

        List<Message> messages = Arrays.asList(m1, m2);

        when(messageDao.findByMessageID(101)).thenReturn(m1);
        when(messageDao.findByMessageID(102)).thenReturn(m2);
        when(userDao.findByUserID("U001")).thenReturn(user);

        List<MessageVo> vos = messageVoService.returnVo(messages);

        assertEquals(2, vos.size());
        assertEquals(101, vos.get(0).getMessageID());
        assertEquals(102, vos.get(1).getMessageID());
    }

    @Test
    void testReturnVo_EmptyList() {
        List<MessageVo> vos = messageVoService.returnVo(new ArrayList<>());

        assertNotNull(vos);
        assertTrue(vos.isEmpty());
    }
}