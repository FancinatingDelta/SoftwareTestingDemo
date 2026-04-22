注意：脚本使用 assertThrows 捕获异常以复现缺陷，因此测试执行通过，但系统行为不满足健壮性预期
## 1. VenueController 功能分析

根据 `VenueController.java`，该 Controller 包含以下接口：

| 序号  | 请求方式 | URL                       | 方法名                 | 功能说明                    | 返回类型                    |
| --- | ---- | ------------------------- | ------------------- | ----------------------- | ----------------------- |
| 1   | GET  | `/venue`                  | `toGymPage(...)`    | 根据场馆 ID 返回场馆详情页面并绑定场馆对象 | 视图名 `venue`             |
| 2   | GET  | `/venuelist/getVenueList` | `venue_list(int)`   | 获取场馆分页列表                | JSON 分页对象 `Page<Venue>` |
| 3   | GET  | `/venue_list`             | `venue_list(Model)` | 返回场馆列表页面并绑定场馆列表与总页数     | 视图名 `venue_list`        |

本次测试脚本重点覆盖以下功能：

* 场馆列表页面访问；
* 场馆详情页面访问；
* 获取场馆分页列表成功；
* 获取场馆分页列表分页参数边界值异常。

## 2. 本测试属于 Controller 层集成测试

本次 `VenueControllerTest` 结合使用了黑盒测试方法和白盒测试方法。

测试路径为：
    src/test/java/com/demo/controller/user/VenueControllerTest.java

### VenueController 集成测试用例表

注意，列里 **输入数据/前置条件** 主要用于说明请求参数及 mock 行为，保留在测试用例文档中。

| 功能点列表  | 用例编号       | 用例描述                                                            | 输入数据/前置条件                                                                          | 预期结果                                                                  | 测试结果                                                             | 结论      |
| ------ | ---------- | --------------------------------------------------------------- | ---------------------------------------------------------------------------------- | --------------------------------------------------------------------- | ---------------------------------------------------------------- | ------- |
| 页面访问   | VC-INT-001 | 访问场馆列表页面，验证 `/venue_list` 返回场馆列表视图，并能计算总页数。测试方法：正常路径（等价类：合法请求）。 | GET `/venue_list`；Mock `venueService.findAll(pageable)` 返回空分页对象                    | HTTP 200，返回视图名 `venue_list`，并调用 `venueService.findAll(...)` 获取列表和总页数。 | HTTP 200，返回视图名 `venue_list`，且 `venueService.findAll(...)` 被调用两次。 | 通过      |
| 页面访问   | VC-INT-002 | 访问场馆详情页面，验证 `/venue` 返回场馆详情视图并绑定场馆对象。测试方法：正常路径（等价类：合法 venueID）。 | GET `/venue?venueID=7`；Mock `venueService.findByVenueID(7)` 返回场馆对象                 | HTTP 200，返回视图名 `venue`，并调用 `venueService.findByVenueID(7)`。           | HTTP 200，返回视图名 `venue`，且调用了 `venueService.findByVenueID(7)`。     | 通过      |
| 场馆列表接口 | VC-INT-003 | 获取场馆分页列表成功。测试方法：正常路径（等价类：合法 page=1）。                            | GET `/venuelist/getVenueList?page=1`；Mock `venueService.findAll(pageable)` 返回空分页对象 | HTTP 200，返回 JSON，Content-Type 为 `application/json`。                   | HTTP 200，返回 JSON。                                                | 通过      |
| 场馆列表接口 | VC-INT-004 | `page=0` 时获取场馆分页列表：验证分页参数边界值。测试方法：边界值分析（page 最小非法值）。            | GET `/venuelist/getVenueList?page=0`                                               | 系统应拦截非法页码或返回明确错误，不应发生未处理异常。                                           | 抛出 `NestedServletException`。                                     | 失败/发现缺陷 |

## 3.结果（覆盖范围和缺陷）

### (1).已覆盖接口

本测试脚本已覆盖以下接口：

| 接口                            | 是否覆盖 | 覆盖情况                      |
| ----------------------------- | ---- | ------------------------- |
| `GET /venue`                  | 是    | 场馆详情页面正常访问                |
| `GET /venuelist/getVenueList` | 是    | 获取场馆分页列表成功、`page=0` 边界值异常 |
| `GET /venue_list`             | 是    | 场馆列表页面正常访问                |

### (2).缺陷

#### 1: 缺陷 BUG-VC-001：`/venuelist/getVenueList` 在 `page=0` 时发生分页参数异常

##### 缺陷接口

    GET /venuelist/getVenueList

##### 缺陷位置

    Pageable venue_pageable= PageRequest.of(page-1,5, Sort.by("venueID").ascending());

##### 缺陷描述

当请求参数 `page=0` 时，Controller 会执行：
    PageRequest.of(-1, 5, ...)

导致非法分页参数异常，并在 MockMvc 测试环境中表现为 `NestedServletException`。说明当前接口未对分页参数进行边界检查。

##### 复现用例

    getVenueListApi_shouldThrowNestedServletException_whenPageIs0_boundaryValue()

##### 实际结果

抛出：
    NestedServletException

根因是分页参数非法。

##### 预期结果

系统应对非法页码进行参数校验，例如：
    page <= 0 时修正为 1

或返回明确错误提示，而不应出现未处理异常。

### (3).风险

#### 1: 风险 RISK-VC-001：`/venue_list` 内部重复调用 `venueService.findAll(...)`，存在重复查询风险

##### 风险接口

    GET /venue_list

##### 风险位置

    List<Venue> venue_list=venueService.findAll(venue_pageable).getContent();
    model.addAttribute("venue_list",venue_list);
    model.addAttribute("total", venueService.findAll(venue_pageable).getTotalPages());

##### 风险描述

该接口在构造页面数据时，对同一个 `Pageable` 参数调用了两次：
    venueService.findAll(venue_pageable)

第一次用于获取场馆列表内容，第二次用于获取总页数。虽然功能上没有问题，但会导致重复查询，增加不必要的开销。

##### 测试证据

    venueListPage_shouldReturnVenueListView_andProvideTotalPages()

测试中通过：
    verify(venueService, times(2)).findAll(any());

验证了该方法被调用两次。

##### 实际结果

接口逻辑执行时，`findAll(...)` 被重复调用两次。

##### 影响

在场馆数据量较大或数据库访问成本较高时，可能造成额外性能损耗。更合理的实现方式应为一次查询后复用返回的 `Page<Venue>` 对象。

* * *

#### 2: 风险 RISK-VC-002：测试代码为 `/venue` 与 `/venue_list` 构造了 session，但 Controller 本身并不依赖登录态

##### 风险说明

当前 `VenueController` 的三个接口中均未读取 session，也未进行登录校验，因此从代码逻辑看，场馆列表和场馆详情属于公共访问页面。

测试代码中在：
    venueListPage_shouldReturnVenueListView_andProvideTotalPages()
    venueDetail_shouldReturnVenueView_andBindVenue()

中人为构造了：
    .sessionAttr("user", user)

虽然这不会影响测试结果，但说明这些接口实际上并不依赖登录态。该点不是代码缺陷，而是测试说明中的一个注意事项。

##### 影响

对最终报告无实质负面影响，但在描述测试前置条件时应避免误写成“必须登录后才能访问场馆页面”。

