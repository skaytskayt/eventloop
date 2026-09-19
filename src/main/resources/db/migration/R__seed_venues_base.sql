-- Базовые площадки Москвы: реальные адреса и координаты.
--
-- Мероприятия сюда НЕ входят: афиша приходит импортом из раздела Пушкинской
-- карты билетного оператора (POST /admin/import). Источник отдаёт только
-- название площадки — страницы с адресами закрыты антибот-защитой,
-- поэтому координаты ведутся здесь.
--
-- Координаты приблизительные (±150 м): для фильтра «готов ехать до N км»
-- этого достаточно, для пешей навигации нет.
--
-- Инвариант, который проверяет схема: в Москве широта всегда больше долготы.
-- Перепутанные местами координаты дают правдоподобное, но неверное расстояние.

INSERT INTO venue (id, name, address, lat, lon) VALUES (1, 'Театр имени Евг. Вахтангова', 'ул. Арбат, 26', 55.7494, 37.5936)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (2, 'МХТ имени А. П. Чехова', 'Камергерский пер., 3', 55.7601, 37.6136)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (3, 'Театр «Ленком Марка Захарова»', 'ул. Малая Дмитровка, 6', 55.7688, 37.607)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (4, 'Большой театр', 'Театральная пл., 1', 55.7601, 37.6186)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (5, 'Малый театр', 'Театральный пр., 1', 55.7599, 37.6203)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (6, 'Московский театр «Современник»', 'Чистопрудный б-р, 19А', 55.7625, 37.6432)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (7, 'Театр сатиры', 'Триумфальная пл., 2', 55.7702, 37.596)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (8, 'Российский академический Молодёжный театр', 'Театральная пл., 2', 55.7607, 37.6203)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (9, 'Театр имени Моссовета', 'Б. Садовая, 16', 55.769, 37.5936)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (10, 'Театр на Таганке', 'ул. Земляной Вал, 76/21', 55.742, 37.6533)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (11, 'Государственная Третьяковская галерея', 'Лаврушинский пер., 10', 55.7415, 37.6208)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (12, 'Новая Третьяковка', 'Крымский Вал, 10', 55.7348, 37.6053)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (13, 'ГМИИ имени А. С. Пушкина', 'ул. Волхонка, 12', 55.7447, 37.6057)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (14, 'Музей Москвы', 'Зубовский б-р, 2', 55.7355, 37.5896)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (15, 'Музей космонавтики', 'пр-т Мира, 111', 55.8225, 37.6394)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (16, 'Государственный исторический музей', 'Красная пл., 1', 55.7553, 37.6178)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (17, 'Дарвиновский музей', 'ул. Вавилова, 57', 55.6907, 37.5623)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (18, 'Еврейский музей и центр толерантности', 'ул. Образцова, 11с1', 55.7896, 37.6003)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (19, 'Московская консерватория', 'Б. Никитская, 13/6', 55.7567, 37.6045)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (20, 'Концертный зал имени П. И. Чайковского', 'Триумфальная пл., 4/31', 55.77, 37.5956)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (21, 'Московский международный Дом музыки', 'Космодамианская наб., 52/8', 55.731, 37.6432)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (22, 'Концертный зал «Зарядье»', 'ул. Варварка, 6с1', 55.7513, 37.6288)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (23, 'Кинотеатр «Художественный»', 'Арбатская пл., 14', 55.7508, 37.5993)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (24, 'Кинотеатр «Октябрь»', 'ул. Новый Арбат, 24', 55.7524, 37.5867)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (25, 'Центр имени Вс. Мейерхольда', 'ул. Новослободская, 23', 55.7845, 37.5966)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (26, 'Музей-заповедник «Царицыно»', 'ул. Дольская, 1', 55.6155, 37.6858)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (27, 'Музей-заповедник «Коломенское»', 'пр-т Андропова, 39', 55.6674, 37.6706)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (28, 'ВДНХ', 'пр-т Мира, 119', 55.8264, 37.6376)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (29, 'Московский планетарий', 'Садовая-Кудринская, 5', 55.7614, 37.5836)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO venue (id, name, address, lat, lon) VALUES (30, 'Центр «Зотов»', 'ул. Ходынская, 2с1', 55.7679, 37.5588)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, lat = EXCLUDED.lat, lon = EXCLUDED.lon;

SELECT setval(pg_get_serial_sequence('venue', 'id'), (SELECT max(id) FROM venue));

-- Удаление демонстрационных мероприятий.
--
-- На раннем этапе, пока источник не был подключён, в базу заливался набор
-- правдоподобных, но ВЫДУМАННЫХ мероприятий с ключами seed-*. Показывать их
-- рядом с настоящей афишей Пушкинской карты нельзя: пользователь не отличит
-- одно от другого, а на защите это выглядело бы как подлог.
DELETE FROM event_item WHERE external_key LIKE 'seed-%';
