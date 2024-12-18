package com.example.control;

import com.example.interfaces.*;

public class GutenbergCrawlerSequential implements GutenbergCrawler {
    private final BookManager bookManager;
    private final LastIdManager lastIdManager;
    private final int maxDownloadAttempts;

    public GutenbergCrawlerSequential(BookManager bookManager, LastIdManager lastIdManager, int maxDownloadAttempts) {
        this.bookManager = bookManager;
        this.lastIdManager = lastIdManager;
        this.maxDownloadAttempts = maxDownloadAttempts;
    }

    @Override
    public void startCrawling(int numBooks) {
        int startId = lastIdManager.getLastDownloadedId() + 1;

        for (int i = startId; i < startId + numBooks; i++) {
            boolean success = false;
            int attempts = 0;

            while (!success && attempts < maxDownloadAttempts) {
                success = bookManager.handleBook(i);
                attempts++;
            }

            if (!success) {
                System.out.println("Failed download for book with ID: " + i + " after " + maxDownloadAttempts + " attempts.");
            }
        }

        lastIdManager.updateLastDownloadedId(startId + numBooks - 1);
    }
}