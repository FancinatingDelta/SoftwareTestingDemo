注意：脚本使用 assertThrows 捕获异常以复现缺陷，因此测试执行通过，但系统行为不满足健壮性预期
# 1. OrderController 功能分析

根据 `OrderController.java`，该 Controller 包含以下接口：

| 序号  | 请求方式 | URL                      | 方法名                      | 功能说明                   | 返回类型                             |
| --- | ---- | ------------------------ | ------------------------ | ---------------------- | -------------------------------- |
| 1   | GET  | `/order_manage`          | `order_manage(...)`      | 返回用户订单管理页面，并计算总页数      | 视图名 `order_manage`               |
| 2   | GET  | `/order_place.do`        | `order_place(Model,int)` | 根据场馆 ID 进入下单页面，并绑定场馆信息 | 视图名 `order_place`                |
| 3   | GET  | `/order_place`           | `order_place(Model)`     | 直接进入下单页面               | 视图名 `order_place`                |
| 4   | GET  | `/getOrderList.do`       | `order_list(...)`        | 获取当前登录用户的订单列表          | JSON 列表 `List<OrderVo>`          |
| 5   | POST | `/addOrder.do`           | `addOrder(...)`          | 提交新订单                  | 重定向到 `order_manage`              |
| 6   | POST | `/finishOrder.do`        | `finishOrder(int)`       | 完成订单                   | 无返回体（HTTP 200）                   |
| 7   | GET  | `/modifyOrder.do`        | `editOrder(...)`         | 进入订单修改页面并绑定订单和场馆信息     | 视图名 `order_edit`                 |
| 8   | POST | `/modifyOrder`           | `modifyOrder(...)`       | 修改订单                   | 响应体 boolean，并重定向到 `order_manage` |
| 9   | POST | `/delOrder.do`           | `delOrder(int)`          | 删除订单                   | 响应体 boolean                      |
| 10  | GET  | `/order/getOrderList.do` | `getOrder(...)`          | 查询指定场馆某日的订单安排          | JSON 对象 `VenueOrder`             |

本次测试脚本重点覆盖以下功能：

- 订单管理页面访问（已登录）；
- 订单管理页面未登录访问异常；
- 获取订单列表成功；
- 获取订单列表未登录异常；
- 获取订单列表分页参数边界值异常；
- 删除订单成功；
- 完成订单成功；
- 下单页面访问并绑定场馆信息；
- 新增订单成功；
- 新增订单未登录异常；
- 查询指定场馆某日订单安排成功。

---

# 2. 本测试属于 Controller 层集成测试

本次 `OrderControllerTest` 结合使用了黑盒测试方法和白盒测试方法。

测试路径为：

```text
src/test/java/com/demo/controller/user/OrderControllerTest.java
```

聚焦于 Controller 层的以下内容：

- URL 映射是否正确；
- 参数绑定是否正确；
- Session 登录态判断是否正确；
- Service 是否被正确调用；
- 返回的页面、JSON、重定向是否正确；
- 异常路径和边界值路径是否会暴露问题。

---

## OrderController 集成测试用例表

注意，列里 **输入数据/前置条件** 主要用于说明测试时的请求参数、session 状态及 mock 行为，保留在测试用例文档中。

