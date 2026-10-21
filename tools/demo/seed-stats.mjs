/*
 * Демо-сидер для экрана статистики: на защите экран не должен быть пустым.
 *
 * Скрипт от имени ~20 выдуманных пользователей MAX (id 990100–990199) листает
 * настоящую афишу из src/main/resources/static/app/data.js и шлёт действия
 * через тот же POST /api/actions, что и Mini App: просмотры, «Пойду»,
 * «в желаемое», «не нравится», переходы к покупке, покупки и их отмены.
 * initData подписывается токеном бота — так же, как это делает MAX.
 *
 * Запуск из корня репозитория (Node 18+, npm install не нужен):
 *
 *     bash:        TOKEN=<токен бота> node tools/demo/seed-stats.mjs
 *     PowerShell:  $env:TOKEN='<токен бота>'; node tools/demo/seed-stats.mjs
 *
 * Переменные окружения:
 *  - TOKEN   — токен бота, тот же, что MAX_BOT_TOKEN у сервера. Обязателен.
 *  - BASE    — адрес сервера, по умолчанию http://127.0.0.1:8080.
 *  - USERS   — сколько пользователей, 1..99, по умолчанию 20.
 *  - SEED    — зерно генератора, по умолчанию 1: тот же SEED — тот же сценарий.
 *  - DRY_RUN — 1: ничего не отправлять (сеть и токен не нужны), только план.
 *
 * Ограничение: created_at ставит сервер, задним числом записать нельзя. Все
 * демо-действия попадают в «сейчас»: «Вернулись» для них 0 %, «Время до
 * покупки» — меньше минуты, «Активны» сегодня = за 7 дней. Повторный запуск
 * дописывает журнал, а не заменяет его.
 *
 * Удалить демо-данные:
 *
 *     docker compose exec db psql -U eventloop -d eventloop -c "DELETE FROM miniapp_action WHERE max_user_id BETWEEN 990100 AND 990199; DELETE FROM miniapp_reaction WHERE max_user_id BETWEEN 990100 AND 990199;"
 *
 * Токен и initData никуда не печатаются и в файлы не пишутся.
 */
import { createHmac } from 'node:crypto';
import { createRequire } from 'node:module';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const DATA_JS = path.resolve(HERE, '..', '..', 'src', 'main', 'resources', 'static', 'app', 'data.js');
const FIRST_ID = 990100;
const PAUSE_MS = 15;

/**
 * initData так, как его проверяет InitDataVerifier: строка проверки — пары
 * key=value по алфавиту через \n, секрет = HMAC-SHA256(ключ "WebAppData",
 * токен), hash = hex HMAC-SHA256(секрет, строка проверки).
 */
export function signInitData(token, userId, authDate) {
  const params = {
    auth_date: String(authDate),
    user: JSON.stringify({ id: userId, first_name: 'Демо ' + (userId - FIRST_ID + 1) })
  };
  const check = Object.keys(params).sort().map((k) => k + '=' + params[k]).join('\n');
  const secret = createHmac('sha256', 'WebAppData').update(token).digest();
  const hash = createHmac('sha256', secret).update(check).digest('hex');
  return new URLSearchParams({ ...params, hash }).toString();
}

