package com.escritorio.agenda_juridica.task.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.escritorio.agenda_juridica.task.DeadlineAlertStatus;
import com.escritorio.agenda_juridica.task.TaskCategory;
import com.escritorio.agenda_juridica.task.TaskPriority;
import com.escritorio.agenda_juridica.task.TaskStatus;

public record TaskResponse(Long id, String title, String description, TaskCategory category, TaskStatus status,
        TaskPriority priority, LocalDate scheduledDate, LocalTime scheduledTime, LocalDate dueDate,
        LocalTime dueTime, LocalDate reminderDate, DeadlineAlertStatus deadlineAlertStatus,
        UserSummaryResponse responsibleUser, UserSummaryResponse createdBy, LocalDateTime completedAt,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
