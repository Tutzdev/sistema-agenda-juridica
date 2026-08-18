package com.escritorio.agenda_juridica.task;

import java.time.LocalDate;

import com.escritorio.agenda_juridica.task.dto.ChangeTaskStatusRequest;
import com.escritorio.agenda_juridica.task.dto.CreateTaskRequest;
import com.escritorio.agenda_juridica.task.dto.TaskResponse;
import com.escritorio.agenda_juridica.task.dto.UpdateTaskRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public Page<TaskResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            @RequestParam(required = false) TaskCategory category,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long responsibleUserId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return taskService.list(startDate, endDate, scheduledDate, dueDate, category, status, priority,
                responsibleUserId, search, pageable);
    }

    @GetMapping("/day")
    public Page<TaskResponse> day(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20) Pageable pageable) {
        return taskService.listDay(date, pageable);
    }

    @GetMapping("/week")
    public Page<TaskResponse> week(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate,
            @PageableDefault(size = 20) Pageable pageable) {
        return taskService.listWeek(referenceDate, pageable);
    }

    @GetMapping("/alerts/{alert}")
    public Page<TaskResponse> alerts(@PathVariable DeadlineAlertStatus alert,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate,
            @PageableDefault(size = 20) Pageable pageable) {
        return taskService.listAlert(alert, referenceDate, pageable);
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id) { return taskService.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody CreateTaskRequest request) { return taskService.create(request); }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse status(@PathVariable Long id, @Valid @RequestBody ChangeTaskStatusRequest request) {
        return taskService.changeStatus(id, request.status());
    }

    @PostMapping("/{id}/complete")
    public TaskResponse complete(@PathVariable Long id) { return taskService.changeStatus(id, TaskStatus.COMPLETED); }

    @PostMapping("/{id}/reopen")
    public TaskResponse reopen(@PathVariable Long id) { return taskService.changeStatus(id, TaskStatus.PENDING); }

    @PostMapping("/{id}/cancel")
    public TaskResponse cancel(@PathVariable Long id) { return taskService.changeStatus(id, TaskStatus.CANCELED); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { taskService.delete(id); }
}
