-- Вопрос про баланс разделился надвое, и состояние AWAITING_BALANCE заменено
-- на AWAITING_CINEMA и AWAITING_OTHER.
--
-- Строка «AWAITING_BALANCE» в колонке переживает деплой и падает уже при
-- чтении пользователя: такого значения enum не знает. Ломается не старт,
-- а первое сообщение конкретного человека.
--
-- Кто ждал вопроса про баланс, начинает с кино. Кто дошёл до READY, там и
-- остаётся: суммы у него уже записаны.
UPDATE app_user
SET dialog_state = 'AWAITING_CINEMA'
WHERE dialog_state NOT IN ('NEW', 'AWAITING_CINEMA', 'AWAITING_OTHER', 'READY');
