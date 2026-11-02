package com.escritorio.agenda_juridica.task;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

import com.escritorio.agenda_juridica.security.CurrentUserService;
import com.escritorio.agenda_juridica.shared.exception.ResourceNotFoundException;
import com.escritorio.agenda_juridica.task.dto.UpdateTaskRequest;
import com.escritorio.agenda_juridica.user.User;
import com.escritorio.agenda_juridica.user.UserRole;
import com.escritorio.agenda_juridica.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Atividade de outro usuário responde 404 em toda operação, mesmo para ADMIN. */
@ExtendWith(MockitoExtension.class)
class TaskServiceVisibilityTest {

    private static final Long TASK_OF_LEANDRO = 20L;

    @Mock private TaskRepository taskRepository;
    @Mock private UserService userService;
    @Mock private CurrentUserService currentUserService;
    @Mock private TaskMapper taskMapper;
    @Mock private DeadlineClassifier classifier;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, userService, currentUserService, taskMapper, classifier,
                Clock.systemDefaultZone());
        User hilton = new User("Hilton", "hilton@gmail.com", "hash", UserRole.ADMIN);
        ReflectionTestUtils.setField(hilton, "id", 1L);
        when(currentUserService.requireCurrentUser()).thenReturn(hilton);
        // O repositório só devolve a atividade a quem é responsável ou criador.
        when(taskRepository.findVisibleById(TASK_OF_LEANDRO, 1L)).thenReturn(Optional.empty());
    }

    @Test
    void getTaskOfAnotherUserReturnsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> taskService.get(TASK_OF_LEANDRO));
    }

    @Test
    void updateTaskOfAnotherUserReturnsNotFound() {
        UpdateTaskRequest request = new UpdateTaskRequest("Editada", null, TaskCategory.DEADLINE,
                TaskPriority.NORMAL, LocalDate.of(2026, 11, 5), null, null, null, null, 1L);

        assertThrows(ResourceNotFoundException.class, () -> taskService.update(TASK_OF_LEANDRO, request));
        verifyNoInteractions(userService);
    }

    @Test
    void changeStatusOfAnotherUserReturnsNotFound() {
        assertThrows(ResourceNotFoundException.class,
                () -> taskService.changeStatus(TASK_OF_LEANDRO, TaskStatus.COMPLETED));
    }

    @Test
    void deleteTaskOfAnotherUserReturnsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> taskService.delete(TASK_OF_LEANDRO));
        verify(taskRepository, never()).delete(any(Task.class));
    }
}
