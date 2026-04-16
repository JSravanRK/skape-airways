-- ═══════════════════════════════════════════════════
--  Skape Airways — Sample Data Seed Script
--  Run this AFTER starting the app for the first time
--  (airports and admin are auto-seeded by the app)
-- ═══════════════════════════════════════════════════

USE airline_db;

-- Sample Flights (run after app creates the tables)
-- Airport IDs are auto-generated; adjust if needed.
-- DEL=1, BOM=2, MAA=3, BLR=4, HYD=5, CCU=6, COK=7, AMD=8, GOI=9, PNQ=10, JAI=11, LKO=12

INSERT INTO flights (flight_number, from_airport_id, to_airport_id,
    departure_time, arrival_time, total_seats, available_seats,
    economy_price, business_price, first_class_price,
    airline_name, aircraft_type, status, version, created_at)
VALUES
  ('SW101', 1, 2, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY + INTERVAL 2 HOUR),
   150, 150, 3500, 9000, 18000, 'Skape Airways', 'Boeing 737', 'SCHEDULED', 0, NOW()),

  ('SW102', 2, 1, DATE_ADD(NOW(), INTERVAL 1 DAY + INTERVAL 4 HOUR), DATE_ADD(NOW(), INTERVAL 1 DAY + INTERVAL 6 HOUR),
   150, 150, 3800, 9500, 19000, 'Skape Airways', 'Boeing 737', 'SCHEDULED', 0, NOW()),

  ('SW201', 1, 4, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY + INTERVAL 3 HOUR),
   180, 180, 4200, 10500, 21000, 'Skape Airways', 'Airbus A320', 'SCHEDULED', 0, NOW()),

  ('SW202', 4, 1, DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY + INTERVAL 3 HOUR),
   180, 180, 4000, 10000, 20000, 'Skape Airways', 'Airbus A320', 'SCHEDULED', 0, NOW()),

  ('SW301', 2, 3, DATE_ADD(NOW(), INTERVAL 1 DAY + INTERVAL 6 HOUR), DATE_ADD(NOW(), INTERVAL 1 DAY + INTERVAL 7 HOUR 30 MINUTE),
   120, 120, 2800, 7000, 14000, 'Skape Airways', 'ATR 72', 'SCHEDULED', 0, NOW()),

  ('SW401', 1, 5, DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY + INTERVAL 2 HOUR 30 MINUTE),
   160, 160, 3200, 8000, 16000, 'Skape Airways', 'Boeing 737', 'SCHEDULED', 0, NOW()),

  ('SW501', 4, 2, DATE_ADD(NOW(), INTERVAL 1 DAY + INTERVAL 8 HOUR), DATE_ADD(NOW(), INTERVAL 1 DAY + INTERVAL 9 HOUR 30 MINUTE),
   150, 150, 3600, 9200, 18500, 'Skape Airways', 'Airbus A320', 'SCHEDULED', 0, NOW()),

  ('SW601', 1, 6, DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY + INTERVAL 2 HOUR),
   200, 200, 4500, 11000, 22000, 'Skape Airways', 'Boeing 777', 'SCHEDULED', 0, NOW()),

  ('SW701', 2, 9, DATE_ADD(NOW(), INTERVAL 1 DAY + INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 1 DAY + INTERVAL 3 HOUR 30 MINUTE),
   130, 130, 5500, 13000, 26000, 'Skape Airways', 'Airbus A320', 'SCHEDULED', 0, NOW()),

  ('SW801', 5, 3, DATE_ADD(NOW(), INTERVAL 2 DAY + INTERVAL 5 HOUR), DATE_ADD(NOW(), INTERVAL 2 DAY + INTERVAL 6 HOUR),
   120, 120, 2500, 6500, 13000, 'Skape Airways', 'ATR 72', 'SCHEDULED', 0, NOW());
