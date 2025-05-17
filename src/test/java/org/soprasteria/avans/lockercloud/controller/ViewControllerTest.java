package org.soprasteria.avans.lockercloud.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.soprasteria.avans.lockercloud.service.FileManagerService;
import org.springframework.ui.Model;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViewControllerTest {

    @Mock
    private FileManagerService fileManagerService;

    @Mock
    private Model model;

    @InjectMocks
    private ViewController controller;

    @Test
    void index_WhenFilesExist_ShouldPopulateModelAndReturnIndexView() {
        List<String> files = Arrays.asList("one.txt", "two.txt");
        when(fileManagerService.listFiles()).thenReturn(files);

        String viewName = controller.index(model);

        assertEquals("index", viewName);
        verify(fileManagerService).listFiles();
        verify(model).addAttribute("files", files);
        verifyNoMoreInteractions(fileManagerService, model);
    }

    @Test
    void index_WhenServiceReturnsEmptyList_ShouldPopulateModelWithEmptyList() {
        List<String> files = List.of();
        when(fileManagerService.listFiles()).thenReturn(files);

        String viewName = controller.index(model);

        assertEquals("index", viewName);
        verify(fileManagerService).listFiles();
        verify(model).addAttribute("files", files);
        verifyNoMoreInteractions(fileManagerService, model);
    }

    @Test
    void index_WhenServiceReturnsNull_ShouldPopulateModelWithNull() {
        when(fileManagerService.listFiles()).thenReturn(null);

        String viewName = controller.index(model);

        assertEquals("index", viewName);
        verify(fileManagerService).listFiles();
        verify(model).addAttribute("files", null);
        verifyNoMoreInteractions(fileManagerService, model);
    }

    @Test
    void index_WhenServiceThrowsException_ShouldPropagateException() {
        when(fileManagerService.listFiles()).thenThrow(new RuntimeException("error fetching files"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> controller.index(model));
        assertEquals("error fetching files", ex.getMessage());
        verify(fileManagerService).listFiles();
        verifyNoInteractions(model);
    }
}
