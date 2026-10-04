package com.helper.vavahelper.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.helper.vavahelper.models.User.PasswordResetToken;
import com.helper.vavahelper.models.User.User;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    // busca pelo HASH do token enviado por e-mail
    Optional<PasswordResetToken> findByToken(String tokenHash);

    Optional<PasswordResetToken> findByUser(User user);

    // para limpar tokens expirados antes de agora (precisa rodar dentro de uma transacao)
    int deleteByExpiryDateBefore(LocalDateTime expiryDate);
}
