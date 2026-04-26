package com.demo.controller.user;

import com.demo.controller.admin.TestRecordUtil;
import com.demo.entity.Order;
import com.demo.entity.User;
import com.demo.entity.Venue;
import com.demo.entity.vo.OrderVo;
import com.demo.service.OrderService;
import com.demo.service.OrderVoService;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
public class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderVoService orderVoService;

    @MockBean
    private VenueService venueService;

    @Test
    void orderManage_shouldReturnOrderManageView_whenLoggedIn() throws Exception {
        User user = new User();
        user.setUserID("u001");
        user.setUserName("Alice");

        Page<Order> page = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 5, Sort.by("orderTime").descending()),
                0
        );

        when(orderService.findUserOrder(eq("u001"), any())).thenReturn(page);

        mockMvc.perform(get("/order_manage").sessionAttr("user", user))
                .andExpect(status().isOk())
                .andExpect(view().name("order_manage"))
                .andExpect(request().attribute("total", page.getTotalPages()));
    }

    @Test
    void orderManage_shouldRecordActual_whenNotLoggedIn() {
        try {
            mockMvc.perform(get("/order_manage"));
            TestRecordUtil.recordSuccess("orderManage(未登录)");
        } catch (Exception e) {
            TestRecordUtil.recordException("orderManage(未登录)", e);
        }
    }

    @Test
    void getOrderList_shouldReturnJsonArray_whenLoggedIn_andPageIs1() throws Exception {
        User user = new User();
        user.setUserID("u001");
        user.setUserName("Alice");

        Page<Order> orderPage = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 5, Sort.by("orderTime").descending()),
                0
        );
        when(orderService.findUserOrder(eq("u001"), any())).thenReturn(orderPage);

        List<OrderVo> voList = Collections.emptyList();
        when(orderVoService.returnVo(eq(Collections.emptyList()))).thenReturn(voList);

        mockMvc.perform(get("/getOrderList.do")
                        .param("page", "1")
                        .sessionAttr("user", user))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("[]")));

        verify(orderService).findUserOrder(eq("u001"), any());
        verify(orderVoService).returnVo(eq(Collections.emptyList()));
        verifyNoMoreInteractions(orderService, orderVoService, venueService);
    }

    @Test
    void getOrderList_shouldRecordActual_whenNotLoggedIn() {
        try {
            mockMvc.perform(get("/getOrderList.do").param("page", "1"));
            TestRecordUtil.recordSuccess("getOrderList(未登录)");
        } catch (Exception e) {
            TestRecordUtil.recordException("getOrderList(未登录)", e);
        }
    }

    @Test
    void getOrderList_shouldRecordActual_whenPageIs0_boundaryValue() {
        User user = new User();
        user.setUserID("u001");
        user.setUserName("Alice");

        // page=0 => PageRequest.of(-1,...) 会触发异常：典型边界值缺陷/风险点
        try {
            mockMvc.perform(get("/getOrderList.do")
                            .param("page", "0")
                            .sessionAttr("user", user));
            TestRecordUtil.recordSuccess("getOrderList(page=0)");
        } catch (Exception e) {
            TestRecordUtil.recordException("getOrderList(page=0)", e);
        }
    }

    @Test
    void delOrder_shouldReturnTrue_andCallService() throws Exception {
        mockMvc.perform(post("/delOrder.do").param("orderID", "123"))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("true")));

        verify(orderService).delOrder(123);
        verifyNoMoreInteractions(orderService, orderVoService, venueService);
    }

    @Test
    void finishOrder_shouldReturnOk_andCallService() throws Exception {
        mockMvc.perform(post("/finishOrder.do").param("orderID", "123"))
                .andExpect(status().isOk());

        verify(orderService).finishOrder(123);
        verifyNoMoreInteractions(orderService, orderVoService, venueService);
    }

    @Test
    void orderPlace_shouldReturnOrderPlaceView_whenAccessed() throws Exception {
        mockMvc.perform(get("/order_place"))
                .andExpect(status().isOk())
                .andExpect(view().name("order_place"));
    }

    @Test
    void orderPlaceDo_shouldReturnOrderPlaceView_andBindVenue() throws Exception {
        Venue venue = new Venue();
        venue.setVenueID(7);
        when(venueService.findByVenueID(7)).thenReturn(venue);

        mockMvc.perform(get("/order_place.do").param("venueID", "7"))
                .andExpect(status().isOk())
                .andExpect(view().name("order_place"));

        verify(venueService).findByVenueID(7);
        verifyNoMoreInteractions(orderService, orderVoService, venueService);
    }

    @Test
    void addOrder_shouldRedirectToOrderManage_andCallSubmit_whenLoggedIn() throws Exception {
        User user = new User();
        user.setUserID("u001");
        user.setUserName("Alice");

        // Controller 会把 startTime 拼成 "yyyy-MM-dd HH:mm:00" 再 parse 成 LocalDateTime
        String startTime = "2026-04-22 10:00";

        mockMvc.perform(post("/addOrder.do")
                        .param("venueName", "Gym A")
                        .param("date", "ignored-by-controller")
                        .param("startTime", startTime)
                        .param("hours", "2")
                        .sessionAttr("user", user))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("order_manage"));

        LocalDateTime expected = LocalDateTime.of(2026, 4, 22, 10, 0, 0);
        verify(orderService).submit(eq("Gym A"), eq(expected), eq(2), eq("u001"));
        verifyNoMoreInteractions(orderService, orderVoService, venueService);
    }

    @Test
    void addOrder_shouldRecordActual_whenNotLoggedIn() {
        try {
            mockMvc.perform(post("/addOrder.do")
                            .param("venueName", "Gym A")
                            .param("startTime", "2026-04-22 10:00")
                            .param("hours", "2"));
            TestRecordUtil.recordSuccess("addOrder(未登录)");
        } catch (Exception e) {
            TestRecordUtil.recordException("addOrder(未登录)", e);
        }
    }

    @Test
    void editOrder_shouldReturnOrderEditView_whenLoggedIn() throws Exception {
        User user = new User();
        user.setUserID("u001");

        Order order = new Order();
        order.setOrderID(1001);
        order.setVenueID(2001);

        Venue venue = new Venue();
        venue.setVenueID(2001);

        when(orderService.findById(1001)).thenReturn(order);
        when(venueService.findByVenueID(2001)).thenReturn(venue);

        mockMvc.perform(get("/modifyOrder.do")
                        .param("orderID", "1001")
                        .sessionAttr("user", user))
                .andExpect(status().isOk())
                .andExpect(view().name("order_edit"));
    }

    @Test
    void editOrder_shouldRecordActual_whenNotLoggedIn() {
        when(orderService.findById(1001)).thenReturn(new Order());

        try {
            mockMvc.perform(get("/modifyOrder.do")
                    .param("orderID", "1001"));
            TestRecordUtil.recordSuccess("editOrder(未登录)");
        } catch (Exception e) {
            TestRecordUtil.recordException("editOrder(未登录)", e);
        }
    }

    @Test
    void modifyOrder_shouldRedirectToOrderManage_whenLoggedIn() throws Exception {
        User user = new User();
        user.setUserID("u001");

        mockMvc.perform(post("/modifyOrder").sessionAttr("user", user)
                        .param("venueName", "VenueA")
                        .param("startTime", "2026-01-01 10:00")
                        .param("hours", "2")
                        .param("orderID", "1001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("order_manage"));

        verify(orderService).updateOrder(
                eq(1001),
                eq("VenueA"),
                any(LocalDateTime.class),
                eq(2),
                eq("u001")
        );
    }

    @Test
    void modifyOrder_shouldRecordActual_whenNotLoggedIn() {
        try {
            mockMvc.perform(post("/modifyOrder")
                    .param("venueName", "VenueA")
                    .param("startTime", "2026-01-02 10:00")
                    .param("hours", "2")
                    .param("orderID", "1001"));
            TestRecordUtil.recordSuccess("modifyOrder(未登录)");
        } catch (Exception e) {
            TestRecordUtil.recordException("modifyOrder(未登录)", e);
        }
    }

    @Test
    void venueOrderApi_shouldReturnVenueOrderJson_whenValidInput() throws Exception {
        Venue venue = new Venue();
        venue.setVenueID(7);
        venue.setVenueName("Gym A");

        when(venueService.findByVenueName("Gym A")).thenReturn(venue);
        when(orderService.findDateOrder(eq(7), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/order/getOrderList.do")
                        .param("venueName", "Gym A")
                        .param("date", "2026-04-22"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));

        verify(venueService).findByVenueName("Gym A");
        verify(orderService).findDateOrder(eq(7), any(LocalDateTime.class), any(LocalDateTime.class));
        verifyNoMoreInteractions(orderService, orderVoService, venueService);
    }
}

