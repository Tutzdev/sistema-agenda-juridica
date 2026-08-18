package com.escritorio.agenda_juridica.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escritorio.agenda_juridica.security.CustomUserDetailsService;
import com.escritorio.agenda_juridica.security.SecurityConfiguration;
import com.escritorio.agenda_juridica.shared.exception.GlobalExceptionHandler;
import com.escritorio.agenda_juridica.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({SecurityConfiguration.class, GlobalExceptionHandler.class})
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "USER")
    void regularUserCannotCreateUsers() throws Exception {
        mockMvc.perform(createRequest()).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administratorCanCreateUsers() throws Exception {
        when(userService.create(any())).thenReturn(new UserResponse(2L, "Ana", "ana@example.com",
                UserRole.USER, true, null, null));
        mockMvc.perform(createRequest()).andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder createRequest() {
        return post("/api/users").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Ana","email":"ana@example.com","password":"senha-segura","role":"USER"}
                """);
    }
}
