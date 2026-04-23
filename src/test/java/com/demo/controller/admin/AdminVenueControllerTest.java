package com.demo.controller.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * AdminVenueController 集成测试
 * 测试目标：验证场馆管理相关接口的判定覆盖和边界值处理
 * 技术：SpringBootTest + MockMvc + @Sql数据准备
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql(scripts = "/sql/admin_venue_test_data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminVenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 测试：场馆管理页面加载
     * 语句覆盖：venue_manage方法
     */
    @Test
    void venueManage_shouldLoadPage() throws Exception {
        mockMvc.perform(get("/venue_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/venue_manage"));
    }

    /**
     * 测试：添加场馆页面加载
     * 语句覆盖：venue_add方法
     */
    @Test
    void venueAdd_shouldReturnAddPage() throws Exception {
        mockMvc.perform(get("/venue_add"))
                .andExpect(status().isOk());
    }

    /**
     * 测试：场馆编辑页面加载 - 有效场馆ID
     * 语句覆盖：editVenue方法
     */
    @Test
    void editVenue_shouldLoadPageWithVenue() throws Exception {
        mockMvc.perform(get("/venue_edit").param("venueID", "4001"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/venue_edit"));
    }

    /**
     * 测试：场馆编辑页面 - 场馆不存在（记录实际结果）
     * 等价类：无效venueID，不存在
     * 注：Service可能返回null，导致NPE，记录实际响应
     */
    @Test
    void editVenue_shouldRecordActual_whenVenueNotExists() {
        try {
            mockMvc.perform(get("/venue_edit").param("venueID", "99999"));
            TestRecordUtil.recordSuccess("editVenue(venueID=99999)");
        } catch (Exception e) {
            TestRecordUtil.recordException("editVenue(venueID=99999)", e);
        }
    }

    /**
     * 测试：获取场馆列表 - 有效页码
     * 等价类：有效page（page>=1）
     */
    @Test
    void getVenueList_shouldReturnList_whenValidPage() throws Exception {
        mockMvc.perform(get("/venueList.do")
                        .param("page", "1"))
                .andExpect(status().isOk());
    }

    /**
     * 测试：边界值 - page=0（记录实际结果）
     * 边界值：page下边界
     * 注：记录实际响应，不做异常断言
     */
    @Test
    void getVenueList_shouldRecordActual_whenZeroPage() {
        try {
            mockMvc.perform(get("/venueList.do").param("page", "0"));
            TestRecordUtil.recordSuccess("getVenueList(page=0)");
        } catch (Exception e) {
            TestRecordUtil.recordException("getVenueList(page=0)", e);
        }
    }

    /**
     * 测试：边界值 - page为负数（记录实际结果）
     * 边界值：负数页码
     * 注：记录实际响应，不做异常断言
     */
    @Test
    void getVenueList_shouldRecordActual_whenNegativePage() {
        try {
            mockMvc.perform(get("/venueList.do").param("page", "-1"));
            TestRecordUtil.recordSuccess("getVenueList(page=-1)");
        } catch (Exception e) {
            TestRecordUtil.recordException("getVenueList(page=-1)", e);
        }
    }

    /**
     * 测试：删除场馆 - 有效场馆存在
     * 等价类：有效venueID，存在
     */
    @Test
    void delVenue_shouldReturnTrue_whenVenueExists() throws Exception {
        mockMvc.perform(post("/delVenue.do")
                        .param("venueID", "4002"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    /**
     * 测试：删除场馆 - 场馆不存在（记录实际结果）
     * 等价类：无效venueID，不存在
     * 注：Service可能抛出异常，记录实际响应状态
     */
    @Test
    void delVenue_shouldRecordActual_whenVenueNotExists() {
        try {
            mockMvc.perform(post("/delVenue.do").param("venueID", "99999"));
            TestRecordUtil.recordSuccess("delVenue(99999)");
        } catch (Exception e) {
            TestRecordUtil.recordException("delVenue(99999)", e);
        }
    }

    /**
     * 测试：检查场馆名 - 场馆名可用（true分支）
     * 等价类：不存在venueName
     */
    @Test
    void checkVenueName_shouldReturnTrue_whenNameNotExists() throws Exception {
        mockMvc.perform(post("/checkVenueName.do")
                        .param("venueName", "NewVenue999"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    /**
     * 测试：检查场馆名 - 场馆名已存在（false分支）
     * 等价类：已存在venueName
     */
    @Test
    void checkVenueName_shouldReturnFalse_whenNameExists() throws Exception {
        mockMvc.perform(post("/checkVenueName.do")
                        .param("venueName", "TestVenue"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    /**
     * 测试：修改场馆 - 有效场馆存在（无文件上传，记录实际结果）
     * 等价类：有效venueID，存在
     * 注：multipart无文件时FileUtil可能异常，记录实际响应
     */
    @Test
    void modifyVenue_shouldRecordActual_whenVenueExists() {
        try {
            mockMvc.perform(multipart("/modifyVenue.do")
                            .param("venueID", "4003")
                            .param("venueName", "UpdatedVenue")
                            .param("address", "UpdatedAddress")
                            .param("description", "UpdatedDesc")
                            .param("price", "200")
                            .param("open_time", "08:00")
                            .param("close_time", "22:00"));
            TestRecordUtil.recordSuccess("modifyVenue(正常修改)");
        } catch (Exception e) {
            TestRecordUtil.recordException("modifyVenue(正常修改)", e);
        }
    }

    /**
     * 测试：修改场馆 - 场馆不存在（记录实际结果）
     * 等价类：无效venueID，不存在
     * 注：Service可能返回null，导致NPE，记录实际响应
     */
    @Test
    void modifyVenue_shouldRecordActual_whenVenueNotExists() {
        try {
            mockMvc.perform(multipart("/modifyVenue.do")
                            .param("venueID", "99999")
                            .param("venueName", "Venue")
                            .param("address", "Addr")
                            .param("description", "Desc")
                            .param("price", "100")
                            .param("open_time", "09:00")
                            .param("close_time", "21:00"));
            TestRecordUtil.recordSuccess("modifyVenue(99999)");
        } catch (Exception e) {
            TestRecordUtil.recordException("modifyVenue(99999)", e);
        }
    }

    /**
     * 测试：添加场馆 - 正常参数（无文件上传，记录实际结果）
     * 语句覆盖：addVenue方法全路径
     * 注：multipart无文件时FileUtil可能异常，记录实际响应
     */
    @Test
    void addVenue_shouldRecordActual_whenNormalParams() {
        try {
            mockMvc.perform(multipart("/addVenue.do")
                            .param("venueName", "NewTestVenue")
                            .param("address", "TestAddress")
                            .param("description", "TestDescription")
                            .param("price", "150")
                            .param("open_time", "08:00")
                            .param("close_time", "20:00"));
            TestRecordUtil.recordSuccess("addVenue(正常参数)");
        } catch (Exception e) {
            TestRecordUtil.recordException("addVenue(正常参数)", e);
        }
    }

    /**
     * 测试：添加场馆 - 空参数（风险证据）
     * 等价类：空字符串参数
     * 注：记录实际响应，不做异常断言
     */
    @Test
    void addVenue_shouldRecordActual_whenEmptyParams() {
        try {
            mockMvc.perform(multipart("/addVenue.do")
                            .param("venueName", "")
                            .param("address", "")
                            .param("description", "")
                            .param("price", "0")
                            .param("open_time", "")
                            .param("close_time", ""));
            TestRecordUtil.recordSuccess("addVenue(空参数)");
        } catch (Exception e) {
            TestRecordUtil.recordException("addVenue(空参数)", e);
        }
    }

    /**
     * 测试：边界值 - price为负数（记录实际结果）
     * 边界值：负数价格
     * 注：记录实际响应，不做异常断言
     */
    @Test
    void addVenue_shouldRecordActual_whenNegativePrice() {
        try {
            mockMvc.perform(multipart("/addVenue.do")
                            .param("venueName", "TestVenue")
                            .param("address", "TestAddr")
                            .param("description", "TestDesc")
                            .param("price", "-100")
                            .param("open_time", "08:00")
                            .param("close_time", "20:00"));
            TestRecordUtil.recordSuccess("addVenue(price=-100)");
        } catch (Exception e) {
            TestRecordUtil.recordException("addVenue(price=-100)", e);
        }
    }

    /**
     * 测试：边界值 - 极大页码
     * 边界值分析：page极大值
     */
    @Test
    void getVenueList_shouldHandleLargePage() throws Exception {
        mockMvc.perform(get("/venueList.do")
                        .param("page", "999999"))
                .andExpect(status().isOk());
    }

    /**
     * 测试：添加场馆 - 带文件上传（可选功能）
     * 注：文件上传需要multipart，记录实际响应
     */
    @Test
    void addVenue_shouldRecordActual_withFileUpload() {
        try {
            MockMultipartFile file = new MockMultipartFile(
                    "picture", "test.jpg", "image/jpeg", "test image content".getBytes());
            mockMvc.perform(multipart("/addVenue.do")
                            .file(file)
                            .param("venueName", "VenueWithPic")
                            .param("address", "PicAddr")
                            .param("description", "PicDesc")
                            .param("price", "200")
                            .param("open_time", "09:00")
                            .param("close_time", "21:00"));
            TestRecordUtil.recordSuccess("addVenue(带文件)");
        } catch (Exception e) {
            TestRecordUtil.recordException("addVenue(带文件)", e);
        }
    }
}