| 功能点列表  | 用例编号       | 用例描述                                                                   | 输入数据/前置条件                                                                                                                         | 预期结果                                                         | 测试结果                                                          | 结论      |
| ------ | ---------- | ---------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------- | ------- |
| 页面访问   | OC-INT-001 | 已登录用户访问订单管理页面，验证 `/order_manage` 返回订单管理视图并写入总页数。测试方法：正常路径（等价类：合法登录状态）。 | GET `/order_manage`；Session 预置 `user`；Mock `findUserOrder("u001", pageable)` 返回空分页对象                                              | HTTP 200，返回视图名 `order_manage`，并在 request 中写入属性 `total`。      | HTTP 200，返回视图名 `order_manage`，request 中存在 `total`。            | 通过      |
| 页面访问   | OC-INT-002 | 未登录访问订单管理页面：验证登录校验异常路径。测试方法：异常路径/鲁棒性测试。                                | GET `/order_manage`；无 Session                                                                                                     | 应提示未登录或返回统一错误响应，不应正常进入页面。                                    | 抛出 `NestedServletException`（根因 `LoginException`）。             | 失败/发现缺陷 |
| 订单列表   | OC-INT-003 | 已登录用户获取订单列表成功。测试方法：正常路径（等价类：合法 page=1，合法登录状态）。                         | GET `/getOrderList.do?page=1`；Session 预置 `user`；Mock `findUserOrder` 返回空分页，Mock `returnVo` 返回空列表                                  | HTTP 200，返回 JSON 数组 `[]`。                                    | HTTP 200，响应体为 `[]`。                                           | 通过      |
| 订单列表   | OC-INT-004 | 未登录获取订单列表：验证登录校验异常路径。测试方法：异常路径/鲁棒性测试。                                  | GET `/getOrderList.do?page=1`；无 Session                                                                                           | 应提示未登录或返回统一错误响应，不应返回订单列表。                                    | 抛出 `NestedServletException`（根因 `LoginException`）。             | 失败/发现缺陷 |
| 订单列表   | OC-INT-005 | `page=0` 时获取订单列表：验证分页参数边界值。测试方法：边界值分析（page 最小非法值）。                     | GET `/getOrderList.do?page=0`；Session 预置 `user`                                                                                   | 系统应拦截非法分页参数或返回明确错误，不应发生未处理异常。                                | 抛出 `NestedServletException`（由 `PageRequest.of(-1,...)` 导致）。   | 失败/发现缺陷 |
| 订单操作   | OC-INT-006 | 删除订单成功。测试方法：正常路径（合法订单 ID）。                                             | POST `/delOrder.do`；`orderID=123`                                                                                                 | HTTP 200，响应体 `true`，并调用 `orderService.delOrder(123)`。        | HTTP 200，响应体 `true`，且调用了 `orderService.delOrder(123)`。        | 通过      |
| 订单操作   | OC-INT-007 | 完成订单成功。测试方法：正常路径（合法订单 ID）。                                             | POST `/finishOrder.do`；`orderID=123`                                                                                              | HTTP 200，并调用 `orderService.finishOrder(123)`。                | HTTP 200，且调用了 `orderService.finishOrder(123)`。                | 通过      |
| 页面访问   | OC-INT-008 | 进入下单页面并绑定场馆信息。测试方法：正常路径（合法场馆 ID）。                                      | GET `/order_place.do?venueID=7`；Mock `findByVenueID(7)` 返回 `Venue` 对象                                                             | HTTP 200，返回视图名 `order_place`，并绑定 `venue`。                    | HTTP 200，返回视图名 `order_place`。                                 | 通过      |
| 新增订单   | OC-INT-009 | 已登录用户新增订单成功。测试方法：正常路径（合法输入 + 合法登录态）。                                   | POST `/addOrder.do`；`venueName=Gym A,startTime=2026-04-22 10:00,hours=2`；Session 预置 `user`                                        | HTTP 3xx，重定向到 `order_manage`，并调用 `orderService.submit(...)`。 | HTTP 3xx，重定向到 `order_manage`，且调用了 `orderService.submit(...)`。 | 通过      |
| 新增订单   | OC-INT-010 | 未登录新增订单：验证登录校验异常路径。测试方法：异常路径/鲁棒性测试。                                    | POST `/addOrder.do`；`venueName=Gym A,startTime=2026-04-22 10:00,hours=2`；无 Session                                                | 应提示未登录或返回统一错误响应，不应创建订单。                                      | 抛出 `NestedServletException`（根因 `LoginException`）。             | 失败/发现缺陷 |
| 场馆订单查询 | OC-INT-011 | 查询指定场馆某日订单安排成功。测试方法：正常路径（合法场馆名与日期）。                                    | GET `/order/getOrderList.do?venueName=Gym A&date=2026-04-22`；Mock `findByVenueName("Gym A")` 返回场馆，Mock `findDateOrder(...)` 返回空列表 | HTTP 200，返回 JSON，Content-Type 为 `application/json`。          | HTTP 200，返回 JSON。                                             | 通过      |

---

# 3. 结果（覆盖范围和缺陷）

## (1). 已覆盖接口

本测试脚本已覆盖以下接口：

| 接口                           | 是否覆盖 | 覆盖情况                       |
| ---------------------------- | ---- | -------------------------- |
| `GET /order_manage`          | 是    | 已登录成功访问、未登录异常              |
| `GET /order_place.do`        | 是    | 根据场馆 ID 进入下单页面             |
| `GET /order_place`           | 否    | 尚未覆盖，直接进入下单页面              |
| `GET /getOrderList.do`       | 是    | 已登录成功、未登录异常、`page=0` 边界值异常 |
| `POST /addOrder.do`          | 是    | 已登录下单成功、未登录异常              |
| `POST /finishOrder.do`       | 是    | 完成订单成功                     |
| `GET /modifyOrder.do`        | 否    | 尚未覆盖，涉及订单和场馆联合绑定           |
| `POST /modifyOrder`          | 否    | 尚未覆盖，涉及重定向和登录态校验           |
| `POST /delOrder.do`          | 是    | 删除订单成功                     |
| `GET /order/getOrderList.do` | 是    | 查询指定场馆某日订单安排成功             |

---

## (2). 缺陷

#### 1: 缺陷 BUG-OC-001：`/order_manage` 未登录时直接抛异常，缺少友好错误处理

##### 缺陷接口

```text
GET /order_manage
```

##### 缺陷位置

```java
Object user=request.getSession().getAttribute("user");
if(user==null) {
    throw new LoginException("请登录！");
}
```

##### 缺陷描述

当用户未登录访问 `/order_manage` 时，Controller 直接抛出 `LoginException`。在当前测试环境下，该异常未被友好处理，最终表现为 `NestedServletException`，导致请求异常终止，而不是返回明确的登录提示页或统一错误响应。

