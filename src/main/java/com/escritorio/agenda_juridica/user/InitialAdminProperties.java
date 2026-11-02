package com.escritorio.agenda_juridica.user;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Administradores criados (ou redefinidos) na subida.
 * Aceita o formato legado {@code app.admin.*} e a lista {@code app.admins[n].*}.
 */
@ConfigurationProperties("app")
public record InitialAdminProperties(Account admin, List<Account> admins) {

    public record Account(String name, String email, String password) {

        boolean isComplete() {
            return StringUtils.hasText(name) && StringUtils.hasText(email) && StringUtils.hasText(password);
        }
    }

    public List<Account> completeAccounts() {
        List<Account> accounts = new ArrayList<>();
        if (admin != null) {
            accounts.add(admin);
        }
        if (admins != null) {
            accounts.addAll(admins);
        }
        return accounts.stream().filter(account -> account != null && account.isComplete()).toList();
    }
}
