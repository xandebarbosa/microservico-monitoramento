package com.coruja.repositories;

import com.coruja.entities.UsuarioTelegram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioTelegramRepository extends JpaRepository<UsuarioTelegram, Long> {
    Optional<UsuarioTelegram> findByTelegramId(String telegramId);
}