// mulberry32: маленький детерминированный ГПСЧ.
function rng(seed) {
  let a = seed >>> 0;
  return function () {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function shuffle(list, rnd) {
  const a = list.slice();
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(rnd() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}

function between(rnd, lo, hi) {
  return lo + Math.floor(rnd() * (hi - lo + 1));
}

/** Сценарий одного пользователя: список [действие, событие] по порядку. */
function userScript(events, rnd) {
  const city = rnd() < 0.65 ? 'moscow' : 'kazan';
  const local = shuffle(events.filter((e) => e.city === city), rnd);
  const cats = shuffle([...new Set(local.map((e) => e.category))], rnd);
  const fav = new Set(cats.slice(0, between(rnd, 1, 2)));
  const steps = [];

  if (rnd() < 0.2) {
    // Ушёл сразу: пару карточек посмотрел и закрыл.
    local.slice(0, between(rnd, 2, 4)).forEach((e) => steps.push(['VIEW', e]));
    return { city, steps };
  }

  const liked = [];
  for (const e of local.slice(0, between(rnd, 4, 18))) {
    steps.push(['VIEW', e]);
    const r = rnd();
    const [like, save, dislike] = fav.has(e.category) ? [0.45, 0.6, 0.75] : [0.1, 0.15, 0.6];
    if (r < like) {
      steps.push(['LIKE', e]);
      liked.push(e);
    } else if (r < save) {
      steps.push(['SAVE', e]);
    } else if (r < dislike) {
      steps.push(['DISLIKE', e]);
      if (rnd() < 0.05) { steps.push(['UNDO', e]); }
    }
  }
  for (const e of liked) {
    if (rnd() >= 0.55) { continue; }
    steps.push(['CLICK_BUY', e]);
    const r = rnd();
    if (r < 0.5) {
      steps.push(['PURCHASE_YES', e]);
      if (rnd() < 0.1) { steps.push(['PURCHASE_CANCEL', e]); }
    } else if (r < 0.8) {
      steps.push(['PURCHASE_NO', e]);
    }
  }
  return { city, steps };
}

function plan(events, users, seed) {
  const rnd = rng(seed);
  const result = [];
  for (let i = 0; i < users; i++) {
    result.push({ id: FIRST_ID + i, ...userScript(events, rnd) });
  }
  return result;
}

/** Ожидаемая воронка по людям — как её посчитает сервер. */
function funnel(users) {
  const has = (u, a) => u.steps.some(([x]) => x === a);
  const bought = (u) => {
    const state = new Map();
    for (const [a, e] of u.steps) {
      if (a === 'PURCHASE_YES') { state.set(e.eventId, true); }
      if (a === 'PURCHASE_CANCEL') { state.set(e.eventId, false); }
    }
    return [...state.values()].some(Boolean);
  };
  return {
    opened: users.length,
    liked: users.filter((u) => has(u, 'LIKE')).length,
    clickBuy: users.filter((u) => has(u, 'CLICK_BUY')).length,
    bought: users.filter(bought).length
  };
}

function report(users) {
  const byType = {};
  for (const u of users) {
    for (const [a] of u.steps) { byType[a] = (byType[a] || 0) + 1; }
  }
  const total = Object.values(byType).reduce((s, n) => s + n, 0);
  const cities = users.reduce((m, u) => ({ ...m, [u.city]: (m[u.city] || 0) + 1 }), {});
  const f = funnel(users);
  console.log('Пользователей: ' + users.length + ' (id ' + users[0].id + '–' + users[users.length - 1].id + '), '
    + 'Москва: ' + (cities.moscow || 0) + ', Казань: ' + (cities.kazan || 0));
  console.log('Действий: ' + total);
  for (const [a, n] of Object.entries(byType).sort((x, y) => y[1] - x[1])) {
    console.log('  ' + a.padEnd(16) + n);
  }
  console.log('Воронка по людям: открыли ' + f.opened + ' / «Пойду» ' + f.liked
    + ' / перешли к покупке ' + f.clickBuy + ' / купили ' + f.bought);
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function fail(message) {
  console.error(message);
  process.exit(1);
}

async function main() {
  const dry = process.env.DRY_RUN === '1';
  const token = process.env.TOKEN || '';
  if (!token && !dry) {
    fail('Нужен токен бота: TOKEN=<токен> node tools/demo/seed-stats.mjs\n'
      + '(PowerShell: $env:TOKEN=\'<токен>\'; node tools/demo/seed-stats.mjs).\n'
      + 'Это тот же токен, что MAX_BOT_TOKEN в .env сервера. Без сети: DRY_RUN=1.');
  }
  const base = (process.env.BASE || 'http://127.0.0.1:8080').replace(/\/+$/, '');
  const users = Number(process.env.USERS || 20);
  if (!Number.isInteger(users) || users < 1 || users > 99) {
    fail('USERS — целое от 1 до 99: id держатся в диапазоне 990100–990199.');
  }
  const seed = Number(process.env.SEED || 1);
  if (!Number.isInteger(seed)) {
    fail('SEED — целое число.');
  }

  const events = createRequire(import.meta.url)(DATA_JS);
  const scripts = plan(events, users, seed);

  if (dry) {
    console.log('DRY_RUN: запросы не отправляются.');
    report(scripts);
    return;
  }

  const authDate = Math.floor(Date.now() / 1000);
  let errors = 0;
  for (const u of scripts) {
    const initData = signInitData(token, u.id, authDate);
    for (const [action, e] of u.steps) {
      let response;
      try {
        response = await fetch(base + '/api/actions', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Init-Data': initData },
          body: JSON.stringify({
            eventId: e.eventId, action, priceRub: e.priceRub, category: e.category, city: e.city
          })
        });
      } catch (err) {
        fail('Сервер по адресу ' + base + ' недоступен. Он запущен? Адрес задаётся переменной BASE.');
      }
      if (response.status === 401) {
        fail('Сервер отклонил подпись (401): TOKEN не совпадает с MAX_BOT_TOKEN сервера.');
      }
      if (!response.ok) {
        errors++;
        console.error('Ответ ' + response.status + ' на ' + action + ' для события ' + e.eventId);
      }
      await sleep(PAUSE_MS);
    }
  }

  report(scripts);
  if (errors) {
    fail('Ошибок: ' + errors + '.');
  }
  console.log('Готово. Открой /stats в боте — экран обновится за 3 секунды.');
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main();
}
