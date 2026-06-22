package com.flowmind.rag.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkService {

    @Value("${flowmind.chunk.size}")
    private int chunkSize;

    @Value("${flowmind.chunk.overlap}")
    private int overlap;

    public List<String> split(String text) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        String normalized = text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();

        int start = 0;
        int length = normalized.length();

        while (start < length) {
            int end = Math.min(start + chunkSize, length);
            String chunk = normalized.substring(start, end).trim();

            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            if (end == length) {
                break;
            }

            start = end - overlap;

            if (start < 0) {
                start = 0;
            }
        }

        return chunks;
    }
}