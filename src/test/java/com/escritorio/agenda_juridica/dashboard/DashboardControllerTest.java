package com.escritorio.agenda_juridica.dashboard;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import com.escritorio.agenda_juridica.dashboard.dto.DailyTasksResponse;
import com.escritorio.agenda_juridica.dashboard.dto.DashboardResponse;
import com.escritorio.agenda_juridica.security.CustomUserDetailsService;
import com.escritorio.agenda_juridica.security.SecurityConfiguration;
import com.escritorio.agenda_juridica.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfiguration.class, GlobalExceptionHandler.class})
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser
    void returnsDashboard() throws Exception {
        LocalDate reference = LocalDate.of(2026, 8, 18);
        DashboardResponse response = new DashboardResponse(reference, LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 8, 21), 2, 1, 3, 1, 1, 0, List.of(), List.of(), List.of(), List.of(),
                List.of(new DailyTasksResponse(LocalDate.of(2026, 8, 17), List.of())));
        when(dashboardService.get(reference, null)).thenReturn(response);

        mockMvc.perform(get("/api/dashboard").param("referenceDate", "2026-08-18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value("2026-08-17"))
                .andExpect(jsonPath("$.totalOverdue").value(1));
    }
}
