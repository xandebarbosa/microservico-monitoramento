package com.coruja;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableJpaAuditing // <-- ADICIONE ESTA ANOTAÇÃO PARA ATIVAR A AUDITORIA
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class MicroservicoMonitoramentoApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicroservicoMonitoramentoApplication.class, args);
	}

}
