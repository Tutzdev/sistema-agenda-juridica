package com.escritorio.agenda_juridica.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

import com.escritorio.agenda_juridica.task.dto.TaskResponse;

public record DashboardResponse(
        LocalDate referenceDate,
        LocalDate weekStart,
        LocalDate weekEnd,
        long totalPending,
        long totalInProgress,
        long totalCompleted,
        long totalOverdue,
        long totalDueToday,
        long totalUpcoming,
        List<TaskResponse> overdueTasks,
        List<TaskResponse> dueTodayTasks,
        List<TaskResponse> upcomingTasks,
        List<TaskResponse> reminderActiveTasks,
        List<DailyTasksResponse> weeklyTasks) {
}
