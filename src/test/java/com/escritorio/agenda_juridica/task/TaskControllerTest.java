package com.escritorio.agenda_juridica.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escritorio.agenda_juridica.security.CustomUserDetailsService;
import com.escritorio.agenda_juridica.security.SecurityConfiguration;
import com.escritorio.agenda_juridica.shared.exception.GlobalExceptionHandler;
import com.escritorio.agenda_juridica.shared.exception.ResourceNotFoundException;
import com.escritorio.agenda_juridica.task.dto.TaskResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
@Import({SecurityConfiguration.class, GlobalExceptionHandler.class})
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void validCreateRequestReturnsCreated() throws Exception {
        when(taskService.create(any())).thenReturn(response(10L));

        mockMvc.perform(post("/api/tasks").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Preparar manifestação",
                                  "category":"DEADLINE",
                                  "priority":"HIGH",
                                  "dueDate":"2026-08-19",
                                  "responsibleUserId":2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Preparar manifestação"));
    }

    @Test
    @WithMockUser
    void invalidRequestReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/tasks").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"responsibleUserId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.category").exists());
    }

    @Test
    @WithMockUser
    void missingTaskReturnsNotFound() throws Exception {
        when(taskService.get(99L)).thenThrow(new ResourceNotFoundException("Atividade não encontrada."));

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Atividade não encontrada."));
    }

    private TaskResponse response(Long id) {
        return new TaskResponse(id, "Preparar manifestação", null, TaskCategory.DEADLINE, TaskStatus.PENDING,
                TaskPriority.HIGH, null, null, null, null, null, DeadlineAlertStatus.NONE,
                null, null, null, null, null);
    }
}
