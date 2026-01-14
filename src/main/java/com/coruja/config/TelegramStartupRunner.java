package com.coruja.config;

import com.coruja.services.TelegramService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class TelegramStartupRunner {

    private static final Logger logger = LoggerFactory.getLogger(TelegramStartupRunner.class);

    @Bean
    // @Profile("!test") // Comente se quiser rodar nos testes também
    public CommandLineRunner runTelegramSync(TelegramService telegramService) {
        return args -> {
            logger.info("=================================================");
            logger.info("🚀 INICIANDO SINCRONIZAÇÃO AUTOMÁTICA DO TELEGRAM");
            logger.info("=================================================");

            telegramService.processarNovosUsuarios()
                    .subscribe(
                            usuarios -> {
                                logger.info("✅ FINALIZADO! Usuários encontrados: {}", usuarios.size());
                                usuarios.forEach(u -> logger.info(" -> Usuário: {} (ID: {})", u.getPrimeiroNome(), u.getTelegramId()));
                            },
                            erro -> logger.error("❌ ERRO NA SINCRONIZAÇÃO AUTOMÁTICA: ", erro)
                    );
        };
    }
}