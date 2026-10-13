package com.mycompany.coletormeteorologico.DAL;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mycompany.coletormeteorologico.modelo.Leitura;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Locale;

/**
 * Fonte de dados real: consulta a API Open-Meteo (https://open-meteo.com).
 * Não exige chave de acesso. Uso gratuito para fins não comerciais.
 */
public class OpenMeteoAPI implements FonteDados
{
    public static final String ORIGEM = "API";

    private static final String URL_BASE = "https://api.open-meteo.com/v1/forecast";
    private static final String VARIAVEIS =
            "temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,"
            + "wind_direction_10m,surface_pressure,apparent_temperature,weather_code";

    // Menor que o intervalo de coleta (10 s), para uma chamada lenta não atropelar a próxima
    private static final Duration TEMPO_LIMITE_CONEXAO = Duration.ofSeconds(5);
    private static final Duration TEMPO_LIMITE_RESPOSTA = Duration.ofSeconds(8);

    private final double latitude;
    private final double longitude;
    private final HttpClient cliente;
    private final ObjectMapper conversorJson;
    private String mensagem = "";

    public OpenMeteoAPI(double latitude, double longitude)
    {
        this.latitude = latitude;
        this.longitude = longitude;

        this.cliente = HttpClient.newBuilder()
                .connectTimeout(TEMPO_LIMITE_CONEXAO)
                .build();

        this.conversorJson = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public Leitura obterLeitura()
    {
        mensagem = "";

        try
        {
            HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(montarUrl()))
                    .timeout(TEMPO_LIMITE_RESPOSTA)
                    .GET()
                    .build();

            HttpResponse<String> resposta = cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());

            if (resposta.statusCode() != 200)
            {
                mensagem = "A API respondeu com erro HTTP " + resposta.statusCode()
                        + ". Corpo: " + resposta.body();
                return null;
            }

            return converter(resposta.body());
        }
        catch (HttpTimeoutException e)
        {
            mensagem = "Tempo esgotado ao consultar a API: " + e.getMessage();
        }
        catch (ConnectException e)
        {
            mensagem = "Não foi possível conectar à API (verifique a internet): " + e.getMessage();
        }
        catch (IOException e)
        {
            mensagem = "Erro de comunicação com a API: " + e.getMessage();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            mensagem = "Consulta à API interrompida.";
        }
        catch (IllegalArgumentException e)
        {
            mensagem = "URL da API inválida: " + e.getMessage();
        }

        return null;
    }

    /**
     * Converte o texto JSON da API em uma Leitura.
     * Visível no pacote para poder ser testado sem acessar a internet.
     */
    Leitura converter(String json)
    {
        try
        {
            OpenMeteoResposta resposta = conversorJson.readValue(json, OpenMeteoResposta.class);

            if (resposta.getAtual() == null)
            {
                mensagem = "Resposta da API sem o bloco \"current\".";
                return null;
            }

            OpenMeteoResposta.Atual atual = resposta.getAtual();

            Leitura leitura = new Leitura();
            leitura.setDataHoraObservacao(atual.getHorario());
            leitura.setTemperatura(atual.getTemperatura());
            leitura.setUmidade(atual.getUmidade());
            leitura.setPrecipitacao(atual.getPrecipitacao());
            leitura.setVelocidadeVento(atual.getVelocidadeVento());
            leitura.setDirecaoVento(atual.getDirecaoVento());
            leitura.setPressao(atual.getPressao());
            leitura.setSensacaoTermica(atual.getSensacaoTermica());
            leitura.setCodigoTempo(atual.getCodigoTempo());
            leitura.setOrigem(ORIGEM);

            return leitura;
        }
        catch (JsonProcessingException e)
        {
            mensagem = "Resposta da API em formato inesperado: " + e.getOriginalMessage();
            return null;
        }
    }

    private String montarUrl()
    {
        // Locale.ROOT garante ponto decimal (-23.50). Com o padrão brasileiro
        // sairia "-23,50" e a API recusaria a coordenada.
        return String.format(Locale.ROOT,
                "%s?latitude=%.4f&longitude=%.4f&current=%s&timezone=America%%2FSao_Paulo",
                URL_BASE, latitude, longitude, VARIAVEIS);
    }

    @Override
    public String getMensagem()
    {
        return mensagem;
    }
}
