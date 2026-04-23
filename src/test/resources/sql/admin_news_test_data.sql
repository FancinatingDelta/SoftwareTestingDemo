-- AdminNewsController 测试数据准备
-- 清理旧数据
DELETE FROM news WHERE newsID IN (3001, 3002, 3003, 3004, 3005);

-- 插入测试新闻
INSERT INTO news (newsID, title, content, time) 
VALUES (3001, 'Test News 1', 'Content for test news 1', NOW());

INSERT INTO news (newsID, title, content, time) 
VALUES (3002, 'Test News 2', 'Content for test news 2', NOW());

INSERT INTO news (newsID, title, content, time) 
VALUES (3003, 'Test News 3', 'Content for test news 3', NOW());

INSERT INTO news (newsID, title, content, time) 
VALUES (3004, 'Test News 4', 'Content for test news 4', NOW());

INSERT INTO news (newsID, title, content, time) 
VALUES (3005, 'Test News 5', 'Content for test news 5', NOW());
