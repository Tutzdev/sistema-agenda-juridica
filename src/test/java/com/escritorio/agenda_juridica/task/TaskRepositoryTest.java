package com.escritorio.agenda_juridica.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import com.escritorio.agenda_juridica.user.User;
import com.escritorio.agenda_juridica.user.UserRepository;
import com.escritorio.agenda_juridica.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
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

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void dashboardQueryLoadsWeeklyAndUpcomingTasks() {
        User user = userRepository.save(new User("Ana", "ana@example.com", "hash", UserRole.USER));
        LocalDate reference = LocalDate.of(2026, 8, 18);
        taskRepository.save(new Task("Reunião", null, TaskCategory.CLIENT_MEETING, TaskPriority.NORMAL,
                reference, null, null, null, null, user, user));
        taskRepository.save(new Task("Prazo", null, TaskCategory.DEADLINE, TaskPriority.HIGH,
                null, null, reference.plusDays(2), null, reference, user, user));

        List<Task> tasks = taskRepository.findDashboardTasks(reference, LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 8, 21), reference.plusDays(3), null,
                List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS));

        assertEquals(2, tasks.size());
        assertTrue(tasks.stream().allMatch(task -> task.getResponsibleUser().getName().equals("Ana")));
    }
}
