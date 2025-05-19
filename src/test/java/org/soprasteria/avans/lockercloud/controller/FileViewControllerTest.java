package org.soprasteria.avans.lockercloud.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.soprasteria.avans.lockercloud.service.FileManagerService;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileViewControllerTest {

    @Mock
    private FileManagerService fileManagerService;

    @Mock
    private Model model;

    @InjectMocks
    private FileViewController controller;

    @Test
    void showCloudDirectory_WhenFilesExist_ShouldPopulateModelAndReturnView() {
        List<String> files = Arrays.asList("file1.txt", "file2.txt");
        when(fileManagerService.listFiles()).thenReturn(files);

        String viewName = controller.showCloudDirectory(model);

        assertEquals("showCloudDirectory", viewName);
        verify(fileManagerService).listFiles();
        verify(model).addAttribute("files", files);
        verifyNoMoreInteractions(model, fileManagerService);
    }

    @Test
    void showCloudDirectory_WhenServiceReturnsNull_ShouldPopulateModelWithNullAndReturnView() {
        when(fileManagerService.listFiles()).thenReturn(null);

        String viewName = controller.showCloudDirectory(model);

        assertEquals("showCloudDirectory", viewName);
        verify(fileManagerService).listFiles();
        verify(model).addAttribute("files", null);
        verifyNoMoreInteractions(model, fileManagerService);
    }

    @Test
    void showCloudDirectory_WhenServiceThrowsException_ShouldPropagate() {
        when(fileManagerService.listFiles())
                .thenThrow(new RuntimeException("service failed"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            controller.showCloudDirectory(model);
        });
        assertEquals("service failed", ex.getMessage());
        verify(fileManagerService).listFiles();
        verifyNoInteractions(model);
    }
}
