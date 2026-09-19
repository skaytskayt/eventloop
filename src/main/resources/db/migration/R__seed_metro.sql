-- Справочник станций московского метро (и МЦК/МЦД).
-- Источник: открытый справочник hh.ru (https://api.hh.ru/metro/1), загружен 2026-09-18.
-- Повторяемая миграция: правь файл и перезапускай приложение.

INSERT INTO metro_station (name, lat, lon) VALUES ('Авиамоторная', 55.7518, 37.718861)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Автозаводская', 55.706472, 37.660074)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Академическая', 55.687613, 37.573655)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Александровский сад', 55.752255, 37.608775)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Алексеевская', 55.807794, 37.638699)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Алма-Атинская', 55.63349, 37.765678)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Алтуфьево', 55.899034, 37.586473)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Аминьевская', 55.698333, 37.466111)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Андроновка', 55.742778, 37.737777)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Аникеевка', 55.832099, 37.219829)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Аннино', 55.583477, 37.596999)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Апрелевка', 55.550278, 37.0675)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Арбатская', 55.752217, 37.602522)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Аэропорт', 55.800441, 37.530477)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Аэропорт Внуково', 55.606667, 37.288333)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Бабушкинская', 55.870641, 37.664341)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Багратионовская', 55.743544, 37.497042)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Баковка', 55.682816, 37.315205)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Балтийская', 55.825833, 37.496111)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Баррикадная', 55.760793, 37.581242)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Бауманская', 55.772405, 37.67904)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Беговая', 55.773505, 37.545518)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Белокаменная', 55.83, 37.700556)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Беломорская', 55.8651, 37.4764)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Белорусская', 55.776046, 37.581748)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Беляево', 55.642357, 37.526115)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Бескудниково', 55.882713, 37.567768)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Бибирево', 55.883868, 37.603011)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Библиотека им.Ленина', 55.752123, 37.610388)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Битца', 55.571186, 37.611443)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Битцевский Парк', 55.600066, 37.556058)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Борисово', 55.6325, 37.743333)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Боровицкая', 55.750399, 37.60934)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Боровское шоссе', 55.647, 37.3701)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Ботанический сад', 55.845077, 37.639044)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Братиславская', 55.658817, 37.748415)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Бульвар Адмирала Ушакова', 55.545207, 37.542329)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Бульвар Генерала Карбышева', 55.780704, 37.469844)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Бульвар Дмитрия Донского', 55.568201, 37.576856)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Бульвар Рокоссовского', 55.816069, 37.734586)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Бунинская аллея', 55.537977, 37.515899)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Бутово', 55.548279, 37.555668)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Бутырская', 55.813333, 37.602778)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Вавиловская', 55.686688, 37.543706)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Варшавская', 55.653314, 37.619483)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('ВДНХ', 55.819626, 37.640751)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Верхние Котлы', 55.69, 37.618889)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Верхние Лихоборы', 55.85566, 37.56282)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Вешняки', 55.721944, 37.799167)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Владыкино', 55.847729, 37.591197)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Внуково', 55.648889, 37.269722)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Водники', 55.953419, 37.511143)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Водный стадион', 55.838978, 37.487515)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Войковская', 55.818923, 37.497791)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Волгоградский проспект', 55.725546, 37.685197)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Волжская', 55.690446, 37.754314)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Волоколамская', 55.835154, 37.382453)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Воробьевы горы', 55.709169, 37.557293)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Воронцовская', 55.658333, 37.540833)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Выхино', 55.716279, 37.81635)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Генерала Тюленева', 55.626248, 37.486032)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Говорово', 55.6588, 37.4174)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Гражданская', 55.805527, 37.55315)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Грачёвская', 55.869444, 37.509167)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Давыдково', 55.715278, 37.451667)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Дегунино', 55.86586, 37.573235)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Деловой центр', 55.748474, 37.537074)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Деловой центр (Выставочная)', 55.750243, 37.542641)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Депо', 55.674257, 37.728446)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Динамо', 55.789704, 37.558212)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Дмитровская', 55.808056, 37.581734)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Добрынинская', 55.728994, 37.622533)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Долгопрудная', 55.938656, 37.520542)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Домодедовская', 55.610131, 37.717111)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Достоевская', 55.781667, 37.613889)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Дубровка', 55.715375, 37.677017)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Ермакова Роща', 55.765556, 37.535278)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Жулебино', 55.684722, 37.855833)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Звенигородская', 55.779711, 37.388256)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('ЗИЛ', 55.698124, 37.647949)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Зорге', 55.787778, 37.504444)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Зюзино', 55.655158, 37.575786)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Зябликово', 55.611944, 37.745278)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Измайлово', 55.788611, 37.742778)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Измайловская', 55.787713, 37.779896)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Калитники', 55.733981, 37.702203)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Калужская', 55.656682, 37.540075)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Кантемировская', 55.636107, 37.656218)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Каховская', 55.65299, 37.597453)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Каширская', 55.655024, 37.648666)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Киевская', 55.743298, 37.565636)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Китай-город', 55.755429, 37.632601)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Кленовый бульвар', 55.674444, 37.680833)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Кожуховская', 55.706156, 37.68544)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Кокошкино', 55.599722, 37.171389)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Коломенская', 55.677423, 37.663719)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Коммунарка', 55.57354, 37.468116)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Комсомольская', 55.774872, 37.654668)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Коньково', 55.631857, 37.519156)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Коптево', 55.839637, 37.520037)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Корниловская', 55.599411, 37.479586)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Косино', 55.708383, 37.8493)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Котельники', 55.6743, 37.8582)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Красково', 55.649722, 37.9825)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Красногвардейская', 55.614075, 37.742697)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Красногорская', 55.814571, 37.303337)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Краснопресненская', 55.760378, 37.577114)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Красносельская', 55.780014, 37.666097)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Красные ворота', 55.768307, 37.6478)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Красный Балтиец', 55.815514, 37.526367)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Красный строитель', 55.589455, 37.615093)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Крёкшино', 55.577778, 37.109722)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Крестьянская застава', 55.732278, 37.665325)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Кропоткинская', 55.745297, 37.604217)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Крылатское', 55.756842, 37.408139)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Крымская', 55.690005, 37.605306)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Крюково', 55.979722, 37.173611)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Кузнецкий мост', 55.761498, 37.624423)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Кузьминки', 55.705493, 37.763295)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Кунцевская', 55.73058, 37.446175)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Курская', 55.753912, 37.660355)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Курьяново', 55.649722, 37.701667)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Кусково', 55.739722, 37.795278)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Кутузовская', 55.741107, 37.533496)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Кучино', 55.752222, 37.954722)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Левобережная', 55.886944, 37.468611)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Ленинский проспект', 55.70678, 37.58499)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Лермонтовский проспект', 55.702036, 37.851044)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Лесной Городок', 55.630278, 37.219167)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Лесопарковая', 55.581656, 37.577816)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Лефортово', 55.764444, 37.702778)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Лианозово', 55.914214, 37.548436)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Лихоборы', 55.848611, 37.551945)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Лобня', 56.0048, 37.29057)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Локомотив', 55.803219, 37.745742)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Ломоносовский проспект', 55.7055, 37.5225)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Лубянка', 55.759889, 37.625336)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Лужники', 55.720278, 37.563056)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Лухмановская', 55.7078, 37.9004)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Люберцы I', 55.681667, 37.896944)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Люблино', 55.680854, 37.747677)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Малино', 55.969167, 37.211667)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Марк', 55.904458, 37.538242)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Марксистская', 55.740746, 37.65604)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Марьина Роща', 55.79843, 37.617795)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Марьино', 55.649158, 37.743844)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Матвеевское', 55.704722, 37.482222)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Маяковская', 55.769808, 37.596192)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Медведково', 55.888103, 37.661562)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Менделеевская', 55.781999, 37.599141)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Мещерская', 55.666667, 37.424444)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Минская', 55.723266, 37.518428)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Митино', 55.846098, 37.36122)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Митьково', 55.786389, 37.6675)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Мичуринец', 55.646111, 37.315)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Мичуринский проспект', 55.688567, 37.485)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Мнёвники', 55.761153, 37.47139)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Молжаниново', 55.924167, 37.380833)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Молодежная', 55.741375, 37.415627)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Москва Товарная', 55.745358, 37.688839)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Москва-Сити', 55.748056, 37.532778)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Москворечье', 55.641239, 37.689789)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Моссельмаш', 55.862222, 37.526389)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Мякинино', 55.823342, 37.385214)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Нагатинская', 55.682099, 37.620917)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Нагатинский Затон', 55.684444, 37.703611)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Нагорная', 55.672962, 37.610397)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Народное Ополчение', 55.77592, 37.485073)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Нахабино', 55.841522, 37.185204)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Нахимовский проспект', 55.662379, 37.605274)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Некрасовка', 55.7029, 37.9264)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Немчиновка', 55.715668, 37.374611)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Нижегородская', 55.732014, 37.729076)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Никольское', 55.759722, 37.897778)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Новаторская', 55.670833, 37.52)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Новогиреево', 55.74834, 37.81646)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Новодачная', 55.924459, 37.527877)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Новокосино', 55.745113, 37.864052)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Новокузнецкая', 55.742391, 37.62928)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Новомосковская', 55.560094, 37.469807)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Новомосковская (Коммунарка)', 55.559765, 37.468716)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Новопеределкино', 55.638834, 37.367756)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Новоподрезково', 55.936389, 37.351667)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Новослободская', 55.779606, 37.601252)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Новохохловская', 55.721206, 37.717674)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Новоясеневская', 55.601947, 37.553017)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Новые Черемушки', 55.670077, 37.554493)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Одинцово', 55.67798, 37.27773)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Озёрная', 55.6698, 37.4495)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Окружная', 55.848889, 37.571111)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Окская', 55.72, 37.77)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Октябрьская', 55.730248, 37.61195)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Октябрьское поле', 55.793581, 37.493317)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Ольгино', 55.751667, 37.978611)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Ольховая', 55.5692, 37.4589)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Опалиха', 55.82333, 37.246843)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Орехово', 55.61269, 37.695214)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Останкино', 55.817222, 37.603611)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Остафьево', 55.50337, 37.520055)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Отрадное', 55.864273, 37.605066)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Охотный ряд', 55.757228, 37.615078)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Очаково I', 55.683611, 37.451389)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Павелецкая', 55.730577, 37.637494)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Павшино', 55.815231, 37.341461)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Панки', 55.668889, 37.9225)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Панфиловская', 55.799167, 37.498889)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Парк культуры', 55.735692, 37.594061)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Парк Победы', 55.736078, 37.515633)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Партизанская', 55.788401, 37.74882)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Пенягино', 55.822539, 37.361049)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Первомайская', 55.794376, 37.799364)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Переделкино', 55.656111, 37.353889)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Перерва', 55.660809, 37.716278)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Перово', 55.743268, 37.77461)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Петровский парк', 55.79233, 37.55952)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Петровско-Разумовская', 55.837651, 37.573134)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Печатники', 55.693821, 37.727919)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Пионерская', 55.736027, 37.466728)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Планерная', 55.859676, 37.436808)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Площадь Гагарина', 55.706944, 37.585833)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Площадь Ильича', 55.747115, 37.680726)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Площадь Революции', 55.756741, 37.62236)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Площадь трёх вокзалов', 55.776377, 37.651486)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Плющево', 55.730556, 37.774167)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Победа', 55.566111, 37.093056)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Подольск', 55.431667, 37.565)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Подрезково', 55.941667, 37.334722)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Поклонная', 55.728333, 37.511667)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Покровское', 55.814247, 37.47678)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Покровское-Стрешнево', 55.814247, 37.47678)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Полежаевская', 55.777201, 37.517895)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Полянка', 55.736795, 37.618594)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Потапово', 55.552538, 37.490978)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Пражская', 55.610962, 37.602386)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Преображенская площадь', 55.796322, 37.713582)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Прокшино', 55.5813, 37.4425)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Пролетарская', 55.731546, 37.666917)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Проспект Вернадского', 55.677164, 37.504792)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Проспект Мира', 55.780705, 37.633422)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Профсоюзная', 55.677671, 37.562595)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Пушкинская', 55.765607, 37.604356)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Пыхтино', 55.625, 37.298056)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Пятницкое шоссе', 55.853634, 37.353108)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Рабочий Посёлок', 55.726957, 37.415577)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Раменки', 55.6961, 37.505)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Рассказовка', 55.6324, 37.3328)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Реутов', 55.751389, 37.855833)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Речной вокзал', 55.854152, 37.476728)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Рижская', 55.793609, 37.636779)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Римская', 55.747027, 37.679996)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Ростокино', 55.839444, 37.667778)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Румянцево', 55.633, 37.4419)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Рязанский проспект', 55.716139, 37.792694)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Савёловская', 55.794122, 37.58791)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Саларьево', 55.6227, 37.424)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Салтыковская', 55.757778, 37.923056)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Санино', 55.583889, 37.138333)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Свиблово', 55.855558, 37.653379)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Севастопольская', 55.651451, 37.59809)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Селигерская', 55.86483, 37.55005)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Семеновская', 55.783096, 37.719289)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Серп и Молот', 55.748056, 37.681944)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Серпуховская', 55.726548, 37.624792)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Сетунь', 55.723713, 37.397259)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Силикатная', 55.470278, 37.555278)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Сколково', 55.666801, 37.424618)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Славянский бульвар', 55.729632, 37.470765)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Смоленская', 55.748398, 37.582988)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Сокол', 55.805564, 37.515245)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Соколиная Гора', 55.77, 37.745278)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Сокольники', 55.790197, 37.679392)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Солнечная', 55.656944, 37.383611)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Солнцево', 55.649, 37.3911)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Сортировочная', 55.763611, 37.720833)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Спартак', 55.8182, 37.4352)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Спортивная', 55.722388, 37.562041)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Сретенский бульвар', 55.766106, 37.635688)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Стахановская', 55.73, 37.76)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Стрешнево', 55.813611, 37.486944)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Строгино', 55.803831, 37.402405)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Студенческая', 55.738761, 37.54842)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Сухаревская', 55.772315, 37.63285)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Сходненская', 55.84926, 37.44076)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Сходня', 55.949722, 37.298611)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Таганская', 55.740949, 37.653469)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Тверская', 55.765343, 37.603918)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Театральная', 55.758808, 37.61768)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Текстильщики', 55.708826, 37.730578)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Теплый Стан', 55.61873, 37.505912)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Терехово', 55.748108, 37.459738)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Тестовская', 55.752562, 37.530915)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Технопарк', 55.695, 37.664167)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Тимирязевская', 55.81866, 37.574498)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Толстопальцево', 55.607778, 37.186389)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Томилино', 55.655, 37.954722)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Третьяковская', 55.740927, 37.625883)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Трикотажная', 55.833137, 37.398967)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Тропарево', 55.6459, 37.4725)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Трубная', 55.76771, 37.621926)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Тульская', 55.70961, 37.622569)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Тургеневская', 55.765371, 37.636732)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Тушинская', 55.825479, 37.437024)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Тютчевская', 55.618801, 37.481415)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Угрешская', 55.718333, 37.697778)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Улица 1905 года', 55.763944, 37.562271)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Улица Академика Янгеля', 55.596753, 37.601498)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Улица Горчакова', 55.542281, 37.532063)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Улица Дмитриевского', 55.7093, 37.8792)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Улица Скобелевская', 55.548103, 37.552721)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Улица Старокачаловская', 55.569194, 37.576074)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Университет', 55.69329, 37.534511)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Университет дружбы народов', 55.64824, 37.507556)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Ухтомская', 55.698611, 37.864167)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Физтех', 55.921389, 37.546389)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Филатов луг', 55.5997, 37.4075)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Филевский парк', 55.739665, 37.483902)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Фили', 55.745513, 37.51428)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Фирсановская', 55.960278, 37.251111)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Фонвизинская', 55.822778, 37.588056)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Фрунзенская', 55.727462, 37.58022)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Химки', 55.894444, 37.450833)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Хлебниково', 55.970682, 37.504638)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Ховрино', 55.878572, 37.487184)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Хорошево', 55.777222, 37.507222)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Хорошевская', 55.77643, 37.51981)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Царицыно', 55.619646, 37.669229)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Цветной бульвар', 55.771653, 37.620466)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('ЦСКА', 55.78643, 37.53502)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Черкизовская', 55.802787, 37.744863)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Чертановская', 55.640538, 37.606065)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Чеховская', 55.765747, 37.608493)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Чистые пруды', 55.76499, 37.638293)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Чкаловская', 55.755951, 37.659293)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Шаболовская', 55.718828, 37.607892)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Шелепиха', 55.757365, 37.525633)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Шереметьевская', 55.983882, 37.498752)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Шипиловская', 55.621667, 37.743611)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Шоссе Энтузиастов', 55.758362, 37.75009)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Щелковская', 55.809962, 37.798261)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Щербинка', 55.509724, 37.562008)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Щукинская', 55.8094, 37.463241)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Электрозаводская', 55.781821, 37.705295)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Юго-Восточная', 55.71, 37.82)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Юго-Западная', 55.663146, 37.482852)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Южная', 55.622436, 37.609047)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Ясенево', 55.606182, 37.5334)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;
INSERT INTO metro_station (name, lat, lon) VALUES ('Яхромская', 55.8775, 37.545833)
  ON CONFLICT (name) DO UPDATE SET lat = EXCLUDED.lat, lon = EXCLUDED.lon;