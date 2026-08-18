package com.escritorio.agenda_juridica.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class InitialAdminInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitialAdminInitializer.class);

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String          name;
    private final String          email;
    private final String          password;

    public InitialAdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
            @Value("${app.admin.name:}") String name,
            @Value("${app.admin.email:}") String email,
            @Value("${app.admin.password:}") String password) {

        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.name            = name;
        this.email           = email;
        this.password        = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() != 0 || !hasCompleteConfiguration()) {
            return;
        }
        userRepository.save(new User(name, email, passwordEncoder.encode(password), UserRole.ADMIN));
        LOGGER.info("Administrador inicial criado para o e-mail configurado.");
    }

    private boolean hasCompleteConfiguration() {
        return StringUtils.hasText(name) && StringUtils.hasText(email) && StringUtils.hasText(password);
    }
}
