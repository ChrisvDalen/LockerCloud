package org.soprasteria.avans.lockercloud.exception;
/**
 * Custom exception class for handling file storage related errors.
 */
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

