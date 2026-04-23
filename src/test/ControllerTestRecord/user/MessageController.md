注意：脚本使用 assertThrows 捕获异常以复现缺陷，因此测试执行通过，但系统行为不满足健壮性预期
## 1. MessageController 功能分析

根据 `MessageController.java`，该 Controller 包含以下接口：

| 序号  | 请求方式 | URL                       | 方法名                      | 功能说明                            | 返回类型                      |
| --- | ---- | ------------------------- | ------------------------ | ------------------------------- | ------------------------- |
| 1   | GET  | `/message_list`           | `message_list(...)`      | 返回留言管理页面，同时加载公共留言分页信息和当前用户留言总页数 | 视图名 `message_list`        |
| 2   | GET  | `/message/getMessageList` | `message_list(int)`      | 获取已通过审核的留言列表                    | JSON 列表 `List<MessageVo>` |
| 3   | GET  | `/message/findUserList`   | `user_message_list(...)` | 获取当前登录用户的留言列表                   | JSON 列表 `List<MessageVo>` |
| 4   | POST | `/sendMessage`            | `sendMessage(...)`       | 发送留言                            | 重定向到 `/message_list`      |
| 5   | POST | `/modifyMessage.do`       | `modifyMessage(...)`     | 修改留言内容并重置状态                     | 响应体 boolean               |
| 6   | POST | `/delMessage.do`          | `delMessage(int)`        | 删除留言                            | 响应体 boolean               |

本次测试脚本重点覆盖以下功能：

* 留言页面访问（已登录）；
* 留言页面未登录访问异常；
* 获取公共留言列表成功；
* 获取公共留言列表分页参数边界值异常；
* 获取当前用户留言列表成功；
* 获取当前用户留言列表未登录异常；
* 发送留言成功；
* 修改留言成功；
* 删除留言成功。

## 2. 本测试属于 Controller 层集成测试

本次 `MessageControllerTest` 结合使用了黑盒测试方法和白盒测试方法。

测试路径为：
    src/test/java/com/demo/controller/user/MessageControllerTest.java

### MessageController 集成测试用例表

注意，列里 **输入数据/前置条件** 主要用于说明请求参数、session 状态及 mock 行为，保留在测试用例文档中。

| 功能点列表  | 用例编号       | 用例描述                                                           | 输入数据/前置条件                                                                                                            | 预期结果                                                         | 测试结果                                                          | 结论      |
| ------ | ---------- | -------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------- | ------- |
| 页面访问   | MC-INT-001 | 已登录用户访问留言页面，验证 `/message_list` 返回留言页面视图。测试方法：正常路径（等价类：合法登录状态）。 | GET `/message_list`；Session 预置 `user`；Mock `findPassState` 返回空分页；Mock `findByUser("u001", pageable)` 返回空分页           | HTTP 200，返回视图名 `message_list`。                               | HTTP 200，返回视图名 `message_list`。                                | 通过      |
| 页面访问   | MC-INT-002 | 未登录访问留言页面：验证登录校验异常路径。测试方法：异常路径/鲁棒性测试。                          | GET `/message_list`；无 Session                                                                                        | 应提示未登录或返回统一错误响应，不应正常进入页面。                                    | 抛出 `NestedServletException`（最终由 `LoginException` 引发）。         | 失败/发现缺陷 |
| 公共留言列表 | MC-INT-003 | 获取公共留言列表成功。测试方法：正常路径（等价类：合法 page=1）。                           | GET `/message/getMessageList?page=1`；Mock `findPassState` 返回空分页；Mock `returnVo` 返回空列表                                | HTTP 200，返回 JSON 数组 `[]`。                                    | HTTP 200，响应体为 `[]`。                                           | 通过      |
| 公共留言列表 | MC-INT-004 | `page=0` 时获取公共留言列表：验证分页参数边界值。测试方法：边界值分析（page 最小非法值）。           | GET `/message/getMessageList?page=0`                                                                                 | 系统应拦截非法页码或返回明确错误，不应发生未处理异常。                                  | 抛出 `NestedServletException`。                                  | 失败/发现缺陷 |
| 用户留言列表 | MC-INT-005 | 已登录用户获取自己的留言列表成功。测试方法：正常路径（等价类：合法登录状态 + 合法 page=1）。            | GET `/message/findUserList?page=1`；Session 预置 `user`；Mock `findByUser("u001", pageable)` 返回空分页；Mock `returnVo` 返回空列表 | HTTP 200，返回 JSON 数组 `[]`。                                    | HTTP 200，响应体为 `[]`。                                           | 通过      |
| 用户留言列表 | MC-INT-006 | 未登录获取用户留言列表：验证登录校验异常路径。测试方法：异常路径/鲁棒性测试。                        | GET `/message/findUserList?page=1`；无 Session                                                                         | 应提示未登录或返回统一错误响应，不应正常返回用户留言。                                  | 抛出 `NestedServletException`（最终由 `LoginException` 引发）。         | 失败/发现缺陷 |
| 留言操作   | MC-INT-007 | 发送留言成功。测试方法：正常路径（等价类：合法 userID 与内容）。                           | POST `/sendMessage`；`userID=u001, content=hello`                                                                     | HTTP 3xx，重定向到 `/message_list`，并调用 `messageService.create()`。 | HTTP 3xx，重定向到 `/message_list`，且调用了 `messageService.create()`。 | 通过      |
| 留言操作   | MC-INT-008 | 修改留言成功。测试方法：正常路径（合法 messageID 与内容）。                            | POST `/modifyMessage.do`；`messageID=1, content=new`；Mock `findById(1)` 返回留言对象                                        | HTTP 200，响应体 `true`，并调用 `messageService.update(...)`。        | HTTP 200，响应体 `true`，且调用了 `findById(1)` 与 `update(...)`。       | 通过      |
| 留言操作   | MC-INT-009 | 删除留言成功。测试方法：正常路径（合法 messageID）。                                | POST `/delMessage.do`；`messageID=1`                                                                                  | HTTP 200，响应体 `true`，并调用 `messageService.delById(1)`。         | HTTP 200，响应体 `true`，且调用了 `messageService.delById(1)`。         | 通过      |

