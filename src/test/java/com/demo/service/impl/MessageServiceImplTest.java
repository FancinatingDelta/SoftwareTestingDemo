package com.demo.service.impl;

import com.demo.dao.MessageDao;
import com.demo.entity.Message;
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

import javax.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageDao messageDao;

    @InjectMocks
    private MessageServiceImpl messageService;

    private Message message;

    @BeforeEach
    void setUp() {
        message = new Message();
        message.setMessageID(1);
        message.setUserID("user001");
        message.setContent("Say something");
        message.setState(MessageServiceImpl.STATE_NO_AUDIT);
    }

    @Test
    void testFindById_Success() {
        when(messageDao.getOne(1)).thenReturn(message);

        Message result = messageService.findById(1);

        assertNotNull(result);
        assertEquals("Say something", result.getContent());
    }

    @Test
    void testFindById_NotFound() {
        when(messageDao.getOne(999)).thenThrow(EntityNotFoundException.class);

        assertThrows(EntityNotFoundException.class,
                () -> messageService.findById(999));
    }

    @Test
    void testFindByUser_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Message> list = Arrays.asList(message);
        Page<Message> page = new PageImpl<>(list, pageable, list.size());

        when(messageDao.findAllByUserID("U001", pageable)).thenReturn(page);

        Page<Message> result = messageService.findByUser("U001", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testFindByUser_NotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Message> list = Arrays.asList(message);
        Page<Message> page = new PageImpl<>(list, pageable, list.size());

        when(messageDao.findAllByUserID("U111", pageable)).thenReturn(null);

        assertNull(messageService.findByUser("U111", pageable));
    }

    @Test
    void testCreate_Success() {
        when(messageDao.save(message)).thenReturn(message);

        int id = messageService.create(message);

        assertEquals(1, id);
    }

    @Test
    void testCreate_Fail_NullObject() {
        assertThrows(NullPointerException.class,
                () -> messageService.create(message));
    }

    @Test
    void testDelById_Success() {
        doNothing().when(messageDao).deleteById(1);

        assertDoesNotThrow(() -> messageService.delById(1));

        verify(messageDao).deleteById(1);
    }

    @Test
    void testDelById_NotFound() {
        doThrow(new EmptyResultDataAccessException(1))
                .when(messageDao).deleteById(999);

        assertThrows(EmptyResultDataAccessException.class,
                () -> messageService.delById(999));
    }

    @Test
    void testUpdate_Success() {
        when(messageDao.save(message)).thenReturn(message);

        assertDoesNotThrow(() -> messageService.update(message));

        verify(messageDao).save(message);
    }

    @Test
    void testConfirmMessage_Success() {
        when(messageDao.findByMessageID(1)).thenReturn(message);

        assertDoesNotThrow(() -> messageService.confirmMessage(1));

        verify(messageDao).updateState(MessageServiceImpl.STATE_PASS, 1);
    }

    @Test
    void testConfirmMessage_NotFound() {
        when(messageDao.findByMessageID(999)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> messageService.confirmMessage(999));

        assertEquals("留言不存在", ex.getMessage());
    }

    @Test
    void testRejectMessage_Success() {
        when(messageDao.findByMessageID(1)).thenReturn(message);

        assertDoesNotThrow(() -> messageService.rejectMessage(1));

        verify(messageDao).updateState(MessageServiceImpl.STATE_REJECT, 1);
    }

    @Test
    void testRejectMessage_NotFound() {
        when(messageDao.findByMessageID(999)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> messageService.rejectMessage(999));

        assertEquals("留言不存在", ex.getMessage());
    }

    @Test
    void testFindWaitState_Success() {
        Message m1 = new Message();
        m1.setMessageID(101);
        m1.setUserID("user01");
        m1.setState(MessageServiceImpl.STATE_NO_AUDIT);

        Message m2 = new Message();
        m2.setMessageID(102);
        m2.setUserID("user02");
        m2.setState(MessageServiceImpl.STATE_NO_AUDIT);

        Message m3 = new Message();
        m3.setMessageID(103);
        m3.setUserID("user03");
        m3.setState(MessageServiceImpl.STATE_NO_AUDIT);

        Pageable pageable = PageRequest.of(0, 10);
        List<Message> list = Arrays.asList(m1, m2, m3);
        Page<Message> page = new PageImpl<>(list, pageable, list.size());

        when(messageDao.findAllByState(MessageServiceImpl.STATE_NO_AUDIT, pageable))
                .thenReturn(page);

        Page<Message> result = messageService.findWaitState(pageable);

        assertNotNull(result);
        assertEquals(3, result.getTotalElements());

        assertEquals(101, result.getContent().get(0).getMessageID());
        assertEquals(102, result.getContent().get(1).getMessageID());
        assertEquals(103, result.getContent().get(2).getMessageID());
    }

    @Test
    void testFindPassState_Success() {
        Message m1 = new Message();
        m1.setMessageID(201);
        m1.setUserID("user01");
        m1.setState(MessageServiceImpl.STATE_PASS);

        Message m2 = new Message();
        m2.setMessageID(202);
        m2.setUserID("user02");
        m2.setState(MessageServiceImpl.STATE_PASS);

        Pageable pageable = PageRequest.of(0, 10);
        List<Message> list = Arrays.asList(m1, m2);
        Page<Message> page = new PageImpl<>(list, pageable, list.size());

        when(messageDao.findAllByState(MessageServiceImpl.STATE_PASS, pageable))
                .thenReturn(page);

        Page<Message> result = messageService.findPassState(pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());

        assertEquals(201, result.getContent().get(0).getMessageID());
        assertEquals(202, result.getContent().get(1).getMessageID());
    }
}