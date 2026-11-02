package com.escritorio.agenda_juridica.task;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.domain.Specification;

/**
 * Visibilidade: todo usuário, inclusive ADMIN, só enxerga atividades em que é
 * o responsável ou o criador. As consultas de leitura recebem o {@code viewerId}.
 */
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @Override
    @EntityGraph(attributePaths = {"responsibleUser", "createdBy"})
    Optional<Task> findById(Long id);

    @EntityGraph(attributePaths = {"responsibleUser", "createdBy"})
    @Query("""
            select t from Task t
            where t.id = :id
              and (t.responsibleUser.id = :viewerId or t.createdBy.id = :viewerId)
            """)
    Optional<Task> findVisibleById(@Param("id") Long id, @Param("viewerId") Long viewerId);

    @Override
    @EntityGraph(attributePaths = {"responsibleUser", "createdBy"})
    Page<Task> findAll(Specification<Task> specification, Pageable pageable);

    @Query("""
            select count(t) from Task t
            where t.status = :status
              and (t.responsibleUser.id = :viewerId or t.createdBy.id = :viewerId)
              and (:responsibleUserId is null or t.responsibleUser.id = :responsibleUserId)
            """)
    long countForDashboard(@Param("status") TaskStatus status,
            @Param("viewerId") Long viewerId,
            @Param("responsibleUserId") Long responsibleUserId);

    @EntityGraph(attributePaths = {"responsibleUser", "createdBy"})
    @Query("""
            select distinct t from Task t
            where (t.responsibleUser.id = :viewerId or t.createdBy.id = :viewerId)
              and (:responsibleUserId is null or t.responsibleUser.id = :responsibleUserId)
              and (
                    t.scheduledDate between :weekStart and :weekEnd
                    or (t.status in :activeStatuses and (
                        t.dueDate <= :upcomingEnd
                        or t.reminderDate <= :referenceDate
                    ))
              )
            order by t.dueDate asc nulls last, t.scheduledDate asc nulls last, t.createdAt desc
            """)
    List<Task> findDashboardTasks(@Param("referenceDate") LocalDate referenceDate,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd,
            @Param("upcomingEnd") LocalDate upcomingEnd,
            @Param("viewerId") Long viewerId,
            @Param("responsibleUserId") Long responsibleUserId,
            @Param("activeStatuses") List<TaskStatus> activeStatuses);
}
