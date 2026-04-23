-- AdminMessageController 测试数据准备
-- 清理旧数据
DELETE FROM message WHERE messageID IN (2001, 2002, 2003, 2004, 2005);

-- 插入待审核留言 (state=1 - STATE_NO_AUDIT)
INSERT INTO message (messageID, userID, content, time, state) 
VALUES (2001, 'testuser1', 'Pending message 1', NOW(), 1);

INSERT INTO message (messageID, userID, content, time, state) 
VALUES (2002, 'testuser2', 'Pending message 2', NOW(), 1);

-- 插入已通过留言 (state=2 - STATE_PASS)
INSERT INTO message (messageID, userID, content, time, state) 
VALUES (2003, 'testuser3', 'Approved message', NOW(), 2);

-- 插入更多待审核留言用于分页测试
INSERT INTO message (messageID, userID, content, time, state) 
VALUES (2004, 'testuser4', 'Pending message 4', NOW(), 1);

INSERT INTO message (messageID, userID, content, time, state) 
VALUES (2005, 'testuser5', 'Pending message 5', NOW(), 1);
