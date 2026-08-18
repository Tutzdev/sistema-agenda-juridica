package com.escritorio.agenda_juridica.task;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeadlineClassifier {

    private final Clock clock;
    private final int upcomingDays;

    public DeadlineClassifier(Clock clock, @Value("${app.dashboard.upcoming-days:3}") int upcomingDays) {
        if (upcomingDays < 0) {
            throw new IllegalArgumentException("app.dashboard.upcoming-days não pode ser negativo.");
        }
        this.clock = clock;
        this.upcomingDays = upcomingDays;
    }

    public DeadlineAlertStatus classify(Task task) {
        return classify(task, LocalDate.now(clock));
    }

    public DeadlineAlertStatus classify(Task task, LocalDate referenceDate) {
        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.CANCELED) {
            return DeadlineAlertStatus.NONE;
        }
        LocalDate dueDate = task.getDueDate();
        if (dueDate != null && dueDate.isBefore(referenceDate)) {
            return DeadlineAlertStatus.OVERDUE;
        }
        if (referenceDate.equals(dueDate)) {
            return DeadlineAlertStatus.DUE_TODAY;
        }
        if (dueDate != null && !dueDate.isAfter(referenceDate.plusDays(upcomingDays))) {
            return DeadlineAlertStatus.UPCOMING;
        }
        if (task.getReminderDate() != null && !task.getReminderDate().isAfter(referenceDate)) {
            return DeadlineAlertStatus.REMINDER_ACTIVE;
        }
        if (task.getScheduledDate() != null) {
            return DeadlineAlertStatus.SCHEDULED;
        }
        return DeadlineAlertStatus.NONE;
    }

    public int upcomingDays() {
        return upcomingDays;
    }
}
