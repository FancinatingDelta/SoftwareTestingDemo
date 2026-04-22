## 1. UserController 功能分析

根据 `UserController.java`，该 Controller 包含以下接口：

| 序号 | 请求方式 | URL | 方法名 | 功能说明 | 返回类型 |
|---|---|---|---|---|---|
| 1 | GET | `/signup` | `signUp()` | 返回注册页面 | 视图名 `signup` |
| 2 | GET | `/login` | `login()` | 返回登录页面 | 视图名 `login` |
| 3 | POST | `/loginCheck.do` | `login(...)` | 用户/管理员登录校验 | 响应体字符串 |
| 4 | POST | `/register.do` | `register(...)` | 用户注册 | 重定向到 `login` |
| 5 | GET | `/logout.do` | `logout(...)` | 普通用户退出登录 | 重定向到 `/index` |
| 6 | GET | `/quit.do` | `quit(...)` | 管理员退出登录 | 重定向到 `/index` |
| 7 | POST | `/updateUser.do` | `updateUser(...)` | 更新用户信息及头像 | 重定向到 `user_info` |
| 8 | GET | `/checkPassword.do` | `checkPassword(...)` | 校验原密码是否正确 | 响应体 boolean |
| 9 | GET | `/user_info` | `user_info(...)` | 返回用户信息页面 | 视图名 `user_info` |

本次测试脚本重点覆盖以下功能：

- 注册页面访问；
- 登录页面访问；
- 用户信息页面访问；
- 普通用户登录成功；
- 管理员登录成功；
- 登录失败；
- 密码校验成功；
- 密码校验失败；
- 密码校验时用户不存在异常；
- 登录参数为空或缺失的风险；
- 普通用户退出；
- 管理员退出；
- 用户注册。

## 2. 本测试属于 Controller 层集成测试
本次 `UserControllerTest` 结合使用了黑盒测试方法和白盒测试方法。

测试路径为：
```text
src/test/java/com/demo/controller/user/UserControllerTest.java
```

### UserController 集成测试用例表
注意，列里 **输入数据/前置条件** 是debug用的，最后汇总交结果的时候应该删去？不过不删貌似也没什么吧！毕竟没有规定能不能加？

| 功能点列表 | 用例编号 | 用例描述 | 输入数据/前置条件 | 预期结果 | 测试结果 | 结论 |
|---|---|---|---|---|---|---|
| 页面访问 | UC-INT-001 | 访问注册页面，验证 `/signup` 返回注册视图。测试方法：正常路径（等价类：合法请求）。 | GET `/signup` | HTTP 200，返回视图名 `signup`。 | HTTP 200，返回视图名 `signup`。 | 通过 |
| 页面访问 | UC-INT-002 | 访问登录页面，验证 `/login` 返回登录视图。测试方法：正常路径（等价类：合法请求）。 | GET `/login` | HTTP 200，返回视图名 `login`。 | HTTP 200，返回视图名 `login`。 | 通过 |
| 页面访问 | UC-INT-003 | 访问用户信息页面，验证 `/user_info` 返回用户信息视图。测试方法：正常路径（等价类：合法请求）。 | GET `/user_info` | HTTP 200，返回视图名 `user_info`。 | HTTP 200，返回视图名 `user_info`。 | 通过 |
| 登录认证 | UC-INT-004 | 普通用户登录成功：覆盖 `isadmin==0` 分支并验证写入 Session。测试方法：等价类划分 + 判定/分支覆盖。 | POST `/loginCheck.do`；`userID=u001,password=pw`；Mock `checkLogin` 返回 `isadmin=0` 用户 | HTTP 200，响应体 `/index`，Session 中存在 `user`。 | HTTP 200，响应体 `/index`，Session 中存在 `user`。 | 通过 |
| 登录认证 | UC-INT-005 | 管理员登录成功：覆盖 `isadmin==1` 分支并验证写入 Session。测试方法：等价类划分 + 判定/分支覆盖。 | POST `/loginCheck.do`；`userID=admin01,password=pw`；Mock `checkLogin` 返回 `isadmin=1` 用户 | HTTP 200，响应体 `/admin_index`，Session 中存在 `admin`。 | HTTP 200，响应体 `/admin_index`，Session 中存在 `admin`。 | 通过 |
| 登录认证 | UC-INT-006 | 登录失败：覆盖 `user==null` 分支并验证不写入 Session。测试方法：无效等价类 + 判定/分支覆盖。 | POST `/loginCheck.do`；`userID=u001,password=wrong`；Mock `checkLogin` 返回 `null` | HTTP 200，响应体 `false`，Session 中不存在 `user/admin`。 | HTTP 200，响应体 `false`，Session 中不存在 `user/admin`。 | 通过 |
| 密码校验 | UC-INT-007 | 用户存在且密码匹配。测试方法：等价类（匹配）。 | GET `/checkPassword.do?userID=u001&password=pw`；Mock 用户密码为 `pw` | HTTP 200，响应体 `true`。 | HTTP 200，响应体 `true`。 | 通过 |
| 密码校验 | UC-INT-008 | 用户存在但密码不匹配。测试方法：等价类（不匹配）。 | GET `/checkPassword.do?userID=u001&password=bad`；Mock 用户密码为 `pw` | HTTP 200，响应体 `false`。 | HTTP 200，响应体 `false`。 | 通过 |
| 密码校验 | UC-INT-009 | 用户不存在时校验密码：复现缺陷（空指针/未处理异常）。测试方法：异常路径/鲁棒性测试（缺陷发现）。 | GET `/checkPassword.do?userID=not-exist&password=any`；Mock `findByUserID` 返回 `null` | **期望（健壮性）**：应返回明确失败结果（如 `false` 或统一错误响应），不应出现未处理异常。 | 抛出 `NestedServletException`（根因空指针）。 | 失败/发现缺陷 |
| 登录认证 | UC-INT-010 | `userID` 为空字符串时登录：验证输入校验风险。测试方法：无效等价类（空字符串）+ 鲁棒性。 | POST `/loginCheck.do`；`userID=""，password=pw`；Mock `checkLogin("", "pw")` 返回 `null` | 返回失败结果（如 `false`）；并记录是否存在输入校验缺失风险。 | HTTP 200，响应体 `false`；且仍调用 Service。 | 通过/风险记录 |
| 登录认证 | UC-INT-011 | `password` 参数缺失(null)时登录：验证输入校验风险。测试方法：无效等价类（缺失/null）+ 鲁棒性。 | POST `/loginCheck.do`；仅传 `userID=u001`；Mock `checkLogin("u001", null)` 返回 `null` | 返回失败结果（如 `false`）；并记录是否存在输入校验缺失风险。 | HTTP 200，响应体 `false`；仍调用 Service，password 绑定为 `null`。 | 通过/风险记录 |
| 退出登录 | UC-INT-012 | 普通用户退出登录：验证 session 状态变化与重定向。测试方法：状态转换 + Session 验证。 | GET `/logout.do`；Session 预置 `user` | HTTP 3xx，重定向 `/index`，Session 中 `user` 被移除。 | HTTP 3xx，重定向 `/index`，Session 中 `user` 被移除。 | 通过 |
| 退出登录 | UC-INT-013 | 管理员退出登录：验证 session 状态变化与重定向。测试方法：状态转换 + Session 验证。 | GET `/quit.do`；Session 预置 `admin` | HTTP 3xx，重定向 `/index`，Session 中 `admin` 被移除。 | HTTP 3xx，重定向 `/index`，Session 中 `admin` 被移除。 | 通过 |
| 用户注册 | UC-INT-014 | 用户注册成功：验证创建用户并重定向。测试方法：正常路径（等价类：合法注册信息）。 | POST `/register.do`；提交合法用户信息 | HTTP 3xx，重定向 `login`，并调用 `userService.create()`。 | HTTP 3xx，重定向 `login`，并调用 `userService.create()`。 | 通过 |

