package com.escritorio.agenda_juridica.task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.escritorio.agenda_juridica.shared.exception.BusinessRuleException;
import com.escritorio.agenda_juridica.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "tasks")
public class Task {

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS = allowedTransitions();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TaskCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "scheduled_time")
    private LocalTime scheduledTime;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "due_time")
    private LocalTime dueTime;

    @Column(name = "reminder_date")
    private LocalDate reminderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsible_user_id", nullable = false)
    private User responsibleUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Task() {
    }

    public Task(String title, String description, TaskCategory category, TaskPriority priority,
            LocalDate scheduledDate, LocalTime scheduledTime, LocalDate dueDate, LocalTime dueTime,
            LocalDate reminderDate, User responsibleUser, User createdBy) {
        updateDetails(title, description, category, priority, scheduledDate, scheduledTime, dueDate, dueTime,
                reminderDate, responsibleUser);
        this.createdBy = createdBy;
        this.status = TaskStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void updateDetails(String title, String description, TaskCategory category, TaskPriority priority,
            LocalDate scheduledDate, LocalTime scheduledTime, LocalDate dueDate, LocalTime dueTime,
            LocalDate reminderDate, User responsibleUser) {

        validateDates(scheduledDate, scheduledTime, dueDate, dueTime, reminderDate);

        this.title           =  title.trim();
        this.description     =  description == null || description.isBlank() ? null : description.trim();
        this.category        =  category;
        this.priority        =  priority;
        this.scheduledDate   =  scheduledDate;
        this.scheduledTime   =  scheduledTime;
        this.dueDate         =  dueDate;
        this.dueTime         =  dueTime;
        this.reminderDate    =  reminderDate;
        this.responsibleUser =  responsibleUser;
    }

    public void changeStatus(TaskStatus newStatus, LocalDateTime now) {
        if (newStatus == status) {
            return;
        }
        if (!ALLOWED_TRANSITIONS.get(status).contains(newStatus)) {
            throw new BusinessRuleException("Transição de status inválida: " + status + " para " + newStatus + ".");
        }

        status = newStatus;
        completedAt = newStatus == TaskStatus.COMPLETED ? now : null;
    }

    private static void validateDates(LocalDate scheduledDate, LocalTime scheduledTime, LocalDate dueDate,
            LocalTime dueTime, LocalDate reminderDate) {

        if (scheduledTime != null && scheduledDate == null) {
            throw new BusinessRuleException("O horário agendado exige uma data agendada.");
        }
        if (dueTime != null && dueDate == null) {
            throw new BusinessRuleException("O horário limite exige uma data limite.");
        }
        if (reminderDate != null && dueDate == null) {
            throw new BusinessRuleException("A data de lembrete exige uma data limite.");
        }
        if (reminderDate != null && reminderDate.isAfter(dueDate)) {
            throw new BusinessRuleException("A data de lembrete não pode ser posterior à data limite.");
        }
    }

    private static Map<TaskStatus, Set<TaskStatus>> allowedTransitions() {
        Map<TaskStatus, Set<TaskStatus>> transitions = new EnumMap<>(TaskStatus.class);
        
        transitions.put(TaskStatus.PENDING, EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED, TaskStatus.CANCELED));
        transitions.put(TaskStatus.IN_PROGRESS, EnumSet.of(TaskStatus.PENDING, TaskStatus.COMPLETED, TaskStatus.CANCELED));
        transitions.put(TaskStatus.COMPLETED, EnumSet.of(TaskStatus.PENDING));
        transitions.put(TaskStatus.CANCELED, EnumSet.of(TaskStatus.PENDING));

        return Map.copyOf(transitions);
    }

    public Long getId()                     { return id; }
    public String getTitle()                { return title; }
    public String getDescription()          { return description; }
    public TaskCategory getCategory()       { return category; }
    public TaskStatus getStatus()           { return status; }
    public LocalDate getScheduledDate()     { return scheduledDate; }
    public LocalTime getScheduledTime()     { return scheduledTime; }
    public LocalDate getDueDate()           { return dueDate; }
    public LocalTime getDueTime()           { return dueTime; }
    public LocalDate getReminderDate()      { return reminderDate; }
    public TaskPriority getPriority()       { return priority; }
    public User getResponsibleUser()        { return responsibleUser; }
    public User getCreatedBy()              { return createdBy; }
    public LocalDateTime getCompletedAt()   { return completedAt; }
    public LocalDateTime getCreatedAt()     { return createdAt; }
    public LocalDateTime getUpdatedAt()     { return updatedAt; }
}
