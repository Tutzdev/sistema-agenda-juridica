package com.escritorio.agenda_juridica.task;

import java.time.LocalDate;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> withFilters(LocalDate startDate, LocalDate endDate,
            LocalDate scheduledDate, LocalDate dueDate, TaskCategory category, TaskStatus status,
            TaskPriority priority, Long responsibleUserId, String search) {
        return Specification.allOf(
                scheduledOnOrAfter(startDate), scheduledOnOrBefore(endDate), equalDate("scheduledDate", scheduledDate),
                equalDate("dueDate", dueDate), equalValue("category", category), equalValue("status", status),
                equalValue("priority", priority), responsibleFor(responsibleUserId), containsText(search));
    }

    public static Specification<Task> scheduledBetween(LocalDate start, LocalDate end) {
        return Specification.allOf(scheduledOnOrAfter(start), scheduledOnOrBefore(end));
    }

    public static Specification<Task> withAlert(DeadlineAlertStatus alert, LocalDate referenceDate, int upcomingDays) {
        return (root, query, builder) -> {
            var active = root.get("status").in(TaskStatus.PENDING, TaskStatus.IN_PROGRESS);
            return switch (alert) {

                case OVERDUE -> builder.and(active, builder.lessThan(root.get("dueDate"), referenceDate));
                case DUE_TODAY -> builder.and(active, builder.equal(root.get("dueDate"), referenceDate));
                case UPCOMING -> builder.and(active, builder.greaterThan(root.get("dueDate"), referenceDate),

                    builder.lessThanOrEqualTo(root.get("dueDate"), referenceDate.plusDays(upcomingDays)));
                    
                default -> throw new IllegalArgumentException("Este filtro aceita OVERDUE, DUE_TODAY ou UPCOMING.");
            };
        };
    }

    private static Specification<Task> scheduledOnOrAfter(LocalDate date) {
        return (root, query, builder) -> date == null ? null
                : builder.greaterThanOrEqualTo(root.get("scheduledDate"), date);
    }

    private static Specification<Task> scheduledOnOrBefore(LocalDate date) {
        return (root, query, builder) -> date == null ? null
                : builder.lessThanOrEqualTo(root.get("scheduledDate"), date);
    }

    private static Specification<Task> equalDate(String field, LocalDate date) {
        return (root, query, builder) -> date == null ? null : builder.equal(root.get(field), date);
    }

    private static <T> Specification<Task> equalValue(String field, T value) {
        return (root, query, builder) -> value == null ? null : builder.equal(root.get(field), value);
    }

    private static Specification<Task> responsibleFor(Long id) {
        return (root, query, builder) -> id == null ? null : builder.equal(root.get("responsibleUser").get("id"), id);
    }

    private static Specification<Task> containsText(String search) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(search)) {
                return null;
            }
            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";

            return builder.or(builder.like(builder.lower(root.get("title")), pattern),
                    builder.like(builder.lower(root.get("description")), pattern));
        };
    }
}
