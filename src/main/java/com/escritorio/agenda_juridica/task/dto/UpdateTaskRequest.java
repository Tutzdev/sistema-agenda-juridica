package com.escritorio.agenda_juridica.task.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.escritorio.agenda_juridica.task.TaskCategory;
import com.escritorio.agenda_juridica.task.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTaskRequest(
        @NotBlank @Size(max = 160) String title,
        @Size(max = 4000) String description,
        @NotNull TaskCategory category,
        @NotNull TaskPriority priority,
        LocalDate scheduledDate,
        LocalTime scheduledTime,
        LocalDate dueDate,
        LocalTime dueTime,
        LocalDate reminderDate,
        @NotNull Long responsibleUserId) {
}
