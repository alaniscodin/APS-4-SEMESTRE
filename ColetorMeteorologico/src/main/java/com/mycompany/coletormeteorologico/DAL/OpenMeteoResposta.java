package com.mycompany.coletormeteorologico.DAL;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * Classe "espelho" do JSON devolvido pela Open-Meteo.
 *
 * O Jackson lê o texto JSON e preenche estes atributos automaticamente.
 * @JsonProperty liga o nome do JSON (ex.: "temperature_2m") ao atributo Java.
 * Campos do JSON que não estão aqui são ignorados (configurado em OpenMeteoAPI).
 */
public class OpenMeteoResposta
{
    @JsonProperty("elevation")
    private Double elevacao;

    @JsonProperty("current")
    private Atual atual;

    public Double getElevacao()
    {
        return elevacao;
    }

    public Atual getAtual()
    {
        return atual;
    }

    /**
     * Bloco "current" do JSON: as condições atuais.
     */
    public static class Atual
    {
        @JsonProperty("time")
        private LocalDateTime horario;

        @JsonProperty("interval")
        private Integer intervaloSegundos;

        @JsonProperty("temperature_2m")
        private Double temperatura;

        @JsonProperty("relative_humidity_2m")
        private Integer umidade;

        @JsonProperty("precipitation")
        private Double precipitacao;

        @JsonProperty("wind_speed_10m")
        private Double velocidadeVento;

        @JsonProperty("wind_direction_10m")
        private Integer direcaoVento;

        @JsonProperty("surface_pressure")
        private Double pressao;

        @JsonProperty("apparent_temperature")
        private Double sensacaoTermica;

        @JsonProperty("weather_code")
        private Integer codigoTempo;

        public LocalDateTime getHorario()
        {
            return horario;
        }

        public Integer getIntervaloSegundos()
        {
            return intervaloSegundos;
        }

        public Double getTemperatura()
        {
            return temperatura;
        }

        public Integer getUmidade()
        {
            return umidade;
        }

        public Double getPrecipitacao()
        {
            return precipitacao;
        }

        public Double getVelocidadeVento()
        {
            return velocidadeVento;
        }

        public Integer getDirecaoVento()
        {
            return direcaoVento;
        }

        public Double getPressao()
        {
            return pressao;
        }

        public Double getSensacaoTermica()
        {
            return sensacaoTermica;
        }

        public Integer getCodigoTempo()
        {
            return codigoTempo;
        }
    }
}
