package com.escritorio.agenda_juridica.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@EnableConfigurationProperties(InitialAdminProperties.class)
public class InitialAdminInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitialAdminInitializer.class);

    private final UserRepository         userRepository;
    private final PasswordEncoder        passwordEncoder;
    private final InitialAdminProperties properties;

    public InitialAdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
            InitialAdminProperties properties) {

        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties      = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // A lista só semeia um banco vazio, para não desfazer trocas de e-mail/senha feitas pela tela.
        if (userRepository.count() == 0) {
            properties.seedAccounts().forEach(this::configure);
        }
        // O administrador único (app.admin.*) segue criando ou redefinindo: serve de recuperação de acesso.
        properties.recoveryAccount().ifPresent(this::configure);
    }

    private void configure(InitialAdminProperties.Account account) {
        String passwordHash = passwordEncoder.encode(account.password());

        userRepository.findByEmailIgnoreCase(account.email()).ifPresentOrElse(
                user -> {
                    user.update(account.name(), account.email(), UserRole.ADMIN, passwordHash);
                    user.setActive(true);
                    userRepository.save(user);
                    LOGGER.info("Administrador configurado atualizado para o e-mail informado.");
                },
                () -> {
                    userRepository.save(new User(account.name(), account.email(), passwordHash, UserRole.ADMIN));
                    LOGGER.info("Administrador inicial criado para o e-mail configurado.");
                });
    }
}
