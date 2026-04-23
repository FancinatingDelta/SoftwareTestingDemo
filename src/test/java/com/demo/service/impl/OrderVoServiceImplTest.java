package com.demo.service.impl;

import com.demo.dao.OrderDao;
import com.demo.dao.VenueDao;
import com.demo.entity.Order;
import com.demo.entity.Venue;
import com.demo.entity.vo.OrderVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.demo.service.OrderService.STATE_NO_AUDIT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderVoServiceImplTest {

    @Mock
    private OrderDao orderDao;

    @Mock
    private VenueDao venueDao;

    @InjectMocks
    private OrderVoServiceImpl orderVoService;

    private Order order;
    private Venue venue;

    @BeforeEach
    void setUp() {
        venue = new Venue();
        venue.setVenueID(1);
        venue.setVenueName("Venue A");

        order = new Order();
        order.setOrderID(1001);
        order.setUserID("U001");
        order.setVenueID(1);
        order.setState(STATE_NO_AUDIT);
        order.setOrderTime(LocalDateTime.now());
        order.setStartTime(LocalDateTime.now().plusHours(1));
        order.setHours(2);
        order.setTotal(200);
    }

    @Test
    void testReturnOrderVoByOrderID_Success() {
        when(orderDao.findByOrderID(1001)).thenReturn(order);
        when(venueDao.findByVenueID(1)).thenReturn(venue);

        OrderVo vo = orderVoService.returnOrderVoByOrderID(1001);

        assertNotNull(vo);
        assertEquals(1001, vo.getOrderID());
        assertEquals("U001", vo.getUserID());
        assertEquals("Venue A", vo.getVenueName());
        assertEquals(STATE_NO_AUDIT, vo.getState());
    }

    @Test
    void testReturnOrderVoByOrderID_OrderNotFound() {
        when(orderDao.findByOrderID(999)).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> orderVoService.returnOrderVoByOrderID(999));
    }

    @Test
    void testReturnOrderVoByOrderID_VenueNotFound() {
        when(orderDao.findByOrderID(1001)).thenReturn(order);
        when(venueDao.findByVenueID(1)).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> orderVoService.returnOrderVoByOrderID(1001));
    }

    @Test
    void testReturnVo_Success() {
        Order order1 = new Order();
        order1.setOrderID(1001);
        order1.setVenueID(1);

        Order order2 = new Order();
        order2.setOrderID(1002);
        order2.setVenueID(1);

        List<Order> orders = Arrays.asList(order1, order2);

        when(orderDao.findByOrderID(1001)).thenReturn(order1);
        when(orderDao.findByOrderID(1002)).thenReturn(order2);
        when(venueDao.findByVenueID(1)).thenReturn(venue);

        List<OrderVo> vos = orderVoService.returnVo(orders);

        assertEquals(2, vos.size());
        assertEquals(1001, vos.get(0).getOrderID());
        assertEquals(1002, vos.get(1).getOrderID());
        assertEquals("Venue A", vos.get(0).getVenueName());
    }

}