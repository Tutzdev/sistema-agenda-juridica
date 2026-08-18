package com.escritorio.agenda_juridica.task.dto;

import com.escritorio.agenda_juridica.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeTaskStatusRequest(@NotNull TaskStatus status) {
}
