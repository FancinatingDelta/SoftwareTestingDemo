-- AdminOrderController 测试数据
-- 用于判定覆盖和等价类划分的测试数据准备
-- 执行时机：每个测试方法执行前（@Sql + @Transactional）

-- 清理可能存在的残留数据（避免主键冲突）
DELETE FROM `order` WHERE orderID IN (1001, 1002, 1003);
DELETE FROM venue WHERE venueID = 2;

-- 插入测试场馆数据（venueID=2，与订单关联）
INSERT INTO venue (venueID, venue_name, description, price, address, open_time, close_time)
VALUES (2, '测试场馆', '用于测试的场馆', 100, '测试地址', '09:00', '22:00');

-- 测试数据1：有效订单 - 状态待审核(state=1)
-- 用于：passOrder.do / rejectOrder.do 的成功场景
INSERT INTO `order` (orderID, userID, venueID, state, order_time, start_time, hours, total) 
VALUES (1001, 'testuser', 2, 1, '2024-01-01 10:00:00', '2024-01-10 10:00:00', 2, 400);

-- 测试数据2：已通过订单 - 状态已审核(state=2)
-- 用于：测试非法状态转换（已通过订单再次审核应失败）
INSERT INTO `order` (orderID, userID, venueID, state, order_time, start_time, hours, total) 
VALUES (1002, 'testuser', 2, 2, '2024-01-01 11:00:00', '2024-01-10 11:00:00', 3, 600);

-- 测试数据3：已完成订单 - 状态已完成(state=3)
-- 用于：更全面的状态转换测试
INSERT INTO `order` (orderID, userID, venueID, state, order_time, start_time, hours, total) 
VALUES (1003, 'testuser', 2, 3, '2024-01-01 12:00:00', '2024-01-10 12:00:00', 1, 200);

-- 注：orderID=99999（不存在）无需插入，用于测试异常情况
-- 注：orderID=0 和 -1 作为边界值，无需预插入数据
