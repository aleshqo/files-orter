package com.example.demo.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;

@Slf4j
@Component
public class FileCleanupScheduler {

    private static final int TIME = 300000;

    @Value("${file.storage-path}")
    private String storagePath;

    @Scheduled(fixedRate = TIME) // 300000 мс = 5 минут
    public void cleanup() {
        File folder = new File(storagePath);
        File[] files = folder.listFiles();

        if (files != null) {
            long now = System.currentTimeMillis();
            Arrays.stream(files).forEach(file -> {
                if (now - file.lastModified() > TIME) { // Проверяем, был ли файл изменен более 5 минут назад
                    if (!file.delete()) {
                        log.error("Не удалось удалить файл: " + file.getAbsolutePath());
                    }
                }
            });
        }
    }
}


