package com.demo.service.impl;

import com.demo.dao.NewsDao;
import com.demo.entity.News;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsServiceImplTest {

    @Mock
    private NewsDao newsDao;

    @InjectMocks
    private NewsServiceImpl newsService;

    private News news;

    @BeforeEach
    void setUp() {
        news = new News();
        news.setNewsID(1);
        news.setTitle("Title 1");
        news.setContent("Content 1");
    }

    @Test
    void testFindAll_Success() {
        News news1 = new News();
        news1.setNewsID(1);
        news1.setTitle("Title 1");

        News news2 = new News();
        news2.setNewsID(2);
        news2.setTitle("Title 2");

        Page<News> page = new PageImpl<>(List.of(news1, news2));
        when(newsDao.findAll(any(Pageable.class))).thenReturn(page);

        Page<News> result = newsService.findAll(PageRequest.of(0, 10));

        assertNotNull(result);
        List<News> content = result.getContent();

        assertEquals(2, content.size());

        News first = content.get(0);
        assertEquals(1, first.getNewsID());
        assertEquals("Title 1", first.getTitle());

        News second = content.get(1);
        assertEquals(2, second.getNewsID());
        assertEquals("Title 2", second.getTitle());
    }

    @Test
    void testFindById_Success() {
        when(newsDao.getOne(1)).thenReturn(news);

        News result = newsService.findById(1);

        assertNotNull(result);
        assertEquals("Title 1", result.getTitle());
        assertEquals("Content 1", result.getContent());
    }

    @Test
    void testFindById_NotFound() {
        when(newsDao.getOne(999)).thenThrow(EntityNotFoundException.class);

        assertThrows(EntityNotFoundException.class,
                () -> newsService.findById(999));
    }

    @Test
    void testCreate_Success() {
        when(newsDao.save(news)).thenReturn(news);

        int id = newsService.create(news);

        assertEquals(1, id);
    }

    @Test
    void testCreate_Fail_NullObject() {
        assertThrows(NullPointerException.class,
                () -> newsService.create(null));
    }

    @Test
    void testUpdate_Success() {
        when(newsDao.save(news)).thenReturn(news);

        assertDoesNotThrow(() -> newsService.update(news));
        verify(newsDao).save(news);
    }

    @Test
    void testDelById_Success() {
        doNothing().when(newsDao).deleteById(1);

        assertDoesNotThrow(() -> newsService.delById(1));
        verify(newsDao).deleteById(1);
    }

    @Test
    void testDelById_NotFound() {
        doThrow(new EmptyResultDataAccessException(1))
                .when(newsDao).deleteById(999);

        assertThrows(EmptyResultDataAccessException.class,
                () -> newsService.delById(999));
    }
}