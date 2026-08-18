package com.escritorio.agenda_juridica.task;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.escritorio.agenda_juridica.user.User;
import com.escritorio.agenda_juridica.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeadlineClassifierTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 18);
    private DeadlineClassifier classifier;
    private User user;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-18T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        classifier = new DeadlineClassifier(clock, 3);
        user = new User("Ana", "ana@example.com", "hash", UserRole.USER);
    }

    @Test
    void classifiesOverdue() {
        assertEquals(DeadlineAlertStatus.OVERDUE, classifier.classify(task(TODAY.minusDays(1), null)));
    }

    @Test
    void classifiesDueToday() {
        assertEquals(DeadlineAlertStatus.DUE_TODAY, classifier.classify(task(TODAY, null)));
    }

    @Test
    void classifiesUpcoming() {
        assertEquals(DeadlineAlertStatus.UPCOMING, classifier.classify(task(TODAY.plusDays(3), null)));
    }

    @Test
    void classifiesActiveReminderOutsideUpcomingWindow() {
        assertEquals(DeadlineAlertStatus.REMINDER_ACTIVE,
                classifier.classify(task(TODAY.plusDays(8), TODAY.minusDays(1))));
    }

    @Test
    void completedTaskHasNoAlert() {
        Task task = task(TODAY.minusDays(1), null);
        task.changeStatus(TaskStatus.COMPLETED, LocalDateTime.of(2026, 8, 18, 9, 0));
        assertEquals(DeadlineAlertStatus.NONE, classifier.classify(task));
    }

    @Test
    void canceledTaskHasNoAlert() {
        Task task = task(TODAY.minusDays(1), null);
        task.changeStatus(TaskStatus.CANCELED, LocalDateTime.of(2026, 8, 18, 9, 0));
        assertEquals(DeadlineAlertStatus.NONE, classifier.classify(task));
    }

    private Task task(LocalDate dueDate, LocalDate reminderDate) {
        return new Task("Prazo", null, TaskCategory.DEADLINE, TaskPriority.NORMAL, TODAY, null,
                dueDate, null, reminderDate, user, user);
    }
}