## 3.结果（覆盖范围和缺陷）

### (1).已覆盖接口

本测试脚本已覆盖以下接口：

| 接口 | 是否覆盖 | 覆盖情况 |
|---|---|---|
| `GET /signup` | 是 | 正常页面返回 |
| `GET /login` | 是 | 正常页面返回 |
| `POST /loginCheck.do` | 是 | 普通用户、管理员、登录失败、空 userID、缺失 password |
| `POST /register.do` | 是 | 注册成功 |
| `GET /logout.do` | 是 | 普通用户退出 |
| `GET /quit.do` | 是 | 管理员退出 |
| `GET /checkPassword.do` | 是 | 密码正确、密码错误、用户不存在异常 |
| `GET /user_info` | 是 | 正常页面返回 |
| `POST /updateUser.do` | 否 | 尚未覆盖，涉及文件上传 |

### (2).缺陷


#### 1: 缺陷 BUG-UC-001：`checkPassword.do` 用户不存在时发生空指针异常

##### 缺陷接口

```text
GET /checkPassword.do
```

##### 缺陷位置

```java
@GetMapping("/checkPassword.do")
@ResponseBody
public boolean checkPassword(String userID,String password)
{
    User user=userService.findByUserID(userID);
    return user.getPassword().equals(password);
}
```

##### 缺陷描述

当 `userService.findByUserID(userID)` 返回 `null` 时，Controller 未进行空值判断，直接调用：

```java
user.getPassword()
```

导致空指针异常。在 MockMvc 测试环境中表现为 `NestedServletException`。

##### 复现用例

```java
checkPassword_shouldThrowNestedServletException_whenUserDoesNotExist_bugEvidence()
```

##### 实际结果

抛出：

```text
NestedServletException
```

根因是：

```text
NullPointerException
```

##### 预期结果

系统应该返回：

```text
false
```
或返回明确的错误提示，而不应抛出未处理异常。
原因是密码校验通常发生在用户修改密码或资料更新前，如果传入不存在用户 ID，可能导致服务端异常。


---

####  2.风险 RISK-UC-001：登录接口缺少 userID 空值校验

##### 风险接口

```text
POST /loginCheck.do
```

##### 风险描述

当 `userID` 为空字符串时，Controller 未进行参数校验，而是直接调用：

```java
userService.checkLogin("", "pw")
```

##### 测试证据

```java
loginCheck_shouldReturnFalse_whenUserIdBlank_riskEvidence()
```

##### 实际结果

返回 `false`，但仍调用 Service。

##### 影响

如果 Service 或 DAO 层未对空字符串进行处理，可能导致无意义数据库查询，或在极端情况下引发异常。

---

#### 3 风险 RISK-UC-002：登录接口缺少 password 缺失校验

##### 风险接口

```text
POST /loginCheck.do
```

##### 风险描述

当请求未携带 password 参数时，Spring MVC 会将其绑定为 `null`。当前 Controller 仍会调用：

```java
userService.checkLogin("u001", null)
```

##### 测试证据

```java
loginCheck_shouldReturnFalse_whenPasswordMissing_riskEvidence()
```

##### 实际结果

返回 `false`，但仍调用 Service。