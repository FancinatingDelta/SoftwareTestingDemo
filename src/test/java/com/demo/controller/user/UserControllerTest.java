package com.demo.controller.user;

import com.demo.entity.User;
import com.demo.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.util.NestedServletException;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void signupPage_shouldReturnSignupView() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"));
    }

    @Test
    void loginPage_shouldReturnLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void userInfoPage_shouldReturnUserInfoView() throws Exception {
        mockMvc.perform(get("/user_info"))
                .andExpect(status().isOk())
                .andExpect(view().name("user_info"));
    }

    @Test
    void loginCheck_shouldSetUserSession_andReturnIndexPath_whenNormalUser() throws Exception {
        User normalUser = new User();
        normalUser.setUserID("u001");
        normalUser.setIsadmin(0);

        when(userService.checkLogin(eq("u001"), eq("pw"))).thenReturn(normalUser);

        mockMvc.perform(post("/loginCheck.do")
                        .param("userID", "u001")
                        .param("password", "pw"))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("/index")))
                .andExpect(request().sessionAttribute("user", normalUser));
    }

    @Test
    void loginCheck_shouldSetAdminSession_andReturnAdminIndexPath_whenAdminUser() throws Exception {
        User adminUser = new User();
        adminUser.setUserID("admin01");
        adminUser.setIsadmin(1);

        when(userService.checkLogin(eq("admin01"), eq("pw"))).thenReturn(adminUser);

        mockMvc.perform(post("/loginCheck.do")
                        .param("userID", "admin01")
                        .param("password", "pw"))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("/admin_index")))
                .andExpect(request().sessionAttribute("admin", adminUser));
    }

    @Test
    void loginCheck_shouldReturnFalse_whenServiceReturnsNull() throws Exception {
        when(userService.checkLogin(eq("u001"), eq("wrong"))).thenReturn(null);

        mockMvc.perform(post("/loginCheck.do")
                        .param("userID", "u001")
                        .param("password", "wrong"))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("false")))
                .andExpect(request().sessionAttributeDoesNotExist("user"))
                .andExpect(request().sessionAttributeDoesNotExist("admin"));
    }

    @Test
    void checkPassword_shouldReturnTrue_whenPasswordMatches() throws Exception {
        User user = new User();
        user.setUserID("u001");
        user.setPassword("pw");
        when(userService.findByUserID(eq("u001"))).thenReturn(user);

        mockMvc.perform(get("/checkPassword.do")
                        .param("userID", "u001")
                        .param("password", "pw"))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("true")));
    }

    @Test
    void checkPassword_shouldReturnFalse_whenPasswordDoesNotMatch() throws Exception {
        User user = new User();
        user.setUserID("u001");
        user.setPassword("pw");
        when(userService.findByUserID(eq("u001"))).thenReturn(user);

        mockMvc.perform(get("/checkPassword.do")
                        .param("userID", "u001")
                        .param("password", "bad"))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("false")));
    }

    @Test
    void checkPassword_shouldThrowNestedServletException_whenUserDoesNotExist_bugEvidence() throws Exception {
        // 缺陷/风险证据：UserController.checkPassword() 未对 user==null 做保护，会导致 NPE，
        // 在 MockMvc 环境中表现为 NestedServletException（构建可复现的失败/缺陷证据）。
        when(userService.findByUserID(eq("not-exist"))).thenReturn(null);

        assertThrows(NestedServletException.class, () ->
                mockMvc.perform(get("/checkPassword.do")
                                .param("userID", "not-exist")
                                .param("password", "any"))
                        .andReturn()
        );

        verify(userService).findByUserID("not-exist");
        verifyNoMoreInteractions(userService);
    }

    @Test
    void loginCheck_shouldReturnFalse_whenUserIdBlank_riskEvidence() throws Exception {
        // 风险证据：Controller 未做输入校验，userID 为空仍会调用 service（等价类：空字符串）
        when(userService.checkLogin(eq(""), eq("pw"))).thenReturn(null);

        mockMvc.perform(post("/loginCheck.do")
                        .param("userID", "")
                        .param("password", "pw"))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("false")));

        verify(userService).checkLogin("", "pw");
        verifyNoMoreInteractions(userService);
    }

    @Test
    void loginCheck_shouldReturnFalse_whenPasswordMissing_riskEvidence() throws Exception {
        // 风险证据：password 缺失时会被绑定为 null，但仍会调用 service（等价类：缺失/空）
        when(userService.checkLogin(eq("u001"), eq((String) null))).thenReturn(null);

        mockMvc.perform(post("/loginCheck.do")
                        .param("userID", "u001"))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("false")));

        verify(userService).checkLogin("u001", null);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void logout_shouldRemoveUserSession_andRedirectToIndex() throws Exception {
        User user = new User();
        user.setUserID("u001");
        user.setIsadmin(0);

        mockMvc.perform(get("/logout.do")
                        .sessionAttr("user", user))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index"))
                .andExpect(request().sessionAttributeDoesNotExist("user"));
    }

    @Test
    void quit_shouldRemoveAdminSession_andRedirectToIndex() throws Exception {
        User admin = new User();
        admin.setUserID("admin01");
        admin.setIsadmin(1);

        mockMvc.perform(get("/quit.do")
                        .sessionAttr("admin", admin))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index"))
                .andExpect(request().sessionAttributeDoesNotExist("admin"));
    }

    @Test
    void register_shouldCreateUser_andRedirectToLogin() throws Exception {
        mockMvc.perform(post("/register.do")
                        .param("userID", "u001")
                        .param("userName", "Alice")
                        .param("password", "pw")
                        .param("email", "a@b.com")
                        .param("phone", "13800000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("login"));

        verify(userService).create(any(User.class));
        verify(userService, never()).updateUser(any(User.class));
    }
}

