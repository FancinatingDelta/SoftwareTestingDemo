package com.demo.controller.user;

import com.demo.entity.Message;
import com.demo.entity.User;
import com.demo.entity.vo.MessageVo;
import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(MessageController.class)
public class MessageControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageService messageService;

    @MockBean
    private MessageVoService messageVoService;

    @Test
    void messageListPage_shouldReturnMessageListView_whenLoggedIn() throws Exception {
        User user = new User();
        user.setUserID("u001");
        user.setUserName("Alice");
        user.setPicture("");

        Page<Message> passPage = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 5, Sort.by("time").descending()),
                0
        );
        Page<Message> userPage = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 5, Sort.by("time").descending()),
                0
        );

        when(messageService.findPassState(any())).thenReturn(passPage);
        when(messageVoService.returnVo(eq(Collections.emptyList()))).thenReturn(Collections.emptyList());
        when(messageService.findByUser(eq("u001"), any())).thenReturn(userPage);

        mockMvc.perform(get("/message_list").sessionAttr("user", user))
                .andExpect(status().isOk())
                .andExpect(view().name("message_list"));

        verify(messageService).findPassState(any());
        verify(messageVoService).returnVo(eq(Collections.emptyList()));
        verify(messageService).findByUser(eq("u001"), any());
        verifyNoMoreInteractions(messageService, messageVoService);
    }

    @Test
    void messageListPage_shouldThrowNestedServletException_whenNotLoggedIn() throws Exception {
        // Controller 会先查 messageService.findPassState(...)，但最终因 session.user==null 抛 LoginException
        assertThrows(NestedServletException.class, () ->
                mockMvc.perform(get("/message_list"))
                        .andReturn()
        );
    }

    @Test
    void getMessageListApi_shouldReturnJsonArray_whenPageIs1() throws Exception {
        Page<Message> page = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 5, Sort.by("time").descending()),
                0
        );
        when(messageService.findPassState(any())).thenReturn(page);

        List<MessageVo> voList = Collections.emptyList();
        when(messageVoService.returnVo(eq(Collections.emptyList()))).thenReturn(voList);

        mockMvc.perform(get("/message/getMessageList").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("[]")));

        verify(messageService).findPassState(any());
        verify(messageVoService).returnVo(eq(Collections.emptyList()));
        verifyNoMoreInteractions(messageService, messageVoService);
    }

    @Test
    void getMessageListApi_shouldThrowNestedServletException_whenPageIs0_boundaryValue() throws Exception {
        assertThrows(NestedServletException.class, () ->
                mockMvc.perform(get("/message/getMessageList").param("page", "0"))
                        .andReturn()
        );
    }

    @Test
    void findUserListApi_shouldReturnJsonArray_whenLoggedIn_andPageIs1() throws Exception {
        User user = new User();
        user.setUserID("u001");
        user.setUserName("Alice");
        user.setPicture("");

        Page<Message> userPage = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 5, Sort.by("time").descending()),
                0
        );

        when(messageService.findByUser(eq("u001"), any())).thenReturn(userPage);
        when(messageVoService.returnVo(eq(Collections.emptyList()))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/message/findUserList")
                        .param("page", "1")
                        .sessionAttr("user", user))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("[]")));

        verify(messageService).findByUser(eq("u001"), any());
        verify(messageVoService).returnVo(eq(Collections.emptyList()));
        verifyNoMoreInteractions(messageService, messageVoService);
    }

    @Test
    void findUserListApi_shouldThrowNestedServletException_whenNotLoggedIn() throws Exception {
        assertThrows(NestedServletException.class, () ->
                mockMvc.perform(get("/message/findUserList").param("page", "1"))
                        .andReturn()
        );
    }

    @Test
    void sendMessage_shouldRedirectToMessageList_andCallCreate() throws Exception {
        mockMvc.perform(post("/sendMessage")
                        .param("userID", "u001")
                        .param("content", "hello"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/message_list"));

        verify(messageService).create(any(Message.class));
        verifyNoMoreInteractions(messageService, messageVoService);
    }

    @Test
    void modifyMessage_shouldReturnTrue_andCallUpdate() throws Exception {
        Message message = new Message();
        message.setMessageID(1);
        message.setUserID("u001");
        message.setContent("old");

        when(messageService.findById(1)).thenReturn(message);

        mockMvc.perform(post("/modifyMessage.do")
                        .param("messageID", "1")
                        .param("content", "new"))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("true")));

        verify(messageService).findById(1);
        verify(messageService).update(any(Message.class));
        verifyNoMoreInteractions(messageService, messageVoService);
    }

    @Test
    void delMessage_shouldReturnTrue_andCallDelete() throws Exception {
        mockMvc.perform(post("/delMessage.do").param("messageID", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("true")));

        verify(messageService).delById(1);
        verifyNoMoreInteractions(messageService, messageVoService);
    }
}

