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
class HomeControllerTest {

    @Mock
    private FileManagerService fileManagerService;

    @Mock
    private Model model;

    @InjectMocks
    private HomeController controller;

    @Test
    void home_WhenFilesExist_ShouldPopulateModelAndReturnIndex() {
        List<String> files = Arrays.asList("a.txt", "b.txt");
        when(fileManagerService.listFiles()).thenReturn(files);

        String view = controller.home(model);

        assertEquals("index", view);
        verify(fileManagerService).listFiles();
        verify(model).addAttribute("files", files);
        verifyNoMoreInteractions(fileManagerService, model);
    }

    @Test
    void home_WhenServiceReturnsNull_ShouldPopulateModelWithNullAndReturnIndex() {
        when(fileManagerService.listFiles()).thenReturn(null);

        String view = controller.home(model);

        assertEquals("index", view);
        verify(fileManagerService).listFiles();
        verify(model).addAttribute("files", null);
        verifyNoMoreInteractions(fileManagerService, model);
    }

    @Test
    void home_WhenServiceThrowsException_ShouldPropagate() {
        when(fileManagerService.listFiles())
                .thenThrow(new RuntimeException("boom"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> controller.home(model));
        assertEquals("boom", ex.getMessage());
        verify(fileManagerService).listFiles();
        verifyNoInteractions(model);
    }
}
