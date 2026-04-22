package com.demo.controller.admin;

import com.demo.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminUserController 集成测试
 * 测试目标：验证用户管理CRUD功能 + 多接口时序组合
 * 技术：SpringBootTest + MockMvc + @Sql数据准备 + 时序依赖测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 测试：用户管理页面加载
     * 语句覆盖：user_manage方法
     */
    @Test
    void userManage_shouldLoadPage() throws Exception {
        mockMvc.perform(get("/user_manage"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"));
    }

    /**
     * 测试：添加用户页面加载
     * 语句覆盖：user_add方法
     */
    @Test
    void userAdd_shouldReturnAddPage() throws Exception {
        mockMvc.perform(get("/user_add"))
                .andExpect(status().isOk());
    }

    /**
     * 测试：检查用户名 - 用户名可用（true分支）
     * 等价类：不存在userID
     * 时序：应在添加用户前调用
     */
    @Test
    void checkUserID_shouldReturnTrue_whenUserNotExists() throws Exception {
        mockMvc.perform(post("/checkUserID.do")
                        .param("userID", "newuser999"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    /**
     * 测试：检查用户名 - 用户名已存在（false分支）
     * 等价类：已存在userID
     */
    @Test
    @Sql(statements = {
        "DELETE FROM user WHERE id = 9999",
        "INSERT INTO user (id, userID, password, isadmin, user_name) VALUES (9999, 'existuser', 'pwd', 0, 'Exist')"
    })
    void checkUserID_shouldReturnFalse_whenUserExists() throws Exception {
        mockMvc.perform(post("/checkUserID.do")
                        .param("userID", "existuser"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    /**
     * 测试：多接口时序组合 - 完整用户生命周期
     * 时序：检查用户名 → 添加用户 → 查询列表验证 → 修改用户 → 删除用户
     */
    @Test
    void userLifecycle_shouldCompleteCrudSequence() throws Exception {
        String userID = "testuser_" + System.currentTimeMillis();
        String updatedUserID = "updated_" + System.currentTimeMillis();

        // 步骤1: 检查用户名可用（时序依赖起点）
        mockMvc.perform(post("/checkUserID.do")
                        .param("userID", userID))
                .andExpect(content().string("true"));

        // 步骤2: 添加用户（Post请求，重定向验证）
        mockMvc.perform(post("/addUser.do")
                        .param("userID", userID)
                        .param("userName", "LifecycleUser")
                        .param("password", "password123")
                        .param("email", "lifecycle@test.com")
                        .param("phone", "13800138000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_manage"));

        // 步骤3: 查询列表验证用户存在（时序依赖：必须在添加之后）
        // 遍历分页查询直到找到新用户（升序排列时新用户可能在后面页）
        int createdUserId = -1;
        for (int page = 1; page <= 100 && createdUserId == -1; page++) {
            MvcResult result = mockMvc.perform(get("/userList.do").param("page", String.valueOf(page)))
                    .andExpect(status().isOk())
                    .andReturn();
            String responseBody = result.getResponse().getContentAsString();
            User[] users = objectMapper.readValue(responseBody, User[].class);
            if (users.length == 0) break; // 无更多数据
            for (User u : users) {
                if (userID.equals(u.getUserID())) {
                    createdUserId = u.getId();
                    break;
                }
            }
        }
        assertTrue(createdUserId > 0, "新建用户应在列表中存在");

        // 步骤4: 修改用户信息（时序依赖：需要获取到用户id）
        mockMvc.perform(post("/modifyUser.do")
                        .param("userID", updatedUserID)
                        .param("oldUserID", userID)
                        .param("userName", "UpdatedName")
                        .param("password", "newpassword")
                        .param("email", "updated@test.com")
                        .param("phone", "13900139000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_manage"));

        // 步骤5: 删除用户（清理，时序依赖：必须在确认用户存在后）
        mockMvc.perform(post("/delUser.do")
                        .param("id", String.valueOf(createdUserId)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    /**
     * 测试：添加用户 - 边界值 - 空参数（风险证据）
     * 等价类：空字符串参数
     */
    @Test
    void addUser_shouldHandleEmptyParams() throws Exception {
        mockMvc.perform(post("/addUser.do")
                        .param("userID", "")
                        .param("userName", "")
                        .param("password", "")
                        .param("email", "")
                        .param("phone", ""))
                .andExpect(status().is3xxRedirection());
    }

    /**
     * 测试：删除用户 - 边界值 - 不存在的ID
     * 等价类：无效id
     * 注：Service抛出EmptyResultDataAccessException，验证异常抛出
     */
    @Test
    void delUser_shouldThrowException_whenNonExistentId() throws Exception {
        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/delUser.do").param("id", "99999"));
        });
    }

    /**
     * 测试：用户编辑页面加载
     * 语句覆盖：user_edit方法
     */
    @Test
    @Sql(statements = {
        "DELETE FROM user WHERE id = 8888",
        "INSERT INTO user (id, userID, password, isadmin, user_name) VALUES (8888, 'edituser', 'pwd', 0, 'EditUser')"
    })
    void userEdit_shouldLoadPageWithUser() throws Exception {
        mockMvc.perform(get("/user_edit").param("id", "8888"))
                .andExpect(status().isOk());
    }

    /**
     * 测试：分页查询 - 边界值 - 极大页码
     * 边界值分析：page极大值
     */
    @Test
    void userList_shouldHandleLargePageNumber() throws Exception {
        mockMvc.perform(get("/userList.do").param("page", "999999"))
                .andExpect(status().isOk());
    }

    /**
     * 测试：分页查询 - 边界值 - page=0
     * 边界值：page下边界（因Controller做page-1，-1不被Pageable接受）
     * 注：Pageable抛出IllegalArgumentException，验证异常抛出
     */
    @Test
    void userList_shouldThrowException_whenZeroPage() throws Exception {
        assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/userList.do").param("page", "0"));
        });
    }

    /**
     * 测试：分页查询 - 边界值 - 负数页码
     * 注：Pageable抛出IllegalArgumentException，验证异常抛出
     */
    @Test
    void userList_shouldThrowException_whenNegativePage() throws Exception {
        assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/userList.do").param("page", "-1"));
        });
    }
}
