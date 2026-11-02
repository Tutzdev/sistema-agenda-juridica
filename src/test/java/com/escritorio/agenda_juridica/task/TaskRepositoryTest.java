package com.escritorio.agenda_juridica.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import com.escritorio.agenda_juridica.user.User;
import com.escritorio.agenda_juridica.user.UserRepository;
import com.escritorio.agenda_juridica.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:agenda;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class TaskRepositoryTest {

    private static final LocalDate REFERENCE = LocalDate.of(2026, 8, 18);
    private static final List<TaskStatus> ACTIVE = List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private User hilton;
    private User leandro;

    @BeforeEach
    void setUp() {
        // Os dois são ADMIN: a regra de visibilidade vale para qualquer perfil.
        hilton = userRepository.save(new User("Hilton", "hilton@example.com", "hash", UserRole.ADMIN));
        leandro = userRepository.save(new User("Leandro", "leandro@example.com", "hash", UserRole.ADMIN));
    }

    @Test
    void dashboardQueryLoadsWeeklyAndUpcomingTasks() {
        taskRepository.save(new Task("Reunião", null, TaskCategory.CLIENT_MEETING, TaskPriority.NORMAL,
                REFERENCE, null, null, null, null, hilton, hilton));
        taskRepository.save(new Task("Prazo", null, TaskCategory.DEADLINE, TaskPriority.HIGH,
                null, null, REFERENCE.plusDays(2), null, REFERENCE, hilton, hilton));

        List<Task> tasks = dashboardTasksOf(hilton);

        assertEquals(2, tasks.size());
        assertTrue(tasks.stream().allMatch(task -> task.getResponsibleUser().getName().equals("Hilton")));
    }

    @Test
    void userDoesNotSeeTasksOfAnotherUser() {
        Task ofLeandro = taskRepository.save(scheduledTask("Audiência do Leandro", leandro, leandro));
        taskRepository.save(scheduledTask("Prazo do Hilton", hilton, hilton));

        assertEquals(List.of("Prazo do Hilton"), titlesInListing(hilton));
        assertEquals(List.of("Audiência do Leandro"), titlesInListing(leandro));
        assertTrue(taskRepository.findVisibleById(ofLeandro.getId(), hilton.getId()).isEmpty());
        assertEquals(List.of("Prazo do Hilton"), dashboardTasksOf(hilton).stream().map(Task::getTitle).toList());
        assertEquals(1, taskRepository.countForDashboard(TaskStatus.PENDING, hilton.getId(), null));
    }

    @Test
    void taskCreatedForAnotherResponsibleIsVisibleToBoth() {
        // Criada pelo Hilton, com o Leandro como responsável.
        Task shared = taskRepository.save(scheduledTask("Protocolo compartilhado", leandro, hilton));

        assertEquals(List.of("Protocolo compartilhado"), titlesInListing(hilton));
        assertEquals(List.of("Protocolo compartilhado"), titlesInListing(leandro));
        assertTrue(taskRepository.findVisibleById(shared.getId(), hilton.getId()).isPresent());
        assertTrue(taskRepository.findVisibleById(shared.getId(), leandro.getId()).isPresent());
        assertEquals(1, dashboardTasksOf(hilton).size());
        assertEquals(1, dashboardTasksOf(leandro).size());
    }

    private Task scheduledTask(String title, User responsible, User creator) {
        return new Task(title, null, TaskCategory.DEADLINE, TaskPriority.NORMAL,
                REFERENCE, null, null, null, null, responsible, creator);
    }

    private List<String> titlesInListing(User viewer) {
        return taskRepository.findAll(TaskSpecifications.visibleTo(viewer.getId()), Pageable.unpaged())
                .map(Task::getTitle).getContent();
    }

    private List<Task> dashboardTasksOf(User viewer) {
        return taskRepository.findDashboardTasks(REFERENCE, LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 21),
                REFERENCE.plusDays(3), viewer.getId(), null, ACTIVE);
    }
}