##### 复现用例

```java
orderManage_shouldThrowNestedServletException_whenNotLoggedIn()
```

##### 实际结果

抛出：

```text
NestedServletException
```

根因是：

```text
LoginException
```

##### 预期结果

系统应返回明确的未登录提示，例如跳转到登录页、返回统一错误页面，或返回标准化错误响应，而不应直接向上抛出未处理异常。

---

#### 2: 缺陷 BUG-OC-002：`/getOrderList.do` 未登录时直接抛异常，缺少友好错误处理

##### 缺陷接口

```text
GET /getOrderList.do
```

##### 缺陷位置

```java
Object user=request.getSession().getAttribute("user");
if(user==null) {
    throw new LoginException("请登录！");
}
```

##### 缺陷描述

当用户未登录访问订单列表接口时，Controller 同样直接抛出 `LoginException`，在测试环境中表现为 `NestedServletException`。该接口属于前端异步获取订单列表的 JSON 接口，若未登录时直接抛异常，前端难以得到结构化提示。

##### 复现用例

```java
getOrderList_shouldThrowNestedServletException_whenNotLoggedIn()
```

##### 实际结果

抛出：

```text
NestedServletException
```

##### 预期结果

系统应返回统一的未登录错误响应，例如 JSON 错误消息或跳转登录页，而不是未处理异常。

---

#### 3: 缺陷 BUG-OC-003：`/getOrderList.do` 在 `page=0` 时发生分页参数异常

##### 缺陷接口

```text
GET /getOrderList.do
```

##### 缺陷位置

```java
Pageable order_pageable = PageRequest.of(page-1,5, Sort.by("orderTime").descending());
```

##### 缺陷描述

当请求参数 `page=0` 时，Controller 会执行：

```java
PageRequest.of(-1, 5, ...)
```

这会触发非法分页参数异常，并在 MockMvc 环境中表现为 `NestedServletException`。说明当前接口未对分页参数进行边界检查。

##### 复现用例

```java
getOrderList_shouldThrowNestedServletException_whenPageIs0_boundaryValue()
```

##### 实际结果

抛出 `NestedServletException`，根因是分页参数非法。

##### 预期结果

系统应对非法页码进行参数校验，例如：

- 将 `page<=0` 统一修正为 1；
- 或返回明确错误提示；
- 而不应出现未处理异常。

---

#### 4: 缺陷 BUG-OC-004：`/addOrder.do` 未登录时直接抛异常，缺少友好错误处理

##### 缺陷接口

```text
POST /addOrder.do
```

##### 缺陷位置

```java
Object user=request.getSession().getAttribute("user");
if(user==null) {
    throw new LoginException("请登录！");
}
```

##### 缺陷描述

当用户未登录提交订单时，Controller 直接抛出 `LoginException`，没有返回明确的登录提示或统一错误页面。

##### 复现用例

```java
addOrder_shouldThrowNestedServletException_whenNotLoggedIn()
```

##### 实际结果

抛出：

```text
NestedServletException
```

##### 预期结果

系统应返回登录提示、统一错误响应或跳转登录页，而不应直接抛出未处理异常。

---

## (3). 风险

#### 1: 风险 RISK-OC-001：`/addOrder.do` 中 `date` 参数被覆盖，存在实现不一致风险

##### 风险接口

```text
POST /addOrder.do
```

##### 风险位置

```java
date=startTime+":00";
```

##### 风险描述

虽然接口参数中包含 `date`，但 Controller 实际上直接使用 `startTime` 拼接秒数来生成下单时间，并完全覆盖了原始 `date` 参数。这意味着：

- 前端传来的 `date` 参数实际上未被使用；
- 接口命名和实际行为不一致；
- 如果前端仍按“日期 + 开始时间”两个字段理解接口，可能产生歧义。

##### 测试证据

```java
addOrder_shouldRedirectToOrderManage_andCallSubmit_whenLoggedIn()
```

该用例中即使传入：

```text
date=ignored-by-controller
```

Controller 仍能正常完成下单，因为真正参与解析的是 `startTime`。

##### 影响

可能导致接口语义不清晰，不利于前后端协作和后续维护。

---

#### 2: 风险 RISK-OC-002：`/finishOrder.do` 无返回体，不利于前端判断执行结果

##### 风险接口

```text
POST /finishOrder.do
```

##### 风险位置

```java
@PostMapping("/finishOrder.do")
@ResponseBody
public void finishOrder(int orderID) {
    orderService.finishOrder(orderID);
}
```

##### 风险描述

该接口执行成功后仅返回 HTTP 200，但不返回任何布尔结果或结构化响应。相比 `delOrder.do` 返回 `true` 的设计，该接口的可用性和一致性稍差。

##### 测试证据

```java
finishOrder_shouldReturnOk_andCallService()
```

##### 实际结果

接口只返回 HTTP 200，无内容。

##### 影响

前端若需要判断“订单是否已成功完成”，只能依赖状态码，缺少更明确的反馈。

