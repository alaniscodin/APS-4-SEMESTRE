package com.mycompany.servidormeteorologico.modelo;

import java.time.LocalDate;

/**
 * Totais das leituras de um dia, exibidos no quadro de resumo da página.
 * Não é entidade: é calculado pelo banco a cada consulta, não gravado.
 */
public class ResumoDia
{
    private final LocalDate data;
    private final long leituras;
    private final long leiturasComChuva;
    private final Double temperaturaMinima;   // null se o dia não tem leituras
    private final Double temperaturaMaxima;

    public ResumoDia(LocalDate data, long leituras, long leiturasComChuva,
            Double temperaturaMinima, Double temperaturaMaxima)
    {
        this.data = data;
        this.leituras = leituras;
        this.leiturasComChuva = leiturasComChuva;
        this.temperaturaMinima = temperaturaMinima;
        this.temperaturaMaxima = temperaturaMaxima;
    }

    public LocalDate getData()
    {
        return data;
    }

    public long getLeituras()
    {
        return leituras;
    }

    public long getLeiturasComChuva()
    {
        return leiturasComChuva;
    }

    public Double getTemperaturaMinima()
    {
        return temperaturaMinima;
    }

    public Double getTemperaturaMaxima()
    {
        return temperaturaMaxima;
    }
}
