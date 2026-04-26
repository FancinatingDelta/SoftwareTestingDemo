package com.demo.service.impl;

import com.demo.dao.OrderDao;
import com.demo.dao.VenueDao;
import com.demo.entity.Order;
import com.demo.entity.Venue;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.demo.service.OrderService.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderDao orderDao;

    @Mock
    private VenueDao venueDao;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private Venue venue;

    @BeforeEach
    void setUp() {
        venue = new Venue();
        venue.setVenueID(1);
        venue.setVenueName("Venue A");
        venue.setPrice(100);

        order = new Order();
        order.setOrderID(1001);
        order.setState(STATE_NO_AUDIT);
    }

    @Test
    void testFindById_Success() {
        when(orderDao.getOne(1001)).thenReturn(order);

        Order result = orderService.findById(1001);

        assertNotNull(result);
        assertEquals(1001, result.getOrderID());
    }

    @Test
    void testFindById_NotFound() {
        when(orderDao.getOne(1111)).thenThrow(EntityNotFoundException.class);

        assertThrows(EntityNotFoundException.class,
                () -> orderService.findById(1111));
    }

    @Test
    void testFindDateOrder_Success() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(4);

        Order o1 = new Order();
        o1.setOrderID(101);
        o1.setStartTime(start.plusHours(1));

        Order o2 = new Order();
        o2.setOrderID(102);
        o2.setStartTime(start.plusHours(2));

        Order o3 = new Order();
        o3.setOrderID(103);
        o3.setStartTime(start.plusHours(3));

        when(orderDao.findByVenueIDAndStartTimeIsBetween(1, start, end))
                .thenReturn(new ArrayList<>(Arrays.asList(o1, o2, o3)));

        List<Order> result = orderService.findDateOrder(1, start, end);

        assertNotNull(result);
        assertEquals(3, result.size());

        assertEquals(101, result.get(0).getOrderID());
        assertEquals(102, result.get(1).getOrderID());
        assertEquals(103, result.get(2).getOrderID());
    }

    @Test
    void testFindUserOrder_Success() {
        Order o1 = new Order();
        o1.setOrderID(101);
        o1.setUserID("U001");

        Order o2 = new Order();
        o2.setOrderID(102);
        o2.setUserID("U001");

        Order o3 = new Order();
        o3.setOrderID(103);
        o3.setUserID("U001");

        Page<Order> page = new PageImpl<>(
                new ArrayList<>(Arrays.asList(o1, o2, o3)),
                PageRequest.of(0, 10),
                3L
        );

        when(orderDao.findAllByUserID(eq("U001"), any(Pageable.class)))
                .thenReturn(page);

        Page<Order> result = orderService.findUserOrder(
                "U001", PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals(3, result.getTotalElements());

        List<Order> content = result.getContent();
        assertEquals(101, content.get(0).getOrderID());
        assertEquals(102, content.get(1).getOrderID());
        assertEquals(103, content.get(2).getOrderID());
    }

    @Test
    void testUpdateOrder_Success() {
        order.setState(STATE_WAIT);
        when(venueDao.findByVenueName("Venue A")).thenReturn(venue);
        when(orderDao.findByOrderID(1001)).thenReturn(order);
        when(orderDao.save(order)).thenReturn(order);

        int orderID = 1001;
        String venueName = "Venue A";
        LocalDateTime start = LocalDateTime.now();
        int hours = 2;
        String userId = "U001";
        assertDoesNotThrow(() ->
            orderService.updateOrder(orderID, venueName, start, hours, userId)
        );

        assertEquals(STATE_NO_AUDIT, order.getState());
        assertEquals(hours, order.getHours());
        assertEquals(venue.getVenueID(), order.getVenueID());
//        assertEquals(LocalDateTime.now(), order.getOrderTime());
        assertEquals(start, order.getStartTime());
        assertEquals(userId, order.getUserID());
        assertEquals(hours * venue.getPrice(), order.getTotal());

        verify(orderDao).save(order);
    }

    @Test
    void testUpdateOrder_VenueNotFound() {
        when(venueDao.findByVenueName("Venue B")).thenReturn(null);
        when(orderDao.findByOrderID(1001)).thenReturn(order);

        assertThrows(NullPointerException.class,
                () -> orderService.updateOrder(
                        1001, "Venue B", LocalDateTime.now(), 2, "U001"));
    }

    @Test
    void testUpdateOrder_OrderNotFound() {
        when(venueDao.findByVenueName("Venue A")).thenReturn(venue);
        when(orderDao.findByOrderID(1111)).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> orderService.updateOrder(
                        1111, "Venue A", LocalDateTime.now(), 2, "U001"));
    }

    @Test
    void testSubmit_Success() {
        when(venueDao.findByVenueName("Venue A")).thenReturn(venue);

        assertDoesNotThrow(() ->
                orderService.submit("Venue A", LocalDateTime.now(), 2, "U001")
        );

        verify(orderDao).save(any(Order.class));
    }

    @Test
    void testSubmit_VenueNotFound() {
        when(venueDao.findByVenueName("Venue B")).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> orderService.submit("Venue B", LocalDateTime.now(), 2, "U001"));
    }

    @Test
    void testDelOrder_Success() {
        doNothing().when(orderDao).deleteById(1001);

        assertDoesNotThrow(() -> orderService.delOrder(1001));

        verify(orderDao).deleteById(1001);
    }

    @Test
    void testDelById_NotFound() {
        doThrow(EmptyResultDataAccessException.class)
                .when(orderDao).deleteById(1111);

        assertThrows(EmptyResultDataAccessException.class,
                () -> orderService.delOrder(1111));
    }

    @Test
    void testConfirmOrder_Success() {
        when(orderDao.findByOrderID(1001)).thenReturn(order);

        assertDoesNotThrow(() -> orderService.confirmOrder(1001));

        verify(orderDao).updateState(STATE_WAIT, 1001);
    }

    @Test
    void testConfirmOrder_NotFound() {
        when(orderDao.findByOrderID(1111)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.confirmOrder(1111));

        assertEquals("订单不存在", ex.getMessage());
    }

    @Test
    void testFinishOrder_Success() {
        when(orderDao.findByOrderID(1001)).thenReturn(order);

        assertDoesNotThrow(() -> orderService.finishOrder(1001));

        verify(orderDao).updateState(STATE_FINISH, 1001);
    }

    @Test
    void testFinishOrder_NotFound() {
        when(orderDao.findByOrderID(1111)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.finishOrder(1111));

        assertEquals("订单不存在", ex.getMessage());
    }

    @Test
    void testRejectOrder_Success() {
        when(orderDao.findByOrderID(1001)).thenReturn(order);

        assertDoesNotThrow(() -> orderService.rejectOrder(1001));

        verify(orderDao).updateState(STATE_REJECT, 1001);
    }

    @Test
    void testRejectOrder_NotFound() {
        when(orderDao.findByOrderID(1111)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.rejectOrder(1111));

        assertEquals("订单不存在", ex.getMessage());
    }

    @Test
    void testFindNoAuditOrder_Success() {
        Order o1 = new Order();
        o1.setOrderID(201);
        o1.setState(STATE_NO_AUDIT);

        Order o2 = new Order();
        o2.setOrderID(202);
        o2.setState(STATE_NO_AUDIT);

        Order o3 = new Order();
        o3.setOrderID(203);
        o3.setState(STATE_NO_AUDIT);

        Page<Order> page = new PageImpl<>(
                new ArrayList<>(Arrays.asList(o1, o2, o3)),
                PageRequest.of(0, 10),
                3L
        );

        when(orderDao.findAllByState(
                eq(STATE_NO_AUDIT), any(Pageable.class)))
                .thenReturn(page);

        Page<Order> result =
                orderService.findNoAuditOrder(PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals(3, result.getTotalElements());

        List<Order> content = result.getContent();
        assertEquals(201, content.get(0).getOrderID());
        assertEquals(202, content.get(1).getOrderID());
        assertEquals(203, content.get(2).getOrderID());
    }

    @Test
    void testFindAuditOrder_Success() {
        Order o1 = new Order();
        o1.setOrderID(301);
        o1.setState(STATE_WAIT);

        Order o2 = new Order();
        o2.setOrderID(302);
        o2.setState(STATE_FINISH);

        Order o3 = new Order();
        o3.setOrderID(303);
        o3.setState(STATE_WAIT);

        when(orderDao.findAudit(STATE_WAIT, STATE_FINISH))
                .thenReturn(new ArrayList<>(Arrays.asList(o1, o2, o3)));

        List<Order> result = orderService.findAuditOrder();

        assertNotNull(result);
        assertEquals(3, result.size());

        assertEquals(301, result.get(0).getOrderID());
        assertEquals(302, result.get(1).getOrderID());
        assertEquals(303, result.get(2).getOrderID());
    }

}