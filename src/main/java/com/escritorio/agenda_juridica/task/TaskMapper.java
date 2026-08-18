package com.escritorio.agenda_juridica.task;

import java.time.LocalDate;

import com.escritorio.agenda_juridica.task.dto.TaskResponse;
import com.escritorio.agenda_juridica.task.dto.UserSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    private final DeadlineClassifier classifier;

    public TaskMapper(DeadlineClassifier classifier) {
        this.classifier = classifier;
    }

    public TaskResponse toResponse(Task task) {
        return toResponse(task, classifier.classify(task));
    }

    public TaskResponse toResponse(Task task, LocalDate referenceDate) {
        return toResponse(task, classifier.classify(task, referenceDate));
    }

    private TaskResponse toResponse(Task task, DeadlineAlertStatus alertStatus) {
        return new TaskResponse(
            task.getId(), task.getTitle(),
            task.getDescription(), 
            task.getCategory(),
            task.getStatus(), 
            task.getPriority(), 
            task.getScheduledDate(), 
            task.getScheduledTime(),
            task.getDueDate(), 
            task.getDueTime(), 
            task.getReminderDate(), 
            alertStatus,

            new UserSummaryResponse(
                task.getResponsibleUser().getId(), 
                task.getResponsibleUser().getName()),
            new UserSummaryResponse(
                task.getCreatedBy().getId(), 
                task.getCreatedBy().getName()),

            task.getCompletedAt(), 
            task.getCreatedAt(), 
            task.getUpdatedAt());
    }
}
