package com.helper.vavahelper.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.helper.vavahelper.exception.InvalidResetTokenException;
import com.helper.vavahelper.models.User.PasswordResetToken;
import com.helper.vavahelper.models.User.User;
import com.helper.vavahelper.repositories.PasswordResetTokenRepository;
import com.helper.vavahelper.repositories.UserRepository;
import com.helper.vavahelper.util.TokenHasher;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    // Intervalo minimo entre dois e-mails de reset para o mesmo usuario (evita spam na caixa de entrada).
    private static final long COOLDOWN_SECONDS = 60;

    private final PasswordResetTokenRepository tokenRepo;
    private final UserRepository userRepo;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate tx;
    private final long expiryMinutes;

    public PasswordResetService(PasswordResetTokenRepository tokenRepo,
                                UserRepository userRepo,
                                JavaMailSender mailSender,
                                PasswordEncoder passwordEncoder,
                                TransactionTemplate tx,
                                @Value("${vavahelper.password-reset.token-expiration}") long expiryMinutes) {
        this.tokenRepo = tokenRepo;
        this.userRepo = userRepo;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.tx = tx;
        this.expiryMinutes = expiryMinutes;
    }

    /**
     * Gera o token e envia o e-mail. Nunca revela se o e-mail existe: para e-mail desconhecido
     * (ou em periodo de espera) o metodo simplesmente retorna sem fazer nada.
     */
    public void createPasswordResetToken(String email, String appUrl) {
        // 1) Parte de banco, em transacao curta
        String[] result = tx.execute(status -> {
            UserDetails found = userRepo.findByLogin(email);
            if (!(found instanceof User user)) {
                return null;
            }

            LocalDateTime now = LocalDateTime.now();
            tokenRepo.deleteByExpiryDateBefore(now);

            Optional<PasswordResetToken> existing = tokenRepo.findByUser(user);
            if (existing.isPresent()) {
                LocalDateTime issuedAt = existing.get().getExpiryDate().minusMinutes(expiryMinutes);
                if (issuedAt.isAfter(now.minusSeconds(COOLDOWN_SECONDS))) {
                    return null;
                }
                tokenRepo.delete(existing.get());
                tokenRepo.flush();
            }

            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(TokenHasher.sha256Hex(rawToken));
            resetToken.setUser(user);
            resetToken.setExpiryDate(now.plusMinutes(expiryMinutes));
            tokenRepo.save(resetToken);

            return new String[] { user.getLogin(), rawToken };
        });

        if (result == null) {
            return;
        }

        // 2) Envio do e-mail fora da transacao (SMTP e lento)
        String resetLink = appUrl + "/auth/reset-password?token=" + result[1];
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(result[0]);
        mail.setSubject("Recuperação de senha");
        mail.setText("Clique no link para redefinir sua senha:\n" + resetLink);
        try {
            mailSender.send(mail);
        } catch (MailException e) {
            // Nao propaga: o cliente nao pode descobrir se o e-mail existe pela diferenca de resposta.
            log.error("Falha ao enviar e-mail de reset de senha", e);
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prt = tokenRepo.findByToken(TokenHasher.sha256Hex(token))
                .orElseThrow(InvalidResetTokenException::new);

        if (prt.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepo.delete(prt);
            throw new InvalidResetTokenException();
        }

        User user = prt.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        tokenRepo.delete(prt);
    }

    /** Roda a cada 5 minutos e remove tokens vencidos. */
    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void purgeExpiredTokens() {
        int removed = tokenRepo.deleteByExpiryDateBefore(LocalDateTime.now());
        if (removed > 0) {
            log.info("Tokens de reset expirados removidos: {}", removed);
        }
    }
}
