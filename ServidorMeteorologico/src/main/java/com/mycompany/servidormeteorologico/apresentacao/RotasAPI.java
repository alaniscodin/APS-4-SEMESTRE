package com.mycompany.servidormeteorologico.apresentacao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.mycompany.servidormeteorologico.modelo.CondicaoTempo;
import com.mycompany.servidormeteorologico.modelo.Controle;
import com.mycompany.servidormeteorologico.modelo.Leitura;
import com.mycompany.servidormeteorologico.modelo.ResumoDia;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * API em JSON consumida pela página (estacao.js). Só leitura, só GET.
 *
 *   GET /api/leituras/ultima       a leitura mais recente
 *   GET /api/leituras?limite=60    as últimas N leituras, da mais antiga à mais nova (gráfico)
 *   GET /api/resumo                totais de hoje: leituras, leituras com chuva, mínima e máxima
 *
 * Erros voltam como {"erro": "mensagem"} com o código HTTP adequado:
 *   400 parâmetro inválido · 404 sem dados ou rota inexistente · 405 método não é GET
 *   503 banco indisponível · 500 erro inesperado
 * Assim a página sabe distinguir "o coletor ainda não gravou nada" de "o banco caiu".
 */
public class RotasAPI implements HttpHandler
{
    /**
     * Datas no formato ISO sem fração de segundo (2026-10-17T21:30:05).
     * O SQL Server guarda até 7 casas decimais, que o JavaScript não precisa
     * e nem todos os navegadores aceitam ao converter para Date.
     */
    private static final ObjectMapper JSON = criarConversor();

    private static ObjectMapper criarConversor()
    {
        JavaTimeModule datas = new JavaTimeModule();
        datas.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));

        return new ObjectMapper()
                .registerModule(datas)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void handle(HttpExchange troca) throws IOException
    {
        try
        {
            if (!troca.getRequestMethod().equals("GET"))
            {
                responderErro(troca, 405, "Método não permitido: esta API só aceita GET.");
                return;
            }

            String caminho = troca.getRequestURI().getPath();
            Controle controle = new Controle();

            switch (caminho)
            {
                case "/api/leituras/ultima" ->
                {
                    Leitura leitura = controle.ultimaLeitura();
                    responder(troca, controle, leitura == null ? null : comCondicao(leitura));
                }
                case "/api/leituras" ->
                {
                    List<Leitura> leituras = controle.historico(parametro(troca, "limite"));
                    responder(troca, controle, leituras == null ? null : comCondicao(leituras));
                }
                case "/api/resumo" ->
                {
                    ResumoDia resumo = controle.resumoDeHoje();
                    responder(troca, controle, resumo);
                }
                default -> responderErro(troca, 404, "Rota não encontrada: " + caminho);
            }
        }
        catch (RuntimeException e)
        {
            // Um erro numa requisição não pode derrubar o servidor
            System.err.println("[ERRO] " + troca.getRequestURI() + ": " + e);
            responderErro(troca, 500, "Erro interno no servidor: " + e.getMessage());
        }
        finally
        {
            troca.close();
        }
    }

    /**
     * Envia o resultado, ou o erro registrado no Controle se ele veio null.
     */
    private void responder(HttpExchange troca, Controle controle, Object resultado) throws IOException
    {
        if (resultado != null)
        {
            enviar(troca, 200, JSON.writeValueAsBytes(resultado));
            return;
        }

        int status = switch (controle.getFalha())
        {
            case PARAMETRO_INVALIDO -> 400;
            case SEM_DADOS -> 404;
            case BANCO_INDISPONIVEL -> 503;
            case NENHUMA -> 500;
        };

        responderErro(troca, status, controle.getMensagem());
    }

    /**
     * Converte a leitura em JSON e acrescenta a descrição do código WMO.
     * A entidade continua idêntica à do coletor; o texto só existe na resposta.
     */
    private ObjectNode comCondicao(Leitura leitura)
    {
        ObjectNode no = JSON.valueToTree(leitura);
        CondicaoTempo condicao = CondicaoTempo.porCodigo(leitura.getCodigoTempo());

        no.put("condicao", condicao == null ? null : condicao.getDescricao());
        no.put("icone", condicao == null ? null : condicao.getIcone());
        return no;
    }

    private ArrayNode comCondicao(List<Leitura> leituras)
    {
        ArrayNode lista = JSON.createArrayNode();

        for (Leitura leitura : leituras)
        {
            lista.add(comCondicao(leitura));
        }

        return lista;
    }

    private void responderErro(HttpExchange troca, int status, String mensagem) throws IOException
    {
        ObjectNode corpo = JSON.createObjectNode().put("erro", mensagem);
        enviar(troca, status, JSON.writeValueAsBytes(corpo));
    }

    private void enviar(HttpExchange troca, int status, byte[] corpo) throws IOException
    {
        troca.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        // Dados mudam a cada 10 s: o navegador não deve reaproveitar resposta antiga
        troca.getResponseHeaders().set("Cache-Control", "no-store");
        troca.sendResponseHeaders(status, corpo.length);

        try (OutputStream saida = troca.getResponseBody())
        {
            saida.write(corpo);
        }
    }

    /**
     * Lê um parâmetro da URL (?nome=valor).
     *
     * @return o valor, ou null se o parâmetro não veio.
     */
    private String parametro(HttpExchange troca, String nome)
    {
        String consulta = troca.getRequestURI().getRawQuery();

        if (consulta == null)
        {
            return null;
        }

        for (String par : consulta.split("&"))
        {
            int igual = par.indexOf('=');
            String chave = igual < 0 ? par : par.substring(0, igual);

            if (URLDecoder.decode(chave, StandardCharsets.UTF_8).equals(nome))
            {
                return igual < 0 ? "" : URLDecoder.decode(par.substring(igual + 1), StandardCharsets.UTF_8);
            }
        }

        return null;
    }
}
