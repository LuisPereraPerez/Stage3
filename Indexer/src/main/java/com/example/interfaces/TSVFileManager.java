package com.example.interfaces;
import java.util.List;

public interface TSVFileManager {
    List<String> readLines(String bookFilePath);

    public void saveWordsToFile(String word, String bookId, int paragraphIndex, int count);
}

