package com.escritorio.agenda_juridica.task;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;

import com.escritorio.agenda_juridica.security.CurrentUserService;
import com.escritorio.agenda_juridica.shared.exception.BusinessRuleException;
import com.escritorio.agenda_juridica.shared.exception.ResourceNotFoundException;
import com.escritorio.agenda_juridica.task.dto.CreateTaskRequest;
import com.escritorio.agenda_juridica.task.dto.TaskResponse;
import com.escritorio.agenda_juridica.task.dto.UpdateTaskRequest;
import com.escritorio.agenda_juridica.user.User;
import com.escritorio.agenda_juridica.user.UserRole;
import com.escritorio.agenda_juridica.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORTABLE_FIELDS = Set.of("title", "category", "status", "priority",
            "scheduledDate", "scheduledTime", "dueDate", "dueTime", "reminderDate", "createdAt", "updatedAt");

    private final TaskRepository       taskRepository;
    private final UserService          userService;
    private final CurrentUserService   currentUserService;
    private final TaskMapper           taskMapper;
    private final DeadlineClassifier   classifier;
    private final Clock                clock;

    public TaskService(TaskRepository taskRepository, UserService userService, CurrentUserService currentUserService,
            TaskMapper taskMapper, DeadlineClassifier classifier, Clock clock) {

        this.taskRepository     = taskRepository;
        this.userService        = userService;
        this.currentUserService = currentUserService;
        this.taskMapper         = taskMapper;
        this.classifier         = classifier;
        this.clock              = clock;
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> list(
        LocalDate startDate, 
        LocalDate endDate, 
        LocalDate scheduledDate,
        LocalDate dueDate, 
        TaskCategory category, 
        TaskStatus status, 
        TaskPriority priority,
        Long responsibleUserId, 
        String search, 
        Pageable pageable) {validateRange(startDate, endDate);

        Pageable safePageable = safePageable(pageable, Sort.by(
                Sort.Order.asc("dueDate").nullsLast(), Sort.Order.asc("scheduledDate").nullsLast(),
                Sort.Order.desc("createdAt")));

        return taskRepository.findAll(TaskSpecifications.withFilters(
            startDate, 
            endDate, 
            scheduledDate, 
            dueDate,
            category, 
            status, 
            priority, 
            responsibleUserId, 
            search), safePageable).map(taskMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> listDay(LocalDate date, Pageable pageable) {
        return taskRepository.findAll(TaskSpecifications.scheduledBetween(date, date),
                        safePageable(pageable, Sort.by("scheduledTime")))
                .map(taskMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> listWeek(LocalDate referenceDate, Pageable pageable) {
        LocalDate monday = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return taskRepository.findAll(TaskSpecifications.scheduledBetween(monday, monday.plusDays(4)),
                        safePageable(pageable, Sort.by("scheduledDate", "scheduledTime")))
                .map(taskMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> listAlert(DeadlineAlertStatus alert, LocalDate referenceDate, Pageable pageable) {

        LocalDate date = referenceDate == null ? LocalDate.now(clock) : referenceDate;
        return taskRepository.findAll(TaskSpecifications.withAlert(alert, date, classifier.upcomingDays()),
                        safePageable(pageable, Sort.by("dueDate", "dueTime")))
                .map(task -> taskMapper.toResponse(task, date));
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return taskMapper.toResponse(findTask(id));
    }

    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        User responsible = userService.findActiveById(request.responsibleUserId());
        User creator = currentUserService.requireCurrentUser();
        Task task = new Task(
            request.title(), 
            request.description(), 
            request.category(), 
            request.priority(),
            request.scheduledDate(), 
            request.scheduledTime(), 
            request.dueDate(), 
            request.dueTime(),
            request.reminderDate(), 
            responsible, 
            creator
        );

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long id, UpdateTaskRequest request) {
        Task task = findTask(id);

        User responsible = userService.findActiveById(request.responsibleUserId());
        task.updateDetails(
            request.title(), 
            request.description(), 
            request.category(), 
            request.priority(),
            request.scheduledDate(), 
            request.scheduledTime(), 
            request.dueDate(), 
            request.dueTime(),
            request.reminderDate(), 
            responsible);

        return taskMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse changeStatus(Long id, TaskStatus status) {
        Task task = findTask(id);
        task.changeStatus(status, LocalDateTime.now(clock));

        return taskMapper.toResponse(task);
    }

    @Transactional
    public void delete(Long id) {
        Task task = findTask(id);

        User current = currentUserService.requireCurrentUser();
        boolean administrator = current.getRole() == UserRole.ADMIN;
        boolean creator = task.getCreatedBy().getId().equals(current.getId());

        if (!administrator && !creator) {
            throw new AccessDeniedException("Somente o criador ou um administrador pode excluir a atividade.");
        }
        if (task.getStatus() != TaskStatus.CANCELED) {
            throw new BusinessRuleException("Somente atividades canceladas podem ser excluídas.");
        }

        taskRepository.delete(task);
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessRuleException("A data inicial não pode ser posterior à data final.");
        }
    }

    private Pageable safePageable(Pageable pageable, Sort defaultSort) {
        Sort requestedSort = pageable.getSort();
        for (Sort.Order order : requestedSort) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BusinessRuleException("Campo de ordenação inválido: " + order.getProperty() + ".");
            }
        }
        Sort sort = requestedSort.isSorted() ? requestedSort : defaultSort;

        return PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE), sort);
    }
}
