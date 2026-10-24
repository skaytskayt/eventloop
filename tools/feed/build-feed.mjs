/*
 * Сборщик снимка афиши для Mini App: Москва + Казань.
 *
 * Источник — раздел Пушкинской карты билетного оператора
 * (www.ticketland.ru и kzn.ticketland.ru), тот же, что разбирает
 * TicketlandParser на бэкенде: читаются блоки schema.org/Event в JSON-LD,
 * а не вёрстка. Кино — из раздела Пушкинской карты «Синема 5» (см. crawlCinema). На выходе — src/main/resources/static/app/data.js, афиши
 * в assets/events/ и их источники в assets/events/CREDITS.md.
 *
 * Запуск из корня репозитория (нужна сеть, npm install не нужен):
 *
 *     node tools/feed/build-feed.mjs
 *
 * Скрипт перезапускаемый: повторный запуск берёт свежую афишу и
 * перезаписывает снимок целиком. Два реестра рядом с ним — его память
 * между запусками, их коммитят вместе с data.js:
 *
 *  - venues.json — координаты площадок по ключу «<city>|<название из JSON-LD>».
 *    Адресов в JSON-LD нет, а страницы площадок закрыты антиботом, поэтому
 *    неизвестную площадку ищет Nominatim и дописывает сюда (source
 *    «nominatim»). Не нашёл — скрипт печатает список и выходит с кодом 1:
 *    впишите координаты вручную (source «manual») или {skip: true, reason}
 *    и перезапустите. Центр города вместо площадки не подставляется —
 *    фильтр «До N км» стал бы врать молча. Записи «manual» у московских
 *    площадок перенесены из R__seed_venues_*.sql.
 *  - ids.json — ticketUrl -> eventId. Только дополняется: по eventId сервер
 *    хранит реакции (miniapp_reaction), поэтому id события не меняется
 *    между снимками, а id выпавших из афиши событий не переиспользуются.
 *
 * Вежливость к источникам: паузы между страницами листинга, запросами
 * к Nominatim (его политика — не чаще 1 запроса в секунду, свой User-Agent)
 * и скачиваниями афиш; известные площадки в Nominatim повторно не ходят.
 */
