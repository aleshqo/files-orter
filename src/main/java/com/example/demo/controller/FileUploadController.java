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

import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@AllArgsConstructor
public class FileUploadController {

    private final FileProcessingService fileProcessingService;

    @GetMapping("/")
    public String uploadForm(Model model) {
        model.addAttribute("message", "Выберите файл для загрузки и обработки:");
        return "uploadForm";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {
        try {
            long startTime = System.currentTimeMillis();
            String sortedFilePath = fileProcessingService.processFile(file);
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            model.addAttribute("message", "Файл успешно обработан за " + processingTime + " мс. Вы можете скачать его ниже.");
            model.addAttribute("downloadLink", "/download?filePath=" + URLEncoder.encode(sortedFilePath, StandardCharsets.UTF_8.toString()));
        } catch (Exception e) {
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


