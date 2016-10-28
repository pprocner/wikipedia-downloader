package com.github.pprocner.downloader.wikipedia;

import org.apache.commons.io.FileUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final String HTML_EXTENSION = ".html";

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java -jar wikipedia-downloader.jar <article-count> <output-dir>");
        }
        int articleCount = Integer.valueOf(args[0]);
        String outputDir = args[1];

        CloseableHttpClient httpClient = null;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            httpClient = HttpClientBuilder.create().disableRedirectHandling().build();
            WikipediaService wikipediaService = new WikipediaService(httpClient);
            for (int article = 0; article < articleCount; article++) {
                Runnable task = new DownloadRandomPrintableArticleTask(wikipediaService, outputDir);
                executorService.submit(task);
            }
            shutdownExecutorService(executorService);
        } finally {
            closeClientSafely(httpClient);
        }
    }

    private static void shutdownExecutorService(ExecutorService executorService) {
        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class DownloadRandomPrintableArticleTask implements Runnable {

        private WikipediaService wikipediaService;
        private String outputDir;

        private DownloadRandomPrintableArticleTask(WikipediaService wikipediaService, String outputDir) {
            this.wikipediaService = wikipediaService;
            this.outputDir = outputDir;
        }

        @Override
        public void run() {
            try {
                String articleTitle = wikipediaService.getRandomArticleName();
                System.out.println("Downloading '" + articleTitle + "' ...");
                String articleHtml = wikipediaService.getPrintableArticle(articleTitle);
                saveArticleToFile(outputDir, articleTitle, articleHtml);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private void saveArticleToFile(String outputDir, String articleTitle, String articleHtml) {
            String fileName = Paths.get(outputDir, articleTitle + HTML_EXTENSION).toString();
            try {
                FileUtils.writeStringToFile(new File(fileName), articleHtml, StandardCharsets.UTF_8);
            } catch (IOException e) {
                System.err.println("Cannot write article '" + articleTitle + "' to file.");
                e.printStackTrace();
            }
        }
    }

    private static void closeClientSafely(CloseableHttpClient client) {
        if(client != null) {
            try {
                client.close();
            } catch (IOException e) {
                System.err.println("Cannot close HTTP client.");
                e.printStackTrace();
            }
        }
    }
}
