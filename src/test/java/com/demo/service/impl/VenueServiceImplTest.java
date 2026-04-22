package com.demo.service.impl;

import com.demo.dao.VenueDao;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VenueServiceImplTest {

    @Mock
    private VenueDao venueDao;

    @InjectMocks
    private VenueServiceImpl venueService;

    private Venue venue;

    @BeforeEach
    void setUp() {
        venue = new Venue();
        venue.setVenueID(1);
        venue.setVenueName("Test Venue");
        venue.setPrice(100);
        venue.setAddress("Test Address");
    }

    @Test
    void testFindByVenueID_Success() {
        when(venueDao.getOne(1)).thenReturn(venue);

        Venue result = venueService.findByVenueID(1);

        assertNotNull(result);
        assertEquals("Test Venue", result.getVenueName());
    }

    @Test
    void testFindByVenueID_NotFound() {
        when(venueDao.getOne(999)).thenThrow(EntityNotFoundException.class);

        assertThrows(EntityNotFoundException.class,
                () -> venueService.findByVenueID(999));
    }

    @Test
    void testFindByVenueName_Success() {
        when(venueDao.findByVenueName("Test Venue")).thenReturn(venue);

        Venue result = venueService.findByVenueName("Test Venue");

        assertNotNull(result);
        assertEquals(1, result.getVenueID());
    }

    @Test
    void testFindByVenueName_NotFound() {
        when(venueDao.findByVenueName("Venue Not Exist")).thenReturn(null);

        assertNull(venueService.findByVenueName("Venue Not Exist"));
    }

    @Test
    void testFindAll_Pageable_Success() {
        Venue v1 = new Venue();
        v1.setVenueID(1);
        v1.setVenueName("Venue A");

        Venue v2 = new Venue();
        v2.setVenueID(2);
        v2.setVenueName("Venue B");

        Venue v3 = new Venue();
        v3.setVenueID(3);
        v3.setVenueName("Venue C");

        Page<Venue> page = new PageImpl<>(List.of(v1, v2, v3),
                PageRequest.of(0, 10), 3);

        when(venueDao.findAll(any(Pageable.class))).thenReturn(page);

        Page<Venue> result = venueService.findAll(PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getTotalPages());

        List<Venue> content = result.getContent();
        assertEquals("Venue A", content.get(0).getVenueName());
        assertEquals("Venue B", content.get(1).getVenueName());
        assertEquals("Venue C", content.get(2).getVenueName());
    }

    @Test
    void testFindAll_List_Success() {
        Venue v1 = new Venue();
        v1.setVenueID(1);
        v1.setVenueName("Venue A");

        Venue v2 = new Venue();
        v2.setVenueID(2);
        v2.setVenueName("Venue B");

        when(venueDao.findAll()).thenReturn(List.of(v1, v2));

        List<Venue> result = venueService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Venue A", result.get(0).getVenueName());
        assertEquals("Venue B", result.get(1).getVenueName());
    }

    @Test
    void testCreate_Success() {
        when(venueDao.save(venue)).thenReturn(venue);

        int id = venueService.create(venue);

        assertEquals(1, id);
    }

    @Test
    void testCreate_Fail_NullObject() {
        assertThrows(NullPointerException.class,
                () -> venueService.create(null));
    }

    @Test
    void testUpdate_Success() {
        when(venueDao.save(venue)).thenReturn(venue);

        assertDoesNotThrow(() -> venueService.update(venue));
        verify(venueDao).save(venue);
    }

    @Test
    void testDelById_Success() {
        doNothing().when(venueDao).deleteById(1);

        assertDoesNotThrow(() -> venueService.delById(1));
        verify(venueDao).deleteById(1);
    }

    @Test
    void testDelById_NotFound() {
        doThrow(EmptyResultDataAccessException.class)
                .when(venueDao).deleteById(999);

        assertThrows(EmptyResultDataAccessException.class,
                () -> venueService.delById(999));
    }

    @Test
    void testCountVenueName_Success() {
        when(venueDao.countByVenueName("Test Venue")).thenReturn(1);

        int count = venueService.countVenueName("Test Venue");

        assertEquals(1, count);
    }

    @Test
    void testCountVenueName_NoMatch() {
        when(venueDao.countByVenueName("Venue Not Exist")).thenReturn(0);

        int count = venueService.countVenueName("Venue Not Exist");

        assertEquals(0, count);
    }
}