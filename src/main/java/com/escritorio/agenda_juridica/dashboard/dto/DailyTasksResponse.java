package com.escritorio.agenda_juridica.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

import com.escritorio.agenda_juridica.task.dto.TaskResponse;

public record DailyTasksResponse(LocalDate date, List<TaskResponse> tasks) {
}
