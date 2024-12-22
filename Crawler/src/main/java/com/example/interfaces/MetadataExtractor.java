package com.example.interfaces;

import java.util.Map;

public interface MetadataExtractor {
    Map<String, String> extractMetadata(int bookId);
}