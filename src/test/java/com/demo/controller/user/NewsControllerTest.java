package com.demo.controller.user;

import com.demo.entity.News;
import com.demo.entity.User;
import com.demo.service.NewsService;
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

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(NewsController.class)
class NewsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

    @Test
    void newsListPage_shouldReturnNewsListView_andProvideTotalPages() throws Exception {
        User user = new User();
        user.setUserID("u001");
        user.setUserName("Alice");
        user.setPicture("");

        Page<News> page = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 5, Sort.by("time").descending()),
                0
        );
        when(newsService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/news_list").sessionAttr("user", user))
                .andExpect(status().isOk())
                .andExpect(view().name("news_list"));

        // Controller 内部会调用 findAll 两次：一次取 content，一次取 totalPages
        verify(newsService, times(2)).findAll(any());
        verifyNoMoreInteractions(newsService);
    }

    @Test
    void newsDetail_shouldReturnNewsView_andBindNews() throws Exception {
        User user = new User();
        user.setUserID("u001");
        user.setUserName("Alice");
        user.setPicture("");

        News news = new News();
        news.setNewsID(1);
        news.setTitle("T");
        news.setContent("C");
        news.setTime(LocalDateTime.now());

        when(newsService.findById(1)).thenReturn(news);

        mockMvc.perform(get("/news")
                        .param("newsID", "1")
                        .sessionAttr("user", user))
                .andExpect(status().isOk())
                .andExpect(view().name("news"));

        verify(newsService).findById(1);
        verifyNoMoreInteractions(newsService);
    }

    @Test
    void getNewsListApi_shouldReturnJson_whenPageIs1() throws Exception {
        Page<News> page = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 5, Sort.by("time").descending()),
                0
        );
        when(newsService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/news/getNewsList").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));

        verify(newsService).findAll(any());
        verifyNoMoreInteractions(newsService);
    }

    @Test
    void getNewsListApi_shouldThrowNestedServletException_whenPageIs0_boundaryValue() throws Exception {
        assertThrows(NestedServletException.class, () ->
                mockMvc.perform(get("/news/getNewsList").param("page", "0"))
                        .andReturn()
        );
    }
}

