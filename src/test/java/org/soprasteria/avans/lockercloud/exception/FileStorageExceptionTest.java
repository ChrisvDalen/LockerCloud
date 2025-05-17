package org.soprasteria.avans.lockercloud.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageExceptionTest {

    @Test
    void constructor_WithMessage_ShouldSetMessageAndNullCause() {
        String msg = "Storage error occurred";
        FileStorageException ex = new FileStorageException(msg);

        assertEquals(msg, ex.getMessage(), "Exception message should match the provided message");
        assertNull(ex.getCause(), "Cause should be null when only message constructor is used");
        assertTrue(ex instanceof RuntimeException, "Should be a RuntimeException");
    }

    @Test
    void constructor_WithNullMessage_ShouldAllowNullMessage() {
        FileStorageException ex = new FileStorageException((String) null);

        assertNull(ex.getMessage(), "Message should be null when constructed with null");
        assertNull(ex.getCause(), "Cause should remain null");
    }

    @Test
    void constructor_WithMessageAndCause_ShouldSetMessageAndCause() {
        String msg = "Failed to store file";
        Throwable cause = new IllegalArgumentException("Invalid file");
        FileStorageException ex = new FileStorageException(msg, cause);

        assertEquals(msg, ex.getMessage(), "Exception message should match the provided message");
        assertSame(cause, ex.getCause(), "Cause should be the same instance provided");
    }

    @Test
    void constructor_WithNullCause_ShouldAllowNullCause() {
        String msg = "Something went wrong";
        FileStorageException ex = new FileStorageException(msg, null);

        assertEquals(msg, ex.getMessage(), "Message should match even if cause is null");
        assertNull(ex.getCause(), "Cause should be null when constructed with null cause");
    }
}
