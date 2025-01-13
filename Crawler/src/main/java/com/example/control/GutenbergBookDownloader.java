package com.example.control;

import com.example.interfaces.BookDownloader;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.util.Timeout;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.URI;

public class GutenbergBookDownloader implements BookDownloader {
    private final String saveDir;

    public GutenbergBookDownloader(String saveDir) {
        this.saveDir = saveDir;
    }

    @Override
    public void downloadBook(int bookId) throws IOException {
        String url = "https://www.gutenberg.org/ebooks/" + bookId;
        Document doc = Jsoup.connect(url).timeout(60000).get();

        String textLink = getTextLink(doc);

        if (textLink != null) {
            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(org.apache.hc.client5.http.config.RequestConfig.custom()
                            .setConnectTimeout(Timeout.ofMinutes(10))
                            .setResponseTimeout(Timeout.ofMinutes(10))
                            .build())
                    .build()) {

                HttpGet httpGet = new HttpGet(URI.create(textLink));
                String bookFileName = saveDir + "/" + bookId + ".txt";
                File file = new File(bookFileName);

                File dir = new File(saveDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                    int status = response.getCode();
                    if (status == HttpStatus.SC_OK) {
                        try (InputStream in = response.getEntity().getContent();
                             FileOutputStream out = new FileOutputStream(file)) {

                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                        }
                    } else {
                        throw new HttpResponseException(status, "The book could not be downloaded.");
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("The book with ID " + bookId + " has no text file available.");
        }
    }

    private static String getTextLink(Document doc) {
        Element link = doc.select("a[href]").stream()
                .filter(a -> a.text().equals("Plain Text UTF-8"))
                .findFirst()
                .orElse(null);
        if (link != null) {
            return "https://www.gutenberg.org" + link.attr("href");
        }
        return null;
    }
}
