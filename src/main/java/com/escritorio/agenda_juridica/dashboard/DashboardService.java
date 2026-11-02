package com.escritorio.agenda_juridica.dashboard;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.escritorio.agenda_juridica.dashboard.WeekCalculator.WorkWeek;
import com.escritorio.agenda_juridica.dashboard.dto.DailyTasksResponse;
import com.escritorio.agenda_juridica.dashboard.dto.DashboardResponse;
import com.escritorio.agenda_juridica.security.CurrentUserService;
import com.escritorio.agenda_juridica.task.DeadlineAlertStatus;
import com.escritorio.agenda_juridica.task.DeadlineClassifier;
import com.escritorio.agenda_juridica.task.Task;
import com.escritorio.agenda_juridica.task.TaskMapper;
import com.escritorio.agenda_juridica.task.TaskRepository;
import com.escritorio.agenda_juridica.task.TaskStatus;
import com.escritorio.agenda_juridica.task.dto.TaskResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final TaskRepository        taskRepository;
    private final DeadlineClassifier    classifier;
    private final TaskMapper            taskMapper;
    private final WeekCalculator        weekCalculator;
    private final Clock                 clock;
    private final CurrentUserService    currentUserService;

    public DashboardService(TaskRepository taskRepository, DeadlineClassifier classifier, TaskMapper taskMapper,
            WeekCalculator weekCalculator, Clock clock, CurrentUserService currentUserService) {
        this.taskRepository = taskRepository;
        this.classifier = classifier;
        this.taskMapper = taskMapper;
        this.weekCalculator = weekCalculator;
        this.clock = clock;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse get(LocalDate requestedDate, Long responsibleUserId) {

        LocalDate referenceDate = requestedDate == null ? LocalDate.now(clock) : requestedDate;
        WorkWeek week = weekCalculator.calculate(referenceDate);
        // Cada usuário (inclusive ADMIN) vê no painel só as atividades em que é responsável ou criador.
        Long viewerId = currentUserService.requireCurrentUser().getId();

        List<Task> tasks = taskRepository.findDashboardTasks(referenceDate, week.start(), week.end(),
                referenceDate.plusDays(classifier.upcomingDays()), viewerId, responsibleUserId,
                List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS));

        Map<DeadlineAlertStatus, List<TaskResponse>> alerts = classify(tasks, referenceDate);
        List<DailyTasksResponse> weeklyTasks = groupWeek(tasks, week, referenceDate);

        return new DashboardResponse(referenceDate, week.start(), week.end(),
                taskRepository.countForDashboard(TaskStatus.PENDING, viewerId, responsibleUserId),
                taskRepository.countForDashboard(TaskStatus.IN_PROGRESS, viewerId, responsibleUserId),
                taskRepository.countForDashboard(TaskStatus.COMPLETED, viewerId, responsibleUserId),
                alerts.get(DeadlineAlertStatus.OVERDUE).size(),
                alerts.get(DeadlineAlertStatus.DUE_TODAY).size(),
                alerts.get(DeadlineAlertStatus.UPCOMING).size(),
                alerts.get(DeadlineAlertStatus.OVERDUE), alerts.get(DeadlineAlertStatus.DUE_TODAY),
                alerts.get(DeadlineAlertStatus.UPCOMING), alerts.get(DeadlineAlertStatus.REMINDER_ACTIVE), weeklyTasks);
    }

    private Map<DeadlineAlertStatus, List<TaskResponse>> classify(List<Task> tasks, LocalDate referenceDate) {
        Map<DeadlineAlertStatus, List<TaskResponse>> result = new EnumMap<>(DeadlineAlertStatus.class);

        for (DeadlineAlertStatus status : DeadlineAlertStatus.values()) {
            result.put(status, new ArrayList<>());
        }
        for (Task task : tasks) {
            DeadlineAlertStatus status = classifier.classify(task, referenceDate);
            result.get(status).add(taskMapper.toResponse(task, referenceDate));
        }

        return result;
    }

    private List<DailyTasksResponse> groupWeek(List<Task> tasks, WorkWeek week, LocalDate referenceDate) {
        List<DailyTasksResponse> days = new ArrayList<>(5);

        for (LocalDate date = week.start(); !date.isAfter(week.end()); date = date.plusDays(1)) {

            LocalDate currentDate = date;

            List<TaskResponse> scheduled = tasks.stream()
                    .filter(task -> currentDate.equals(task.getScheduledDate()))
                    .map(task -> taskMapper.toResponse(task, referenceDate))
                    .toList();
            days.add(new DailyTasksResponse(date, scheduled));
        }
        
        return List.copyOf(days);
    }
}
