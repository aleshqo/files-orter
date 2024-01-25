package com.example.demo.service;

import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
public class FileProcessingService {

    private static final int CHUNK_SIZE = 100_000; // Размер чанка для сортировки
    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors(); // Количество потоков

    public String processFile(MultipartFile multipartFile) throws IOException {
        File inputFile = convertMultiPartToFile(multipartFile);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<File> sortedChunks = splitAndSortFile(inputFile, executor);
        File outputFile = mergeSortedFiles(sortedChunks);
        executor.shutdown();
        return outputFile.getAbsolutePath();
    }

    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }

    private List<File> splitAndSortFile(File inputFile, ExecutorService executor) throws IOException {
        List<File> sortedFiles = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                if (lines.size() >= CHUNK_SIZE) {
                    sortedFiles.add(sortAndSave(lines, executor));
                    lines = new ArrayList<>();
                }
            }
            if (!lines.isEmpty()) {
                sortedFiles.add(sortAndSave(lines, executor));
            }
        }
        return sortedFiles;
    }

    @SneakyThrows
    private File sortAndSave(List<String> lines, ExecutorService executor) {
        Future<File> future = executor.submit(() -> {
            List<String> words = lines.stream()
                    .flatMap(line -> Arrays.stream(line.split("\\s+"))) // Разделение строк на слова
                    .sorted() // Сортировка слов
                    .collect(Collectors.toList());

            File tempFile = File.createTempFile("sorted_chunk", ".txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                for (String word : words) {
                    writer.write(word);
                    writer.newLine();
                }
            }
            return tempFile;
        });
        return future.get();
    }

    @SneakyThrows
    private File mergeSortedFiles(List<File> sortedFiles) {
        File outputFile = new File("sorted_output.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            PriorityQueue<BufferedReader> queue = new PriorityQueue<>(Comparator.comparing(bufferedReader -> {
                try {
                    return bufferedReader.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Ошибка mergeSortedFiles");
                }
            }));
            for (File file : sortedFiles) {
                queue.add(new BufferedReader(new FileReader(file)));
            }
            while (!queue.isEmpty()) {
                BufferedReader reader = queue.poll();
                String line = reader.readLine();
                if (line != null) {
                    writer.write(line);
                    writer.newLine();
                    queue.add(reader);
                } else {
                    reader.close();
                }
            }
        }
        return outputFile;
    }
}
