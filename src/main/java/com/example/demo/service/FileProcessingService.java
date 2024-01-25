package com.example.demo.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

@Service
@Slf4j
public class FileProcessingService {

    @Value("${file.storage-path}")
    private String storagePath;

    @SneakyThrows
    public List<String> sortData(MultipartFile multipartFile) {
        File inputFile = convertToFile(multipartFile);

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            ForkJoinPool forkJoinPool = new ForkJoinPool();
            return forkJoinPool.invoke(new MergeSortTask(lines));
        }
    }

    @SneakyThrows
    public File writeDataToFile(List<String> sortedData, String fileName) {
        File outputFile = new File(storagePath + "sorted_" + fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (String sortedLine : sortedData) {
                writer.write(sortedLine);
                writer.newLine();
            }
        }
        return outputFile;
    }

    private File convertToFile(MultipartFile multipartFile) throws IOException {
        File file = new File(storagePath + Objects.requireNonNull(multipartFile.getOriginalFilename()));
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(multipartFile.getBytes());
        }
        return file;
    }

    private static class MergeSortTask extends RecursiveTask<List<String>> {
        private final List<String> list;

        MergeSortTask(List<String> list) {
            this.list = list;
        }

        @Override
        protected List<String> compute() {
            if (list.size() <= 1) {
                return list;
            }

            int middleIndex = list.size() / 2;
            MergeSortTask leftTask = new MergeSortTask(list.subList(0, middleIndex));
            MergeSortTask rightTask = new MergeSortTask(list.subList(middleIndex, list.size()));

            leftTask.fork();
            List<String> rightResult = rightTask.compute();
            List<String> leftResult = leftTask.join();

            return merge(leftResult, rightResult);
        }

        private List<String> merge(List<String> left, List<String> right) {
            int leftIndex = 0, rightIndex = 0;
            List<String> merged = new ArrayList<>(left.size() + right.size());

            while (leftIndex < left.size() && rightIndex < right.size()) {
                if (left.get(leftIndex).compareTo(right.get(rightIndex)) <= 0) {
                    merged.add(left.get(leftIndex++));
                } else {
                    merged.add(right.get(rightIndex++));
                }
            }

            while (leftIndex < left.size()) {
                merged.add(left.get(leftIndex++));
            }

            while (rightIndex < right.size()) {
                merged.add(right.get(rightIndex++));
            }

            return merged;
        }
    }
}
