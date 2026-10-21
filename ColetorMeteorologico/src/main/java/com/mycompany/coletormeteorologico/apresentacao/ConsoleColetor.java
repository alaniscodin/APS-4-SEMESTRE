package com.mycompany.coletormeteorologico.apresentacao;

import com.mycompany.coletormeteorologico.modelo.Leitura;
import java.time.format.DateTimeFormatter;

/**
 * Camada de apresentação do coletor: mostra as leituras no console.
 * (O coletor não precisa de tela; quem exibe os dados ao usuário é o front-end.)
 */
public class ConsoleColetor
{
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public void exibirLeitura(Leitura leitura)
    {
        System.out.println("========== LEITURA METEOROLÓGICA ==========");
        System.out.println("Coletada em .......... " + formatarData(leitura));
        System.out.println("Observação da fonte .. " + (leitura.getDataHoraObservacao() == null
                ? "-" : leitura.getDataHoraObservacao().format(FORMATO_DATA)));
        System.out.println("Origem ............... " + leitura.getOrigem());
        System.out.println("Temperatura .......... " + valor(leitura.getTemperatura(), "°C"));
        System.out.println("Sensação térmica ..... " + valor(leitura.getSensacaoTermica(), "°C"));
        System.out.println("Umidade .............. " + valor(leitura.getUmidade(), "%"));
        System.out.println("Precipitação ......... " + valor(leitura.getPrecipitacao(), "mm"));
        System.out.println("Chovendo ............. " + (Boolean.TRUE.equals(leitura.getChovendo()) ? "Sim" : "Não"));
        System.out.println("Vento ................ " + valor(leitura.getVelocidadeVento(), "km/h"));
        System.out.println("Direção do vento ..... " + valor(leitura.getDirecaoVento(), "°"));
        System.out.println("Pressão .............. " + valor(leitura.getPressao(), "hPa"));
        System.out.println("Código do tempo (WMO)  " + valor(leitura.getCodigoTempo(), ""));
        System.out.println("===========================================");
    }

    public void exibirMensagem(String mensagem)
    {
        System.out.println(mensagem);
    }

    public void exibirErro(String mensagem)
    {
        System.err.println("[ERRO] " + mensagem);
    }

    private String formatarData(Leitura leitura)
    {
        return leitura.getDataHoraColeta() == null ? "-" : leitura.getDataHoraColeta().format(FORMATO_DATA);
    }

    private String valor(Object valor, String unidade)
    {
        return valor == null ? "-" : (valor + " " + unidade).trim();
    }
}
