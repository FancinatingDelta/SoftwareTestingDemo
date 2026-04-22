package com.demo.controller.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminOrderController 集成测试
 * 测试目标：验证订单审核相关接口的判定覆盖和边界值处理
 * 技术：SpringBootTest + MockMvc + @Sql数据准备
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql(scripts = "/sql/admin_order_test_data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 测试：通过订单 - 有效订单存在且状态为待审核（判定：true分支）
     * 等价类：有效orderID，存在且state=1
     */
    @Test
    void passOrder_shouldReturnTrue_whenOrderExistsAndPending() throws Exception {
        mockMvc.perform(post("/passOrder.do")
                        .param("orderID", "1001"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    /**
     * 测试：拒绝订单 - 有效订单存在且状态为待审核（判定：true分支）
     * 等价类：有效orderID，存在且state=1
     */
    @Test
    void rejectOrder_shouldReturnTrue_whenOrderExistsAndPending() throws Exception {
        mockMvc.perform(post("/rejectOrder.do")
                        .param("orderID", "1001"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    /**
     * 测试：通过订单 - 订单不存在（判定：false分支，触发异常）
     * 等价类：无效orderID，不存在
     * 边界值：极大值99999
     * 注：Service抛出RuntimeException，但Controller无异常处理，验证异常抛出
     */
    @Test
    void passOrder_shouldThrowException_whenOrderNotExists() throws Exception {
        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/passOrder.do").param("orderID", "99999"));
        });
    }

    /**
     * 测试：拒绝订单 - 订单不存在（判定：false分支，触发异常）
     * 等价类：无效orderID，不存在
     * 注：Service抛出RuntimeException，验证异常抛出
     */
    @Test
    void rejectOrder_shouldThrowException_whenOrderNotExists() throws Exception {
        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/rejectOrder.do").param("orderID", "99999"));
        });
    }

    /**
     * 测试：通过订单 - 订单已审核（Service当前不检查状态，仍返回true）
     * 等价类：存在但state!=1（state=2已审核）
     * 注：当前Service实现不校验状态，故返回200而非500
     */
    @Test
    void passOrder_shouldReturnTrue_whenOrderAlreadyAudited() throws Exception {
        mockMvc.perform(post("/passOrder.do")
                        .param("orderID", "1002"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    /**
     * 测试：边界值 - orderID=0（边界值分析）
     * 边界值：最小值（整数边界）
     * 注：Service抛出RuntimeException（订单不存在），验证异常抛出
     */
    @Test
    void passOrder_shouldThrowException_whenZeroOrderId() throws Exception {
        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/passOrder.do").param("orderID", "0"));
        });
    }

    /**
     * 测试：边界值 - orderID为负数（无效等价类）
     * 边界值：负整数
     * 注：Service抛出RuntimeException（订单不存在），验证异常抛出
     */
    @Test
    void passOrder_shouldThrowException_whenNegativeOrderId() throws Exception {
        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/passOrder.do").param("orderID", "-1"));
        });
    }

    /**
     * 测试：获取未审核订单列表 - 有效页码
     * 等价类：有效page（page>=1）
     */
    @Test
    void getOrderList_shouldReturnList_whenValidPage() throws Exception {
        mockMvc.perform(get("/admin/getOrderList.do")
                        .param("page", "1"))
                .andExpect(status().isOk());
    }

    /**
     * 测试：边界值 - page=0（会导致负数页码，触发异常）
     * 边界值：page下边界（因Controller做page-1，-1不被Pageable接受）
     * 注：Pageable抛出IllegalArgumentException，验证异常抛出
     */
    @Test
    void getOrderList_shouldThrowException_whenZeroPage() throws Exception {
        assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/admin/getOrderList.do").param("page", "0"));
        });
    }

    /**
     * 测试：边界值 - page为负数
     * 边界值：负数页码
     * 注：Pageable抛出IllegalArgumentException，验证异常抛出
     */
    @Test
    void getOrderList_shouldThrowException_whenNegativePage() throws Exception {
        assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/admin/getOrderList.do").param("page", "-1"));
        });
    }

    /**
     * 测试：边界值 - page极大值
     * 边界值：极大页码
     */
    @Test
    void getOrderList_shouldHandleLargePage() throws Exception {
        mockMvc.perform(get("/admin/getOrderList.do")
                        .param("page", "999999"))
                .andExpect(status().isOk());
    }

    /**
     * 测试：管理员预约管理页面加载
     * 语句覆盖：reservation_manage方法全路径
     */
    @Test
    void reservationManage_shouldLoadPage() throws Exception {
        mockMvc.perform(get("/reservation_manage"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"));
    }
}
