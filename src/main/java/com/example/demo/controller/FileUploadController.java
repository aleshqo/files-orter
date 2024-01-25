package com.example.demo.controller;

import com.example.demo.service.FileProcessingService;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@AllArgsConstructor
public class FileUploadController {

    private final FileProcessingService fileProcessingService;

    @GetMapping("/")
    public String uploadForm(Model model) {
        model.addAttribute("processed", false); // Добавляем эту строку
        model.addAttribute("message", "Выберите файл для загрузки и обработки:");
        return "uploadForm";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {
        try {
            long startSortTime = System.currentTimeMillis();
            List<String> sortedData = fileProcessingService.sortData(file);
            long sortElapsed = System.currentTimeMillis() - startSortTime;

            long startCreateFileTime = System.currentTimeMillis();
            File sortedFile = fileProcessingService.writeDataToFile(sortedData, file.getOriginalFilename());
            long createFileElapsed = System.currentTimeMillis() - startCreateFileTime;

            long fullTime = System.currentTimeMillis() - startSortTime;

            String message = String.format(""" 
                            Время сортировки: %s мс.
                            Время записи файла: %s мс.
                            Общее время: %s мс.
                            Файл вы можете скачать ниже.
                            """,
                    sortElapsed, createFileElapsed, fullTime
            );
            model.addAttribute("message", message);
            model.addAttribute("processed", true);
            model.addAttribute("downloadLink", "/download?filePath=" + sortedFile.getPath());
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("processed", false);
            model.addAttribute("message", "Ошибка при обработке файла: " + e.getMessage());
        }
        return "uploadForm";
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filePath) {
        try {
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("Не удалось прочитать файл: " + filePath);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Ошибка при скачивании файла: " + filePath, e);
        }
    }
}


