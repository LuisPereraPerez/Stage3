package com.example.interfaces;


import java.io.IOException;

public interface ProcessedBooks {
    boolean isProcessed(String bookId);
    void markAsProcessed(String bookId) throws IOException;
}

