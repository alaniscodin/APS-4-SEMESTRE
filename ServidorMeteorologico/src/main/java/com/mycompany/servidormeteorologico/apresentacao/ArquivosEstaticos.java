package com.mycompany.servidormeteorologico.apresentacao;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Entrega a página (HTML, CSS, JS) guardada em src/main/resources/web.
 *
 * Página e API saem do mesmo endereço (http://localhost:8080), por isso o
 * navegador não bloqueia as chamadas da página à API (sem problema de CORS).
 */
public class ArquivosEstaticos implements HttpHandler
{
    private static final String PASTA = "/web";

    /**
     * Só arquivos com estas extensões são servidos. Além de definir o
     * Content-Type, isto impede que um caminho de pasta devolva a listagem
     * do diretório.
     */
    private static final Map<String, String> TIPOS = Map.of(
            "html", "text/html; charset=UTF-8",
            "css", "text/css; charset=UTF-8",
            "js", "text/javascript; charset=UTF-8",
            "svg", "image/svg+xml",
            "png", "image/png",
            "ico", "image/x-icon");

    @Override
    public void handle(HttpExchange troca) throws IOException
    {
        try
        {
            String caminho = troca.getRequestURI().getPath();

            if (caminho.equals("/"))
            {
                caminho = "/index.html";
            }

            // Bloqueia "../" para que ninguém leia arquivos fora da pasta web
            if (caminho.contains(".."))
            {
                enviarTexto(troca, 400, "Caminho inválido.");
                return;
            }

            String tipo = TIPOS.get(extensao(caminho));

            if (tipo == null)
            {
                enviarTexto(troca, 404, "Arquivo não encontrado: " + caminho);
                return;
            }

            try (InputStream arquivo = ArquivosEstaticos.class.getResourceAsStream(PASTA + caminho))
            {
                if (arquivo == null)
                {
                    enviarTexto(troca, 404, "Arquivo não encontrado: " + caminho);
                    return;
                }

                byte[] conteudo = arquivo.readAllBytes();
                troca.getResponseHeaders().set("Content-Type", tipo);
                // Durante o desenvolvimento, sempre buscar a versão atual do arquivo
                troca.getResponseHeaders().set("Cache-Control", "no-cache");
                troca.sendResponseHeaders(200, conteudo.length);

                try (OutputStream saida = troca.getResponseBody())
                {
                    saida.write(conteudo);
                }
            }
        }
        finally
        {
            troca.close();
        }
    }

    private String extensao(String caminho)
    {
        int ponto = caminho.lastIndexOf('.');

        return ponto < 0 ? "" : caminho.substring(ponto + 1).toLowerCase();
    }

    private void enviarTexto(HttpExchange troca, int status, String texto) throws IOException
    {
        byte[] corpo = texto.getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        troca.sendResponseHeaders(status, corpo.length);

        try (OutputStream saida = troca.getResponseBody())
        {
            saida.write(corpo);
        }
    }
}
