package com.example.interfaces;

import java.io.IOException;

public interface BookDownloader {
    void downloadBook(int bookId) throws IOException;
}