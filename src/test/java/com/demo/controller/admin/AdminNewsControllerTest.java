package com.demo.controller.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * AdminNewsController 集成测试
 * 测试目标：验证新闻管理相关接口的判定覆盖和边界值处理
 * 技术：SpringBootTest + MockMvc + @Sql数据准备
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql(scripts = "/sql/admin_news_test_data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminNewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 测试：新闻管理页面加载
     * 语句覆盖：news_manage方法
     */
    @Test
    void newsManage_shouldLoadPage() throws Exception {
        mockMvc.perform(get("/news_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/news_manage"));
    }

    /**
     * 测试：添加新闻页面加载
     * 语句覆盖：news_add方法
     */
    @Test
    void newsAdd_shouldReturnAddPage() throws Exception {
        mockMvc.perform(get("/news_add"))
                .andExpect(status().isOk());
    }

    /**
     * 测试：新闻编辑页面加载 - 有效新闻ID
     * 语句覆盖：news_edit方法
     */
    @Test
    void newsEdit_shouldLoadPageWithNews() throws Exception {
        mockMvc.perform(get("/news_edit").param("newsID", "3001"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/news_edit"));
    }

    /**
     * 测试：新闻编辑页面 - 新闻不存在（记录实际结果）
     * 等价类：无效newsID，不存在
     * 注：Service可能返回null，记录实际响应
     */
    @Test
    void newsEdit_shouldRecordActual_whenNewsNotExists() {
        try {
            mockMvc.perform(get("/news_edit").param("newsID", "99999"));
            TestRecordUtil.recordSuccess("newsEdit(newsID=99999)");
        } catch (Exception e) {
            TestRecordUtil.recordException("newsEdit(newsID=99999)", e);
        }
    }

    /**
     * 测试：获取新闻列表 - 有效页码
     * 等价类：有效page（page>=1）
     */
    @Test
    void newsList_shouldReturnList_whenValidPage() throws Exception {
        mockMvc.perform(get("/newsList.do")
                        .param("page", "1"))
                .andExpect(status().isOk());
    }

    /**
     * 测试：边界值 - page=0（记录实际结果）
     * 边界值：page下边界
     * 注：记录实际响应，不做异常断言
     */
    @Test
    void newsList_shouldRecordActual_whenZeroPage() {
        try {
            mockMvc.perform(get("/newsList.do").param("page", "0"));
            TestRecordUtil.recordSuccess("newsList(page=0)");
        } catch (Exception e) {
            TestRecordUtil.recordException("newsList(page=0)", e);
        }
    }

    /**
     * 测试：边界值 - page为负数（记录实际结果）
     * 边界值：负数页码
     * 注：记录实际响应，不做异常断言
     */
    @Test
    void newsList_shouldRecordActual_whenNegativePage() {
        try {
            mockMvc.perform(get("/newsList.do").param("page", "-1"));
            TestRecordUtil.recordSuccess("newsList(page=-1)");
        } catch (Exception e) {
            TestRecordUtil.recordException("newsList(page=-1)", e);
        }
    }

    /**
     * 测试：删除新闻 - 有效新闻存在
     * 等价类：有效newsID，存在
     */
    @Test
    void delNews_shouldReturnTrue_whenNewsExists() throws Exception {
        mockMvc.perform(post("/delNews.do")
                        .param("newsID", "3002"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    /**
     * 测试：删除新闻 - 新闻不存在（记录实际结果）
     * 等价类：无效newsID，不存在
     * 注：Service可能抛出异常，记录实际响应状态
     */
    @Test
    void delNews_shouldRecordActual_whenNewsNotExists() {
        try {
            mockMvc.perform(post("/delNews.do").param("newsID", "99999"));
            TestRecordUtil.recordSuccess("delNews(99999)");
        } catch (Exception e) {
            TestRecordUtil.recordException("delNews(99999)", e);
        }
    }

    /**
     * 测试：修改新闻 - 有效新闻存在
     * 等价类：有效newsID，存在
     */
    @Test
    void modifyNews_shouldRedirect_whenNewsExists() throws Exception {
        mockMvc.perform(post("/modifyNews.do")
                        .param("newsID", "3003")
                        .param("title", "Updated Title")
                        .param("content", "Updated Content"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("news_manage"));
    }

    /**
     * 测试：修改新闻 - 新闻不存在（记录实际结果）
     * 等价类：无效newsID，不存在
     * 注：Service可能返回null，导致NPE，记录实际响应
     */
    @Test
    void modifyNews_shouldRecordActual_whenNewsNotExists() {
        try {
            mockMvc.perform(post("/modifyNews.do")
                            .param("newsID", "99999")
                            .param("title", "Title")
                            .param("content", "Content"));
            TestRecordUtil.recordSuccess("modifyNews(99999)");
        } catch (Exception e) {
            TestRecordUtil.recordException("modifyNews(99999)", e);
        }
    }

    /**
     * 测试：添加新闻 - 正常参数
     * 语句覆盖：addNews方法全路径
     */
    @Test
    void addNews_shouldRedirectToManage() throws Exception {
        mockMvc.perform(post("/addNews.do")
                        .param("title", "New News")
                        .param("content", "News Content"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("news_manage"));
    }

    /**
     * 测试：边界值 - 极大页码
     * 边界值分析：page极大值
     */
    @Test
    void newsList_shouldHandleLargePage() throws Exception {
        mockMvc.perform(get("/newsList.do")
                        .param("page", "999999"))
                .andExpect(status().isOk());
    }

    /**
     * 测试：边界值 - 空参数（风险证据）
     * 等价类：空字符串参数
     * 注：记录实际响应，不做异常断言
     */
    @Test
    void addNews_shouldRecordActual_whenEmptyParams() {
        try {
            mockMvc.perform(post("/addNews.do")
                            .param("title", "")
                            .param("content", ""));
            TestRecordUtil.recordSuccess("addNews(空参数)");
        } catch (Exception e) {
            TestRecordUtil.recordException("addNews(空参数)", e);
        }
    }
}
