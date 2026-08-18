package com.escritorio.agenda_juridica.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.escritorio.agenda_juridica.shared.exception.BusinessRuleException;
import com.escritorio.agenda_juridica.user.User;
import com.escritorio.agenda_juridica.user.UserRole;
import org.junit.jupiter.api.Test;

class TaskTest {

    private final User user = new User("Ana", "ana@example.com", "hash", UserRole.USER);

    @Test
    void completionSetsCompletedAtAndReopeningClearsIt() {
        Task task = validTask();
        LocalDateTime completionTime = LocalDateTime.of(2026, 8, 18, 10, 30);

        task.changeStatus(TaskStatus.COMPLETED, completionTime);
        assertEquals(completionTime, task.getCompletedAt());
        assertNotNull(task.getCompletedAt());

        task.changeStatus(TaskStatus.PENDING, completionTime.plusHours(1));
        assertNull(task.getCompletedAt());
    }

    @Test
    void rejectsReminderAfterDueDate() {
        assertThrows(BusinessRuleException.class, () -> new Task("Prazo", null, TaskCategory.DEADLINE,
                TaskPriority.NORMAL, null, null, LocalDate.of(2026, 8, 18), null,
                LocalDate.of(2026, 8, 19), user, user));
    }

    private Task validTask() {
        return new Task("Prazo", null, TaskCategory.DEADLINE, TaskPriority.NORMAL, null, null,
                LocalDate.of(2026, 8, 18), null, LocalDate.of(2026, 8, 17), user, user);
    }
}