## 3.结果（覆盖范围和缺陷）

### (1).已覆盖接口

本测试脚本已覆盖以下接口：

| 接口                            | 是否覆盖 | 覆盖情况                      |
| ----------------------------- | ---- | ------------------------- |
| `GET /message_list`           | 是    | 已登录成功访问、未登录异常             |
| `GET /message/getMessageList` | 是    | 获取公共留言列表成功、`page=0` 边界值异常 |
| `GET /message/findUserList`   | 是    | 已登录成功、未登录异常               |
| `POST /sendMessage`           | 是    | 发送留言成功                    |
| `POST /modifyMessage.do`      | 是    | 修改留言成功                    |
| `POST /delMessage.do`         | 是    | 删除留言成功                    |

### (2).缺陷

#### 1: 缺陷 BUG-MC-001：`/message_list` 未登录时直接抛异常，缺少友好错误处理

##### 缺陷接口

    GET /message_list

##### 缺陷位置

    Object user=request.getSession().getAttribute("user");
    if(user==null) {
        throw new LoginException("请登录！");
    }

##### 缺陷描述

当用户未登录访问 `/message_list` 时，Controller 在已经查询过公共留言分页信息之后，再检查 session 中的 `user`。若未登录，则直接抛出 `LoginException`。在 MockMvc 测试环境中，该异常表现为 `NestedServletException`，请求无法被友好处理。

##### 复现用例

    messageListPage_shouldThrowNestedServletException_whenNotLoggedIn()

##### 实际结果

抛出：
    NestedServletException

根因是：
    LoginException

##### 预期结果

系统应返回明确的未登录提示，例如跳转登录页、返回统一错误页面或标准化错误响应，而不应直接抛出未处理异常。

* * *

#### 2: 缺陷 BUG-MC-002：`/message/getMessageList` 在 `page=0` 时发生分页参数异常

##### 缺陷接口

    GET /message/getMessageList

##### 缺陷位置

    Pageable message_pageable= PageRequest.of(page-1,5, Sort.by("time").descending());

##### 缺陷描述

当请求参数 `page=0` 时，Controller 会执行：
    PageRequest.of(-1, 5, ...)

导致非法分页参数异常，并在 MockMvc 环境中表现为 `NestedServletException`。说明当前接口未对分页参数进行边界检查。

##### 复现用例

    getMessageListApi_shouldThrowNestedServletException_whenPageIs0_boundaryValue()

##### 实际结果

抛出 `NestedServletException`，根因是分页参数非法。

##### 预期结果

系统应对非法页码进行参数校验，例如：

* 将 `page<=0` 修正为 1；
* 或返回明确错误提示；
* 而不应出现未处理异常。

* * *

#### 3: 缺陷 BUG-MC-003：`/message/findUserList` 未登录时直接抛异常，缺少友好错误处理

##### 缺陷接口

    GET /message/findUserList

##### 缺陷位置

    Object user=request.getSession().getAttribute("user");
    if(user==null) {
        throw new LoginException("请登录！");
    }

##### 缺陷描述

