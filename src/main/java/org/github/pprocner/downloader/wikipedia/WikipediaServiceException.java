package org.github.pprocner.downloader.wikipedia;

public class WikipediaServiceException extends RuntimeException {

    public WikipediaServiceException(String message, Exception cause) {
        super(message, cause);
    }

    public WikipediaServiceException(String message) {
        super(message);
    }
}
