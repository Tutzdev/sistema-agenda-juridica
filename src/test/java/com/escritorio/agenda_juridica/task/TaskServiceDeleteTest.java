package com.escritorio.agenda_juridica.task;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import com.escritorio.agenda_juridica.security.CurrentUserService;
import com.escritorio.agenda_juridica.shared.exception.BusinessRuleException;
import com.escritorio.agenda_juridica.user.User;
import com.escritorio.agenda_juridica.user.UserRole;
import com.escritorio.agenda_juridica.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TaskServiceDeleteTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 2, 10, 0);

    @Mock private TaskRepository taskRepository;
    @Mock private UserService userService;
    @Mock private CurrentUserService currentUserService;
    @Mock private TaskMapper taskMapper;
    @Mock private DeadlineClassifier classifier;

    private TaskService taskService;
    private User admin;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, userService, currentUserService, taskMapper, classifier,
                Clock.systemDefaultZone());
        admin = new User("Hilton", "hilton@gmail.com", "hash", UserRole.ADMIN);
        ReflectionTestUtils.setField(admin, "id", 1L);
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
    }

    @Test
    void deletesCompletedTask() {
        Task task = taskWithStatus(TaskStatus.COMPLETED);

        taskService.delete(10L);

        verify(taskRepository).delete(task);
    }

    @Test
    void deletesCanceledTask() {
        Task task = taskWithStatus(TaskStatus.CANCELED);

        taskService.delete(10L);

        verify(taskRepository).delete(task);
    }

    @Test
    void doesNotDeleteOpenTask() {
        taskWithStatus(TaskStatus.PENDING);

        assertThrows(BusinessRuleException.class, () -> taskService.delete(10L));
        verify(taskRepository, never()).delete(any(Task.class));
    }

    private Task taskWithStatus(TaskStatus status) {
        Task task = new Task("Prazo", null, TaskCategory.DEADLINE, TaskPriority.NORMAL, null, null,
                LocalDate.of(2026, 11, 5), null, null, admin, admin);
        if (status != TaskStatus.PENDING) {
            task.changeStatus(status, NOW);
        }
        when(taskRepository.findVisibleById(10L, 1L)).thenReturn(Optional.of(task));
        return task;
    }
}
