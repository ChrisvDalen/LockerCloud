package org.soprasteria.avans.lockercloud.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.soprasteria.avans.lockercloud.dto.SyncResult;
import org.soprasteria.avans.lockercloud.service.FileManagerService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncControllerTest {

    @Mock
    private FileManagerService fileManagerService;

    @InjectMocks
    private SyncController controller;

    @Test
    void triggerSync_WhenServiceReturnsCompletedFuture_ShouldReturnSameResult() {
        SyncResult expected = new SyncResult();
        when(fileManagerService.syncLocalClientFilesAsync())
                .thenReturn(CompletableFuture.completedFuture(expected));

        CompletableFuture<SyncResult> future = controller.triggerSync();

        assertNotNull(future, "Future should not be null");
        assertFalse(future.isCompletedExceptionally(), "Future should complete normally");
        assertSame(expected, future.join(), "Returned SyncResult should match expected");
    }

    @Test
    void triggerSync_WhenServiceThrowsImmediately_ShouldPropagateException() {
        when(fileManagerService.syncLocalClientFilesAsync())
                .thenThrow(new RuntimeException("immediate failure"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> controller.triggerSync());
        assertEquals("immediate failure", ex.getMessage());
    }

    @Test
    void triggerSync_WhenFutureCompletesExceptionally_ShouldCompleteExceptionally() {
        CompletableFuture<SyncResult> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("async failure"));
        when(fileManagerService.syncLocalClientFilesAsync()).thenReturn(failed);

        CompletableFuture<SyncResult> future = controller.triggerSync();

        assertTrue(future.isCompletedExceptionally(), "Future should complete exceptionally");
        CompletionException ex = assertThrows(CompletionException.class, future::join);
        assertTrue(ex.getCause() instanceof IllegalStateException);
        assertEquals("async failure", ex.getCause().getMessage());
    }
}
