package com.example.control;

import com.example.interfaces.GutenbergCrawler;
import com.example.interfaces.LastIdManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GutenbergCrawlerParallel implements GutenbergCrawler {
    private final BookManager bookManager;
    private final LastIdManager lastIdManager;
    private final int maxDownloadAttempts;
    private final int threadPoolSize;

    public GutenbergCrawlerParallel(BookManager bookManager, LastIdManager lastIdManager, int maxDownloadAttempts, int threadPoolSize) {
        this.bookManager = bookManager;
        this.lastIdManager = lastIdManager;
        this.maxDownloadAttempts = maxDownloadAttempts;
        this.threadPoolSize = threadPoolSize;
    }

    @Override
    public void startCrawling(int numBooks) {
        int startId = lastIdManager.getLastDownloadedId() + 1;

        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

        for (int i = startId; i < startId + numBooks; i++) {
            int bookId = i;
            executorService.submit(() -> handleBookWithRetries(bookId));
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.MINUTES)) {
                System.err.println("Timeout: some tasks did not finish within the limit.");
            }
        } catch (InterruptedException e) {
            System.err.println("Execution interrupted: " + e.getMessage());
        }

        lastIdManager.updateLastDownloadedId(startId + numBooks - 1);
    }

    private void handleBookWithRetries(int bookId) {
        boolean success = false;
        int attempts = 0;

        while (!success && attempts < maxDownloadAttempts) {
            success = bookManager.handleBook(bookId);
            attempts++;
        }

        if (!success) {
            System.out.println("Failed download for book with ID: " + bookId + " after " + maxDownloadAttempts + " attempts.");
        }
    }
}