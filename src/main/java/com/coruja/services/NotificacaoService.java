package com.coruja.services;

public interface NotificacaoService {
    /**
     * Envia uma mensagem de texto.
     * @param mensagem O texto a ser enviado.
     * @param destinatario O número ou ID de destino (pode ser null se for um grupo padrão).
     */
    void enviarMensagem(String mensagem, String destinatario);
}
