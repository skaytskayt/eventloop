-- Напоминание об отложенном событии за трое суток до начала. Сервер ленту из
-- app/data.js не знает (см. V8), поэтому дату и название присылает клиент
-- вместе с действием. Строки до V10 их не имеют и напоминаний не получат.
--
-- reminded_at — когда напоминание ушло: планировщик не шлёт его повторно.

ALTER TABLE miniapp_reaction
    ADD COLUMN starts_at   TIMESTAMPTZ,
    ADD COLUMN title       VARCHAR(300),
    ADD COLUMN reminded_at TIMESTAMPTZ;

CREATE INDEX miniapp_reaction_remind_idx ON miniapp_reaction (starts_at)
    WHERE kind = 'SAVED' AND reminded_at IS NULL;
