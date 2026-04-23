package com.demo.controller.user;

import com.demo.entity.User;
import com.demo.entity.Venue;
import com.demo.service.VenueService;
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

import com.demo.controller.admin.TestRecordUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(VenueController.class)
public class VenueControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VenueService venueService;

    @Test
    void venueListPage_shouldReturnVenueListView_andProvideTotalPages() throws Exception {
        User user = new User();
        user.setUserID("u001");
        user.setUserName("Alice");
        user.setPicture("");

        Page<Venue> page = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 5, Sort.by("venueID").ascending()),
                0
        );

        when(venueService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/venue_list").sessionAttr("user", user))
                .andExpect(status().isOk())
                .andExpect(view().name("venue_list"));

        // Controller 内部会调用 findAll 两次：一次取 content，一次取 totalPages
        verify(venueService, times(2)).findAll(any());
        verifyNoMoreInteractions(venueService);
    }

    @Test
    void venueDetail_shouldReturnVenueView_andBindVenue() throws Exception {
        User user = new User();
        user.setUserID("u001");
        user.setUserName("Alice");
        user.setPicture("");

        Venue venue = new Venue();
        venue.setVenueID(7);
        venue.setVenueName("Gym A");
        venue.setAddress("Somewhere");
        venue.setPicture("");

        when(venueService.findByVenueID(7)).thenReturn(venue);

        mockMvc.perform(get("/venue")
                        .param("venueID", "7")
                        .sessionAttr("user", user))
                .andExpect(status().isOk())
                .andExpect(view().name("venue"));

        verify(venueService).findByVenueID(7);
        verifyNoMoreInteractions(venueService);
    }

    @Test
    void getVenueListApi_shouldReturnJson_whenPageIs1() throws Exception {
        Page<Venue> page = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 5, Sort.by("venueID").ascending()),
                0
        );
        when(venueService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/venuelist/getVenueList").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));

        verify(venueService).findAll(any());
        verifyNoMoreInteractions(venueService);
    }

    @Test
    void getVenueListApi_shouldRecordActual_whenPageIs0_boundaryValue() {
        try {
            mockMvc.perform(get("/venuelist/getVenueList").param("page", "0"));
            TestRecordUtil.recordSuccess("getVenueListApi(page=0)");
        } catch (Exception e) {
            TestRecordUtil.recordException("getVenueListApi(page=0)", e);
        }
    }
}

