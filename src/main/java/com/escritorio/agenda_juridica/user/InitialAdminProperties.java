package com.escritorio.agenda_juridica.user;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Administradores configurados na subida.
 * {@code app.admins[n].*}: contas iniciais, criadas só quando o banco ainda não tem usuários.
 * {@code app.admin.*}: administrador de recuperação, criado ou redefinido a cada subida.
 */
@ConfigurationProperties("app")
public record InitialAdminProperties(Account admin, List<Account> admins) {

    public record Account(String name, String email, String password) {

        boolean isComplete() {
            return StringUtils.hasText(name) && StringUtils.hasText(email) && StringUtils.hasText(password);
        }
    }

    public List<Account> seedAccounts() {
        return admins == null ? List.of()
                : admins.stream().filter(account -> account != null && account.isComplete()).toList();
    }

    public Optional<Account> recoveryAccount() {
        return Optional.ofNullable(admin).filter(Account::isComplete);
    }
}