import { existsSync, mkdirSync, readdirSync, readFileSync, unlinkSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(HERE, '..', '..');
const APP = path.join(ROOT, 'src', 'main', 'resources', 'static', 'app');
const DATA_JS = path.join(APP, 'data.js');
const EVENTS_DIR = path.join(APP, 'assets', 'events');
const CREDITS = path.join(EVENTS_DIR, 'CREDITS.md');
const VENUES_JSON = path.join(HERE, 'venues.json');
const IDS_JSON = path.join(HERE, 'ids.json');

const CITIES = {
  moscow: { host: 'www.ticketland.ru', title: 'Москва', lat: 55.7558, lon: 37.6173 },
  kazan: { host: 'kzn.ticketland.ru', title: 'Казань', lat: 55.7961, lon: 49.1064 }
};
/*
 * Не больше 200 событий на город, не считая кино: копия Mini App лежит и в
 * git, и на GitHub Pages, а одна афиша — около 30 КБ.
 */
const LIMIT_PER_CITY = 200;

/*
 * Кино по Пушкинской карте — у сети «Синема 5»: у Ticketland его нет вовсе.
 * Страницы — Nuxt, данные лежат в __NUXT_DATA__ (фильмы, сеансы с ценой,
 * кинотеатры с координатами). Событие — пара «фильм + кинотеатр» с ближайшим
 * сеансом; /api/** и страницы с датой закрыты robots.txt, их не трогаем.
 */
const CINEMA_HOST = 'https://cinema5.ru';
const CINEMA_CITY = { moscow: 'moskva', kazan: 'kazan' };
const WALLET_LIMIT = { CINEMA: 2000, OTHER: 3000 };
const MAX_VENUE_KM = 60;
const MAX_PAGES = 40;
const DEMO_DATE = '2026-10-29';

const BROWSER_UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
  + '(KHTML, like Gecko) Chrome/130.0 Safari/537.36';
const NOMINATIM_UA = 'EventLoop-feed-builder/1.0';
const PAUSE_PAGE_MS = 1200;
const PAUSE_NOMINATIM_MS = 1100;
const PAUSE_POSTER_MS = 300;

/* Точки отсчёта Казани — для сводки по расстояниям (те же, что Screens.ORIGINS). */
const KAZAN_ORIGINS = [
  { name: 'Казань · центр', lat: 55.7961, lon: 49.1064 },
  { name: 'Казань · м. Козья слобода', lat: 55.8182, lon: 49.0999 },
  { name: 'Казань · м. Горки', lat: 55.7600, lon: 49.1916 },
  { name: 'Казань · м. Проспект Победы', lat: 55.7504, lon: 49.2096 }
];

const LD_JSON = /<script[^>]*type\s*=\s*["']application\/ld\+json["'][^>]*>([\s\S]*?)<\/script>/g;

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

function readJson(file) {
  return existsSync(file) ? JSON.parse(readFileSync(file, 'utf8')) : {};
}

function writeJson(file, value) {
  writeFileSync(file, JSON.stringify(value, null, 2) + '\n', 'utf8');
}

/* Прежний снимок — CommonJS-файл с module.exports в хвосте. */
function loadPreviousEvents() {
  if (!existsSync(DATA_JS)) return [];
  try {
    const require = createRequire(import.meta.url);
    const events = require(DATA_JS);
    return Array.isArray(events) ? events : [];
  } catch (e) {
    console.warn('Прежний data.js не прочитан: ' + e.message);
    return [];
  }
}

function haversineKm(lat1, lon1, lat2, lon2) {
  const rad = (d) => d * Math.PI / 180;
  const dLat = rad(lat2 - lat1);
  const dLon = rad(lon2 - lon1);
  const a = Math.sin(dLat / 2) ** 2
    + Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(dLon / 2) ** 2;
  return 2 * 6371 * Math.asin(Math.min(1, Math.sqrt(a)));
}

function decodeEntities(s) {
  if (s == null) return null;
  return String(s)
    .replace(/&quot;/g, '"')
    .replace(/&#39;|&#039;|&apos;/g, "'")
    .replace(/&laquo;/g, '«')
    .replace(/&raquo;/g, '»')
    .replace(/&nbsp;/g, ' ')
    .replace(/&#(\d+);/g, (_, n) => String.fromCodePoint(Number(n)))
    .replace(/&amp;/g, '&')
    .replace(/\s+/g, ' ')
    .trim();
}

function absolute(url, host) {
  if (!url || !String(url).trim()) return null;
  const u = String(url).trim();
  if (u.startsWith('//')) return 'https:' + u;
  if (u.startsWith('/')) return 'https://' + host + u;
  return u;
}

/* Как TicketlandParser.price: «400», «1 200», изредка с копейками. */
function price(raw) {
  if (raw == null) return null;
  const digits = String(raw).replace(/[^\d.,]/g, '').replace(/[.,]\d{1,2}$/, '').replace(/[^\d]/g, '');
  if (!digits) return null;
  const value = parseInt(digits, 10);
  return value > 0 ? value : null;
}

/* Держать в паре с TicketlandParser.mapCategory — те же подстроки в том же порядке. */
function mapCategory(additionalType, keywords) {
  const s = ((additionalType || '') + ' ' + (keywords || '')).toLowerCase();
  if (s.includes('кино') || s.includes('фильм')) return 'CINEMA';
  if (s.includes('музе') || s.includes('выставк') || s.includes('галере')) return 'MUSEUM';
  if (s.includes('концерт') || s.includes('филармон') || s.includes('опера')
    || s.includes('балет') || s.includes('классика')) return 'CONCERT';
  if (s.includes('экскурс') || s.includes('лекц') || s.includes('мастер-класс')) return 'EXCURSION';
  return 'THEATRE';
}

/* Страница мероприятия: /раздел/площадка/мероприятие/ — без запроса и якоря. */
function eventPage(url) {
  if (!url) return null;
  try {
    const u = new URL(url);
    const segments = u.pathname.split('/').filter(Boolean);
    if (segments.length !== 3) return null;
    return u.origin + '/' + segments.join('/') + '/';
  } catch (e) {
    return null;
  }
}

function addressText(location) {
  const a = location && location.address;
  if (!a) return null;
  if (typeof a === 'string') return decodeEntities(a);
  return decodeEntities([a.streetAddress, a.addressLocality].filter(Boolean).join(', ')) || null;
}

async function fetchText(url, ua) {
  const res = await fetch(url, { headers: { 'User-Agent': ua, 'Accept-Language': 'ru' } });
  if (!res.ok) throw new Error('HTTP ' + res.status + ' ' + url);
  return res.text();
}

function parseListing(html, cityCode) {
  const host = CITIES[cityCode].host;
  const out = [];
  let malformed = 0;
  LD_JSON.lastIndex = 0;
  let m;
  while ((m = LD_JSON.exec(html))) {
    let node;
    try {
      node = JSON.parse(m[1].trim());
    } catch (e) {
      malformed++;
      continue;
    }
    if (!node || node['@type'] !== 'Event') continue;
    const location = node.location || {};
    const offers = node.offers || {};
    out.push({
      city: cityCode,
      title: decodeEntities(node.name),
      image: absolute(node.image, host),
      startDate: node.startDate || null,
      additionalType: node.additionalType || null,
      keywords: decodeEntities(node.keywords),
      venueName: decodeEntities(location.name),
      ldAddress: addressText(location),
      price: price(offers.price),
      rawUrl: absolute(offers.url, host)
    });
  }
  return { sessions: out, malformed };
}

async function crawl(cityCode, report) {
  const host = CITIES[cityCode].host;
  const seen = new Set();
  const sessions = [];
  let pages = 0;
  for (let page = 1; page <= MAX_PAGES; page++) {
    if (page > 1) await sleep(PAUSE_PAGE_MS);
    const html = await fetchText('https://' + host + '/pushkin-card/?page=' + page, BROWSER_UA);
    const parsed = parseListing(html, cityCode);
    if (parsed.sessions.length === 0) break;
    // Страховка: если страница повторяет уже виденное, источник отдаёт последнюю снова.
    const fresh = parsed.sessions.filter((s) => !seen.has(s.rawUrl + '|' + s.startDate));
    if (fresh.length === 0) break;
    fresh.forEach((s) => seen.add(s.rawUrl + '|' + s.startDate));
    sessions.push(...fresh);
    pages++;
    report.malformed += parsed.malformed;
  }
  report.pages = pages;
  report.sessions = sessions.length;
  return sessions;
}

/* Сеансы -> события: ближайший будущий сеанс + число будущих сеансов в листинге. */
function toEvents(sessions, report, now) {
  const byUrl = new Map();
  for (const s of sessions) {
    if (!s.title || !s.venueName || !s.startDate) { report.drop('нет названия, площадки или даты'); continue; }
    const ticketUrl = eventPage(s.rawUrl);
    if (!ticketUrl) { report.drop('ссылка не на страницу мероприятия'); continue; }
    if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/.test(s.startDate)) { report.drop('непонятная дата'); continue; }
    const startsAt = s.startDate.length === 16 ? s.startDate + ':00+03:00' : s.startDate + '+03:00';
    if (new Date(startsAt).getTime() < now) { report.drop('сеанс уже прошёл'); continue; }
    if (s.price == null) { report.drop('нет цены'); continue; }
    const category = mapCategory(s.additionalType, s.keywords);
    const limit = category === 'CINEMA' ? WALLET_LIMIT.CINEMA : WALLET_LIMIT.OTHER;
    if (s.price > limit) { report.drop('дороже кошелька'); continue; }

    const prev = byUrl.get(ticketUrl);
    if (!prev) {
      byUrl.set(ticketUrl, { ...s, ticketUrl, startsAt, category, priceRub: s.price, sessionCount: 1 });
    } else {
      prev.sessionCount++;
      if (startsAt < prev.startsAt) {
        Object.assign(prev, { startsAt, priceRub: s.price, image: s.image || prev.image });
      }
    }
  }
  return [...byUrl.values()];
}

/* Nuxt хранит состояние плоским массивом со ссылками по индексу (devalue). */
function nuxtData(html) {
  const m = /<script[^>]*id="__NUXT_DATA__"[^>]*>([\s\S]*?)<\/script>/.exec(html);
  if (!m) return null;
  const arr = JSON.parse(m[1]);
  const WRAP = new Set(['Reactive', 'ShallowReactive', 'Ref', 'ShallowRef', 'EmptyRef', 'EmptyShallowRef', 'Set', 'Map', 'Date', 'NuxtError', 'Island', 'JSON']);
  const cache = new Map();
  const at = (i) => {
    if (typeof i !== 'number' || i < 0) return undefined;
    if (cache.has(i)) return cache.get(i);
    const v = arr[i];
    if (v === null || typeof v !== 'object') { cache.set(i, v); return v; }
    if (Array.isArray(v)) {
      if (typeof v[0] === 'string' && WRAP.has(v[0])) { const r = at(v[1]); cache.set(i, r); return r; }
      const out = [];
      cache.set(i, out);
      v.forEach((x) => out.push(at(x)));
      return out;
    }
    const o = {};
    cache.set(i, o);
    for (const k of Object.keys(v)) o[k] = at(v[k]);
    return o;
  };
  return at(0);
}

async function crawlCinema(cityCode, report) {
  const slug = CINEMA_CITY[cityCode];
  const listing = nuxtData(await fetchText(CINEMA_HOST + '/' + slug + '/pushkins-card', BROWSER_UA));
  const state = (listing && listing.state) || {};
  const page = state['$s/' + slug + '/pushkins-card'];
  const movies = (page && page.items || []).filter((m) => m && m.pushkinsCard && m.slug);
  const cinemas = new Map();
  const cinemaState = state['$s' + slug + '-cinemas'];
  for (const c of (cinemaState && cinemaState.items) || []) {
    if (c && c.id != null) cinemas.set(c.id, c);
  }
  const out = [];
  for (const m of movies) {
    await sleep(PAUSE_PAGE_MS);
    const url = CINEMA_HOST + '/' + slug + '/movie/' + m.slug;
    let item;
    try {
      const data = nuxtData(await fetchText(url, BROWSER_UA));
      item = data && data.state && data.state['$s' + m.slug] && data.state['$s' + m.slug].item;
    } catch (e) {
      report.drop('страница фильма не открылась');
      continue;
    }
    if (!item || !item.pushkinsCard) { report.drop('фильм без Пушкинской карты'); continue; }
    // Сеансы по кинотеатрам: ближайший будущий и сколько их всего.
    const byCinema = new Map();
    for (const d of item.dates || []) {
      for (const c of (d && d.cinemas) || []) {
        for (const s of (c && c.shows) || []) {
          if (!s || !s.datetime || !(s.minPrice > 0)) continue;
          const startsAt = s.datetime.replace(' ', 'T') + ':00+03:00';
          if (new Date(startsAt).getTime() < Date.now()) continue;
          const prev = byCinema.get(s.cinemaId);
          if (!prev) byCinema.set(s.cinemaId, { startsAt, price: s.minPrice, count: 1 });
          else {
            prev.count++;
            if (startsAt < prev.startsAt) Object.assign(prev, { startsAt, price: s.minPrice });
          }
        }
      }
    }
    for (const [cinemaId, show] of byCinema) {
      const c = cinemas.get(cinemaId);
      if (!c || !c.latitude || !c.longitude) { report.drop('кинотеатр без координат'); continue; }
      if (show.price > WALLET_LIMIT.CINEMA) { report.drop('дороже кошелька'); continue; }
      out.push({
        city: cityCode,
        title: decodeEntities(item.name),
        image: item.vposter || m.vposter || null,
        description: decodeEntities(item.description),
        keywords: (item.genres || []).join(', '),
        venueName: decodeEntities(c.name),
        cinemaAddress: decodeEntities(String(c.address || '').replace(/^г\.\s*[^,]+,\s*/, '')),
        cinemaLat: Number(c.latitude),
        cinemaLon: Number(c.longitude),
        // Якорь различает кинотеатры: ticketUrl — ключ в ids.json.
        ticketUrl: url + '#cinema-' + cinemaId,
        startsAt: show.startsAt,
        category: 'CINEMA',
        priceRub: show.price,
        sessionCount: show.count
      });
    }
  }
  report.cinema = out.length;
  return out;
}

function nominatimAddress(hit, cityTitle) {
  const a = hit.address || {};
  if (a.road && a.house_number) return a.road + ', ' + a.house_number;
  if (a.road) return a.road;
  return cityTitle;
}

async function geocode(query, cityCode) {
  const city = CITIES[cityCode];
  const url = 'https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=ru&addressdetails=1&q='
    + encodeURIComponent(query);
  await sleep(PAUSE_NOMINATIM_MS);
  const res = await fetch(url, { headers: { 'User-Agent': NOMINATIM_UA, 'Accept-Language': 'ru' } });
  if (!res.ok) return null;
  const hits = await res.json();
  if (!Array.isArray(hits) || hits.length === 0) return null;
  const lat = Number(hits[0].lat);
  const lon = Number(hits[0].lon);
  if (!Number.isFinite(lat) || !Number.isFinite(lon)) return null;
  // Одноимённая площадка в другом городе — промах, а не находка.
  if (haversineKm(city.lat, city.lon, lat, lon) > MAX_VENUE_KM) return null;
  return {
    address: nominatimAddress(hits[0], city.title),
    lat: Number(lat.toFixed(4)),
    lon: Number(lon.toFixed(4)),
    source: 'nominatim'
  };
}

async function resolveVenues(events, venues, previous) {
  const prevAddress = new Map();
  for (const e of previous) {
    if (e.venueName && e.venueAddress) prevAddress.set((e.city || 'moscow') + '|' + e.venueName, e.venueAddress);
  }
  const missing = [];
  const keys = [...new Set(events.map((e) => e.city + '|' + e.venueName))];
  for (const key of keys) {
    if (venues[key]) continue;
    const [cityCode, name] = key.split('|');
    const cityTitle = CITIES[cityCode].title;
    const sample = events.find((e) => e.city + '|' + e.venueName === key);
    const queries = [];
    if (prevAddress.has(key)) queries.push(prevAddress.get(key) + ', ' + cityTitle);
    if (sample && sample.ldAddress) queries.push(sample.ldAddress + ', ' + cityTitle);
    queries.push(name + ', ' + cityTitle);
    let found = null;
    for (const q of queries) {
      found = await geocode(q, cityCode);
      if (found) break;
    }
    if (found) {
      venues[key] = found;
      console.log('Nominatim: ' + key + ' -> ' + found.address + ' (' + found.lat + ', ' + found.lon + ')');
    } else {
      missing.push(key);
    }
  }
  return missing;
}

function assignIds(events, ids, previous) {
  for (const e of previous) {
    if (e.ticketUrl && Number.isInteger(e.eventId) && ids[e.ticketUrl] == null) ids[e.ticketUrl] = e.eventId;
  }
  let next = Math.max(0, ...Object.values(ids)) + 1;
  for (const e of events) {
    if (ids[e.ticketUrl] == null) ids[e.ticketUrl] = next++;
    e.eventId = ids[e.ticketUrl];
    e.sessionId = e.eventId + 800;
  }
}

/*
 * Сначала события, уже бывшие в ленте (на них могут ссылаться реакции),
 * затем по кругу между площадками, чтобы одна филармония не заняла всё.
 */
function select(events, previousUrls) {
  const sorted = [...events].sort((a, b) => a.startsAt.localeCompare(b.startsAt) || a.title.localeCompare(b.title));
  const picked = sorted.filter((e) => previousUrls.has(e.ticketUrl)).slice(0, LIMIT_PER_CITY);
  const byVenue = new Map();
  for (const e of sorted) {
    if (previousUrls.has(e.ticketUrl)) continue;
    if (!byVenue.has(e.venueName)) byVenue.set(e.venueName, []);
    byVenue.get(e.venueName).push(e);
  }
  const queues = [...byVenue.keys()].sort().map((k) => byVenue.get(k));
  while (picked.length < LIMIT_PER_CITY && queues.some((q) => q.length)) {
    for (const q of queues) {
      if (picked.length >= LIMIT_PER_CITY) break;
      if (q.length) picked.push(q.shift());
    }
  }
  return picked;
}

function extOf(url) {
  const m = /\.(jpe?g|png|webp)(?:$|\?)/i.exec(url || '');
  return m ? m[1].toLowerCase() : 'jpg';
}

async function ensurePhoto(e, previousByUrl) {
  const prev = previousByUrl.get(e.ticketUrl);
  if (prev && prev.photo && existsSync(path.join(APP, prev.photo))) {
    e.photo = prev.photo;
    return true;
  }
  if (!e.image) return false;
  const slug = new URL(e.ticketUrl).pathname.split('/').filter(Boolean).pop().slice(0, 40);
  const file = slug + '-' + e.eventId + '.' + extOf(e.image);
  const target = path.join(EVENTS_DIR, file);
  if (!existsSync(target)) {
    await sleep(PAUSE_POSTER_MS);
    try {
      const res = await fetch(e.image, { headers: { 'User-Agent': BROWSER_UA } });
      if (!res.ok) throw new Error('HTTP ' + res.status);
      const bytes = Buffer.from(await res.arrayBuffer());
      if (bytes.length === 0) throw new Error('пустой файл');
      writeFileSync(target, bytes);
    } catch (err) {
      console.warn('Афиша не скачалась («' + e.title + '»): ' + err.message);
      return false;
    }
  }
  e.photo = 'assets/events/' + file;
  return true;
}

/* Данные чужие и попадают в исполняемый JS — экранируется всё, что ломает литерал. */
function q(s) {
  return "'" + String(s == null ? '' : s)
    .replace(/\\/g, '\\\\')
    .replace(/'/g, "\\'")
    .replace(/\r/g, '\\r')
    .replace(/\n/g, '\\n')
    .replace(/\u2028/g, '\\u2028')
    .replace(/\u2029/g, '\\u2029') + "'";
}

function sessionsPhrase(n) {
  if (n === 1) return 'В афише один сеанс.';
  const mod10 = n % 10;
  const mod100 = n % 100;
  const word = mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14) ? 'сеанса' : 'сеансов';
  return 'В афише ' + n + ' ' + word + '.';
}

function summaryOf(e) {
  if (e.description) return e.description + ' ' + e.venueName + ', ' + e.venueAddress + '. ' + sessionsPhrase(e.sessionCount);
  const kw = e.keywords ? e.keywords.charAt(0).toUpperCase() + e.keywords.slice(1).toLowerCase() + '. ' : '';
  const place = e.venueAddress ? e.venueName + ', ' + e.venueAddress : e.venueName;
  return kw + place + '. ' + sessionsPhrase(e.sessionCount);
}

function renderDataJs(events, today) {
  const header = `/*
 * Подборка для Mini App: настоящая афиша Пушкинской карты, Москва и Казань.
 *
 * Собрана из раздела Пушкинской карты билетного оператора (www.ticketland.ru
 * и kzn.ticketland.ru) — того же источника, который разбирает TicketlandParser
 * на бэкенде: читаются блоки schema.org/Event в JSON-LD, а не вёрстка.
 *
 * Файл не правится руками: его собирает \`node tools/feed/build-feed.mjs\`.
 * Снимок сделан ${today}; в нём только сеансы, не прошедшие на момент сборки.
 * Поле city ('moscow' | 'kazan') — город площадки; Mini App показывает ленту
 * города, ближайшего к точке отсчёта.
 *
 * Координаты площадок — tools/feed/venues.json (Nominatim или вручную,
 * точность от ±150 м до 1 км). distanceKm в файле нет: его считает app.js
 * от точки отсчёта, выбранной в боте, а без неё — от центра Москвы.
 *
 * eventId стабильны между снимками через tools/feed/ids.json: по ним сервер
 * хранит реакции. ticketUrl ведёт на страницу самого мероприятия у оператора.
 * Афиши лежат локально в assets/events/ — Mini App не ходит во внешнюю сеть;
 * источники — в CREDITS.md.
 *
 * Кино — из раздела Пушкинской карты сети «Синема 5» (cinema5.ru): у
 * Ticketland его нет. Одно событие — фильм в одном кинотеатре с ближайшим
 * сеансом; цена — минимальная на этом сеансе.
 */
const MOCK_EVENTS = [
`;
  const body = events.map((e) => [
    '  {',
    '    sessionId: ' + e.sessionId + ',',
    '    eventId: ' + e.eventId + ',',
    '    title: ' + q(e.title) + ',',
    '    summary: ' + q(summaryOf(e)) + ',',
    '    category: ' + q(e.category) + ',',
    '    city: ' + q(e.city) + ',',
    '    photo: ' + q(e.photo) + ',',
    '    posterUrl: ' + q(e.image) + ',',
    '    eventUrl: ' + q(e.ticketUrl) + ',',
    '    ticketUrl: ' + q(e.ticketUrl) + ',',
    '    venueName: ' + q(e.venueName) + ',',
    '    venueAddress: ' + q(e.venueAddress) + ',',
    '    venueLat: ' + e.venueLat.toFixed(4) + ',',
    '    venueLon: ' + e.venueLon.toFixed(4) + ',',
    '    startsAt: ' + q(e.startsAt) + ',',
    '    priceRub: ' + e.priceRub + ',',
    '    sessionCount: ' + e.sessionCount,
    '  }'
  ].join('\n')).join(',\n');
  return header + body + '\n];\n\nif (typeof module !== \'undefined\') { module.exports = MOCK_EVENTS; }\n';
}

function cell(s) {
  return String(s == null ? '' : s).replace(/\|/g, '\\|');
}

function renderCredits(events) {
  const rows = events.map((e) => '| ' + cell(path.basename(e.photo)) + ' | ' + cell(e.title) + ' | '
    + cell(e.ticketUrl) + ' | ' + cell(e.image) + ' |');
  return `# Афиши мероприятий

Файлы в этой папке — рекламные афиши мероприятий, скачанные вместе с самой
афишей со страниц билетного оператора ticketland.ru, а постеры фильмов — со
страниц кинотеатров «Синема 5» (cinema5.ru). Свободной лицензии у них
нет: права принадлежат организаторам. Копии лежат локально, потому что Mini App
обязан работать без внешней сети, и используются в некоммерческом демо со
ссылкой на страницу мероприятия у оператора.

Перед публичным запуском изображения нужно либо отдавать по \`posterUrl\` на лету,
либо согласовать с правообладателями.

| Файл | Мероприятие | Страница у оператора | Оригинал афиши |
|---|---|---|---|
${rows.join('\n')}
`;
}

function newReport() {
  const dropped = {};
  return {
    pages: 0, sessions: 0, malformed: 0, dropped,
    drop(reason) { dropped[reason] = (dropped[reason] || 0) + 1; }
  };
}

async function main() {
  const now = Date.now();
  const today = new Date(now + 3 * 3600 * 1000).toISOString().slice(0, 10);
  const previous = loadPreviousEvents();
  const previousByUrl = new Map(previous.map((e) => [e.ticketUrl, e]));
  const previousUrls = new Set(previousByUrl.keys());
  const venues = readJson(VENUES_JSON);
  const ids = readJson(IDS_JSON);

  const reports = {};
  let all = [];
  for (const cityCode of Object.keys(CITIES)) {
    reports[cityCode] = newReport();
    const sessions = await crawl(cityCode, reports[cityCode]);
    const events = toEvents(sessions, reports[cityCode], now);
    reports[cityCode].events = events.length;
    all = all.concat(events);
    await sleep(PAUSE_PAGE_MS);
    const films = await crawlCinema(cityCode, reports[cityCode]);
    // Координаты кинотеатров дал сам источник — Nominatim не нужен.
    for (const f of films) {
      venues[f.city + '|' + f.venueName] = { address: f.cinemaAddress, lat: Number(f.cinemaLat.toFixed(4)), lon: Number(f.cinemaLon.toFixed(4)), source: 'cinema5' };
    }
    all = all.concat(films);
  }

  const missing = await resolveVenues(all, venues, previous);
  writeJson(VENUES_JSON, venues);
  if (missing.length) {
    console.error('Не нашлись координаты площадок:\n  ' + missing.join('\n  '));
    console.error('Впишите их в tools/feed/venues.json вручную (source: "manual") '
      + 'или пометьте {"skip": true, "reason": "..."} и перезапустите скрипт.');
    process.exit(1);
  }

  const located = [];
  for (const e of all) {
    const v = venues[e.city + '|' + e.venueName];
    if (v.skip) { reports[e.city].drop('площадка помечена skip'); continue; }
    Object.assign(e, { venueAddress: v.address, venueLat: v.lat, venueLon: v.lon });
    located.push(e);
  }
  const skipped = Object.entries(venues).filter(([, v]) => v.skip).map(([k, v]) => k + ' (' + (v.reason || 'skip') + ')');
  if (skipped.length) console.log('Площадки со skip: ' + skipped.join('; '));

  assignIds(located, ids, previous);
  writeJson(IDS_JSON, ids);

  mkdirSync(EVENTS_DIR, { recursive: true });
  let final = [];
  for (const cityCode of Object.keys(CITIES)) {
    // Кино — сверх лимита: фильмов по карте немного, и терять их в круге площадок жалко.
    const inCity = located.filter((e) => e.city === cityCode);
    const picked = select(inCity.filter((e) => e.category !== 'CINEMA'), previousUrls)
      .concat(inCity.filter((e) => e.category === 'CINEMA'));
    for (const e of picked) {
      if (await ensurePhoto(e, previousByUrl)) final.push(e);
      else reports[cityCode].drop('афиша не скачалась');
    }
  }
  final.sort((a, b) => a.startsAt.localeCompare(b.startsAt) || a.city.localeCompare(b.city)
    || a.title.localeCompare(b.title));

  writeFileSync(DATA_JS, renderDataJs(final, today), 'utf8');
  writeFileSync(CREDITS, renderCredits(final), 'utf8');

  const used = new Set(final.map((e) => path.basename(e.photo)));
  let removed = 0;
  for (const f of readdirSync(EVENTS_DIR)) {
    if (f === 'CREDITS.md' || used.has(f)) continue;
    unlinkSync(path.join(EVENTS_DIR, f));
    removed++;
  }

  console.log('\nСводка снимка ' + today);
  for (const [cityCode, r] of Object.entries(reports)) {
    const inFeed = final.filter((e) => e.city === cityCode);
    const venuesCount = new Set(inFeed.map((e) => e.venueName)).size;
    console.log('  ' + CITIES[cityCode].title + ': страниц ' + r.pages + ', сеансов ' + r.sessions
      + ', событий ' + r.events + ', кино ' + (r.cinema || 0) + ', в ленте ' + inFeed.length + ' (площадок ' + venuesCount + ')');
    if (r.malformed) console.log('    нечитаемых блоков JSON-LD: ' + r.malformed);
    for (const [reason, n] of Object.entries(r.dropped)) console.log('    отброшено — ' + reason + ': ' + n);
  }
  const kazan = final.filter((e) => e.city === 'kazan');
  console.log('  Казань со startsAt >= ' + DEMO_DATE + ': '
    + kazan.filter((e) => e.startsAt >= DEMO_DATE).length);
  for (const o of KAZAN_ORIGINS) {
    const d = kazan.map((e) => haversineKm(o.lat, o.lon, e.venueLat, e.venueLon));
    console.log('  от «' + o.name + '»: ≤3 км ' + d.filter((x) => x <= 3).length
      + ', ≤5 км ' + d.filter((x) => x <= 5).length + ', ≤10 км ' + d.filter((x) => x <= 10).length
      + ' из ' + d.length);
  }
  console.log('  афиш удалено как неиспользуемых: ' + removed);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
