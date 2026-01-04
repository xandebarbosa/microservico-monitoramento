package com.coruja.services;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class WhatsappService extends EvolutionWhatsappService {

    public WhatsappService(WebClient.Builder webClientBuilder) {
        super(webClientBuilder);
    }

    // Se precisar chamar a API externa, injete o Feign Client ou RestTemplate aqui
    // private final EvolutionClient evolutionClient;

    @Override
    public void enviarMensagem(String numero, String texto) {
        //Lógica especifica para enivar via Evolution API
        System.out.println("Enviando WhatsApp para " + numero + ": " + texto);
    }


    public void verificarStatusInstancia() {
        // Lógica para checar saúde da instância
    }
}
