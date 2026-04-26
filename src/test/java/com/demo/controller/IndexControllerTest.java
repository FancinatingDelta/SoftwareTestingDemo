package com.demo.controller;

import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import com.demo.service.NewsService;
import com.demo.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class IndexControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

    @MockBean
    private VenueService venueService;

    @MockBean
    private MessageService messageService;

    @MockBean
    private MessageVoService messageVoService;

    /**
     * 测试：首页数据为空的情况
     * 等价类：边界值（空数据）
     */
    @Test
    void index_shouldHandleEmptyDataGracefully() throws Exception {
        when(newsService.findAll(any(PageRequest.class))).thenReturn(Page.empty());
        when(venueService.findAll(any(PageRequest.class))).thenReturn(Page.empty());
        when(messageService.findPassState(any(PageRequest.class))).thenReturn(Page.empty());
        when(messageVoService.returnVo(any(List.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/index"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("news_list", Collections.emptyList()))
                .andExpect(model().attribute("venue_list", Collections.emptyList()))
                .andExpect(model().attribute("message_list", Collections.emptyList()));
    }

    /**
     * 测试：访问管理员首页
     * 等价类：正常访问
     */
    @Test
    void adminIndex_shouldReturnAdminIndexView() throws Exception {
        mockMvc.perform(get("/admin_index"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admin_index"));
    }
}