当用户未登录访问“我的留言列表”接口时，Controller 直接抛出 `LoginException`，在测试环境中表现为 `NestedServletException`。该接口属于异步 JSON 接口，若未登录时直接抛异常，前端无法获得结构化错误信息。

##### 复现用例

    findUserListApi_shouldThrowNestedServletException_whenNotLoggedIn()

##### 实际结果

抛出：
    NestedServletException

##### 预期结果

系统应返回统一的未登录错误响应，例如 JSON 错误信息或跳转登录页，而不是未处理异常。

### (3).风险

#### 1: 风险 RISK-MC-001：`/message_list` 在未登录时仍先查询公共留言，存在不必要调用

##### 风险接口

    GET /message_list

##### 风险位置

    Page<Message> messages=messageService.findPassState(message_pageable);
    List<MessageVo> message_list=messageVoService.returnVo(messages.getContent());
    
    model.addAttribute("total",messages.getTotalPages());
    
    Object user=request.getSession().getAttribute("user");
    if(user==null) {
        throw new LoginException("请登录！");
    }

##### 风险描述

该接口在检查用户是否登录之前，已经先调用了：
    messageService.findPassState(...)
    messageVoService.returnVo(...)

也就是说，未登录用户访问时，系统仍然会先执行一部分业务查询，再因为 session 中无 `user` 而抛异常。这会造成不必要的服务调用和资源消耗。

##### 测试证据

    messageListPage_shouldThrowNestedServletException_whenNotLoggedIn()

测试代码注释中也指出：Controller 会先查 `messageService.findPassState(...)`，但最终因 `session.user==null` 抛出异常。

##### 实际结果

未登录用户访问时，仍然先执行公共留言查询逻辑，之后才抛出登录异常。

##### 影响

会增加无效业务调用，不利于性能与控制流程的清晰性。更合理的做法应当是在依赖登录态的页面入口尽早校验 session。

* * *

#### 2: 风险 RISK-MC-002：`/sendMessage` 未校验 session，仅依赖传入 `userID`，存在伪造身份风险

##### 风险接口

    POST /sendMessage

##### 风险位置

    public void sendMessage(String userID, String content, HttpServletResponse response) throws IOException {
        Message message=new Message();
        message.setUserID(userID);

##### 风险描述

`sendMessage` 接口没有从 session 中获取当前登录用户，也没有校验请求发起者是否已登录，而是直接使用前端传入的 `userID` 构造留言对象。这意味着调用者可以绕过前端页面限制，伪造任意 `userID` 提交留言。

##### 测试证据

    sendMessage_shouldRedirectToMessageList_andCallCreate()

该用例直接提交：
    userID=u001
    content=hello

在未构造 session 的情况下仍可成功创建留言并重定向。

##### 实际结果

接口在无登录态情况下也能成功创建留言。

##### 影响

存在身份伪造风险，不利于系统安全性与数据可信性。更合理的实现方式应当从 session 获取当前用户 ID，而非信任客户端提交的 `userID` 参数。

* * *

#### 3: 风险 RISK-MC-003：`/modifyMessage.do` 未校验留言归属与登录态，存在越权修改风险

##### 风险接口

    POST /modifyMessage.do

##### 风险位置

    Message message=messageService.findById(messageID);
    message.setContent(content);
    message.setTime(LocalDateTime.now());
    message.setState(1);
    messageService.update(message);

##### 风险描述

该接口未检查当前请求是否有登录态，也未验证当前用户是否为该留言的拥有者。只要知道 `messageID`，理论上即可修改任意留言内容，并将状态重置为 1。

##### 测试证据

    modifyMessage_shouldReturnTrue_andCallUpdate()

测试中未构造 session，也未验证留言归属，仅通过 `messageID=1` 即可完成修改流程。

##### 实际结果

在无登录校验和归属校验的情况下可成功更新留言。

##### 影响

存在越权修改风险，可能导致其他用户留言被任意篡改。

* * *

#### 4: 风险 RISK-MC-004：`/delMessage.do` 未校验登录态与留言归属，存在越权删除风险

##### 风险接口

    POST /delMessage.do

##### 风险位置

    public boolean delMessage(int messageID)
    {
        messageService.delById(messageID);
        return true;
    }

##### 风险描述

该接口未校验用户登录态，也未验证当前用户是否有权删除指定留言。调用方只需提供 `messageID` 即可删除留言。

##### 测试证据

    delMessage_shouldReturnTrue_andCallDelete()

测试中未构造 session，仍可成功删除留言。

##### 实际结果

无登录态情况下即可调用删除逻辑。

##### 影响

存在越权删除风险，可能导致留言数据被恶意删除。

