package com.maxbot.eventLoop.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Запуск импорта афиши.
 *
 * <p>Эндпоинт закрыт токеном из конфигурации. Если токен не задан, импорт
 * по сети недоступен вовсе — иначе любой, кто дотянется до порта приложения,
 * смог бы дёргать внешний источник от нашего имени.
 */
@RestController
public class ImportController {

    private static final Logger log = LoggerFactory.getLogger(ImportController.class);

    private final EventImportService importer;
    private final String adminToken;

    public ImportController(EventImportService importer,
                            @Value("${eventloop.admin-token:}") String adminToken) {
        this.importer = importer;
        this.adminToken = adminToken;
    }

    @PostMapping("/admin/import")
    public ResponseEntity<?> importFromSource(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestParam(defaultValue = "5") int pages) {
        if (!authorized(token)) {
            return ResponseEntity.status(403).body("нет доступа");
        }
        log.info("Запрошен импорт афиши, страниц: {}", pages);
        return ResponseEntity.ok(importer.importFromSource(Math.clamp(pages, 1, 40)));
    }

    /** Разбор заранее сохранённой страницы — когда источник закрылся антиботом. */
    @PostMapping("/admin/import/html")
    public ResponseEntity<?> importFromHtml(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestBody String html) {
        if (!authorized(token)) {
            return ResponseEntity.status(403).body("нет доступа");
        }
        return ResponseEntity.ok(importer.importFromHtml(html));
    }

    private boolean authorized(String token) {
        return adminToken != null && !adminToken.isBlank() && adminToken.equals(token);
    }
}
