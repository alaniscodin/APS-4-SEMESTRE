package com.mycompany.coletormeteorologico.modelo;

/**
 * Verifica se a leitura tem os dados obrigatórios do enunciado
 * e se os valores estão em faixas fisicamente plausíveis.
 */
public class Validacao
{
    private String mensagem = "";

    public void validarLeitura(Leitura leitura)
    {
        StringBuilder erros = new StringBuilder();

        // Obrigatórios pelo enunciado (item 2)
        if (leitura.getTemperatura() == null)
        {
            erros.append("Temperatura ausente. ");
        }
        else if (leitura.getTemperatura() < -50 || leitura.getTemperatura() > 60)
        {
            erros.append("Temperatura fora da faixa (-50 a 60 °C). ");
        }

        if (leitura.getUmidade() == null)
        {
            erros.append("Umidade ausente. ");
        }
        else if (leitura.getUmidade() < 0 || leitura.getUmidade() > 100)
        {
            erros.append("Umidade fora da faixa (0 a 100 %). ");
        }

        if (leitura.getPrecipitacao() == null)
        {
            erros.append("Precipitação ausente. ");
        }
        else if (leitura.getPrecipitacao() < 0)
        {
            erros.append("Precipitação negativa. ");
        }

        if (leitura.getVelocidadeVento() == null)
        {
            erros.append("Velocidade do vento ausente. ");
        }
        else if (leitura.getVelocidadeVento() < 0 || leitura.getVelocidadeVento() > 400)
        {
            erros.append("Velocidade do vento fora da faixa (0 a 400 km/h). ");
        }

        // Complementares: podem faltar, mas se vierem precisam ser plausíveis
        if (leitura.getDirecaoVento() != null
                && (leitura.getDirecaoVento() < 0 || leitura.getDirecaoVento() > 360))
        {
            erros.append("Direção do vento fora da faixa (0 a 360°). ");
        }

        if (leitura.getPressao() != null
                && (leitura.getPressao() < 300 || leitura.getPressao() > 1100))
        {
            erros.append("Pressão fora da faixa (300 a 1100 hPa). ");
        }

        this.mensagem = erros.toString().trim();
    }

    public String getMensagem()
    {
        return mensagem;
    }
}
