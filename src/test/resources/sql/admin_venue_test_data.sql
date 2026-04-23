-- AdminVenueController 测试数据准备
-- 清理旧数据
DELETE FROM venue WHERE venueID IN (4001, 4002, 4003, 4004, 4005);

-- 插入测试场馆
INSERT INTO venue (venueID, venue_name, address, description, price, picture, open_time, close_time) 
VALUES (4001, 'TestVenue', 'Test Address 1', 'Description for venue 1', 100, '', '08:00', '22:00');

INSERT INTO venue (venueID, venue_name, address, description, price, picture, open_time, close_time) 
VALUES (4002, 'VenueToDelete', 'Test Address 2', 'Description for venue 2', 150, '', '09:00', '21:00');

INSERT INTO venue (venueID, venue_name, address, description, price, picture, open_time, close_time) 
VALUES (4003, 'VenueToModify', 'Test Address 3', 'Description for venue 3', 200, '', '10:00', '20:00');

INSERT INTO venue (venueID, venue_name, address, description, price, picture, open_time, close_time) 
VALUES (4004, 'Venue4', 'Test Address 4', 'Description for venue 4', 120, '', '08:30', '21:30');

INSERT INTO venue (venueID, venue_name, address, description, price, picture, open_time, close_time) 
VALUES (4005, 'Venue5', 'Test Address 5', 'Description for venue 5', 180, '', '07:00', '23:00');
