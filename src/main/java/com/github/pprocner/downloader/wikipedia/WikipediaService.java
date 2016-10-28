package com.github.pprocner.downloader.wikipedia;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

class WikipediaService {

    private static final String RANDOM_ARTICLE_URL = "https://en.wikipedia.org/wiki/Special:Random";
    private static final String ARTICLE_BASE_URL = "https://en.wikipedia.org/w/index.php";
    private static final String PRINTABLE_PARAM = "printable=yes";

    private CloseableHttpClient httpClient;

    WikipediaService(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    String getRandomArticleName() {
        HttpGet request = new HttpGet(RANDOM_ARTICLE_URL);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(request);
            Header locationHeader = response.getFirstHeader("Location");
            if (locationHeader != null) {
                return extractArticleTitle(locationHeader.getValue());
            } else {
                throw new WikipediaServiceException("Cannot get random article. Location header not found.");
            }
        } catch (IOException e) {
            throw new WikipediaServiceException("Cannot get random article.", e);
        } finally {
            closeResponseSafely(response);
        }
    }

    private void closeResponseSafely(CloseableHttpResponse response) {
        if (response != null) {
            try {
                response.close();
            } catch (IOException e) {
                System.err.println("Cannot close HTTP response.");
                e.printStackTrace();
            }
        }
    }

    private String extractArticleTitle(String articleUrl) {
        int lastSlashIndex = articleUrl.lastIndexOf('/');
        return articleUrl.substring(lastSlashIndex + 1);
    }

    String getPrintableArticle(String title) {
        String articleUrl = createPrintableArticleUrl(title);
        HttpGet request = new HttpGet(articleUrl);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            InputStream bodyStream = entity.getContent();
            return inputStreamToString(bodyStream);
        } catch (IOException e) {
            throw new WikipediaServiceException("Cannot get printable page of article '" + title + "'.", e);
        } finally {
            closeResponseSafely(response);
        }
    }

    private String inputStreamToString(InputStream inputStream) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
        return writer.toString();
    }

    private String createPrintableArticleUrl(String title) {
        return ARTICLE_BASE_URL + "?title=" + title + "&" + PRINTABLE_PARAM;
    }
}
