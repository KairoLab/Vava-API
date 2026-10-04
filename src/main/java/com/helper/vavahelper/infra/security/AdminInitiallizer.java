package com.helper.vavahelper.infra.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import com.helper.vavahelper.models.User.User;
import com.helper.vavahelper.models.User.UserRole;
import com.helper.vavahelper.repositories.UserRepository;
import com.helper.vavahelper.util.PasswordPolicy;

/**
 * Cria o administrador inicial a partir de ADMIN_LOGIN / ADMIN_PASSWORD.
 * Sem ADMIN_PASSWORD nenhum admin e criado. A senha antiga "123", que versoes anteriores
 * criavam automaticamente, e tratada como credencial comprometida e removida.
 */
@Configuration
public class AdminInitiallizer {

    private static final Logger log = LoggerFactory.getLogger(AdminInitiallizer.class);
    private static final String LEGACY_DEFAULT_PASSWORD = "123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate tx;

    @Value("${app.admin.login:admin}")
    private String adminLogin;

    @Value("${app.admin.password:}")
    private String adminPassword;

    public AdminInitiallizer(UserRepository userRepository, PasswordEncoder passwordEncoder, TransactionTemplate tx) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tx = tx;
    }

    @Bean
    public CommandLineRunner createAdminUser() {
        return args -> tx.executeWithoutResult(status -> {
            boolean hasPassword = adminPassword != null && !adminPassword.isBlank();
            if (hasPassword && !PasswordPolicy.isValid(adminPassword)) {
                throw new IllegalStateException("ADMIN_PASSWORD fraca: use " + PasswordPolicy.REQUIREMENTS + ".");
            }

            UserDetails existing = userRepository.findByLogin(adminLogin);

            if (existing instanceof User user) {
                if (passwordEncoder.matches(LEGACY_DEFAULT_PASSWORD, user.getPassword())) {
                    if (hasPassword) {
                        user.setPassword(passwordEncoder.encode(adminPassword));
                        userRepository.save(user);
                        log.warn("O admin '{}' usava a senha padrao antiga e ela foi substituida por ADMIN_PASSWORD.", adminLogin);
                    } else {
                        userRepository.delete(user);
                        log.error("O admin '{}' usava a senha padrao antiga '123' e foi removido. "
                                + "Defina ADMIN_PASSWORD para recria-lo com uma senha forte.", adminLogin);
                    }
                }
                return;
            }

            if (!hasPassword) {
                log.info("ADMIN_PASSWORD nao definida: nenhum usuario admin foi criado.");
                return;
            }

            User admin = new User(adminLogin, passwordEncoder.encode(adminPassword), UserRole.ADMIN);
            userRepository.save(admin);
            log.info("Usuario admin '{}' criado.", adminLogin);
        });
    }
}
