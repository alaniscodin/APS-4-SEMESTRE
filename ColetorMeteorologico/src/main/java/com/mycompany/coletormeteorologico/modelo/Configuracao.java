package com.mycompany.coletormeteorologico.modelo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Parâmetros do coletor lidos de coletor.properties.
 *
 * Procura o arquivo em duas posições, nesta ordem:
 *  1. na pasta onde o programa está sendo executado — permite mudar o
 *     intervalo ou trocar para o simulador sem recompilar;
 *  2. embutido no programa (classpath), que serve de padrão.
 *
 * Se nenhum for encontrado, usa valores padrão em vez de falhar: um erro de
 * configuração não deve impedir o coletor de rodar.
 */
public class Configuracao
{
    private static final String ARQUIVO = "coletor.properties";

    private final Properties propriedades = new Properties();
    private String origem = "";

    public Configuracao()
    {
        carregar();
    }

    private void carregar()
    {
        File externo = new File(ARQUIVO);

        if (externo.isFile())
        {
            try (InputStream entrada = new FileInputStream(externo))
            {
                ler(entrada);
                origem = "arquivo " + externo.getAbsolutePath();
                return;
            }
            catch (IOException e)
            {
                origem = "padrões internos (falha ao ler " + externo.getAbsolutePath()
                        + ": " + e.getMessage() + ")";
                return;
            }
        }

        try (InputStream entrada = Configuracao.class.getClassLoader().getResourceAsStream(ARQUIVO))
        {
            if (entrada != null)
            {
                ler(entrada);
                origem = "configuração embutida no programa";
                return;
            }
        }
        catch (IOException e)
        {
            // cai nos padrões abaixo
        }

        origem = "padrões internos (nenhum " + ARQUIVO + " encontrado)";
    }

    /**
     * Lê o arquivo como UTF-8, descartando o BOM se houver.
     *
     * O Bloco de Notas do Windows grava UTF-8 com BOM (três bytes invisíveis no
     * início). Sem descartá-los, eles grudam no nome da primeira propriedade,
     * que deixa de ser encontrada e volta ao valor padrão sem qualquer aviso.
     */
    private void ler(InputStream entrada) throws IOException
    {
        BufferedInputStream bufferizada = new BufferedInputStream(entrada);

        bufferizada.mark(3);
        byte[] inicio = new byte[3];
        int lidos = bufferizada.read(inicio, 0, 3);

        boolean temBom = lidos == 3
                && (inicio[0] & 0xFF) == 0xEF
                && (inicio[1] & 0xFF) == 0xBB
                && (inicio[2] & 0xFF) == 0xBF;

        if (!temBom)
        {
            bufferizada.reset();
        }

        try (Reader leitor = new InputStreamReader(bufferizada, StandardCharsets.UTF_8))
        {
            propriedades.load(leitor);
        }
    }

    public int getIntervaloSegundos()
    {
        int segundos = inteiro("coletor.intervalo.segundos", 10);

        // Um intervalo zero ou negativo faria o agendador girar sem parar
        return segundos > 0 ? segundos : 10;
    }

    /**
     * @return "API" ou "SIMULADOR".
     */
    public String getFonte()
    {
        String valor = propriedades.getProperty("coletor.fonte", "API").trim().toUpperCase();

        return valor.equals("SIMULADOR") ? "SIMULADOR" : "API";
    }

    /**
     * @return o cenário fixo do simulador, ou vazio para percorrer todos.
     */
    public String getCenarioSimulador()
    {
        return propriedades.getProperty("coletor.simulador.cenario", "").trim().toUpperCase();
    }

    public double getLatitude()
    {
        return decimal("coletor.latitude", -23.50);
    }

    public double getLongitude()
    {
        return decimal("coletor.longitude", -47.46);
    }

    /**
     * De onde a configuração veio. Mostrado ao iniciar, para não haver dúvida
     * sobre qual arquivo está valendo.
     */
    public String getOrigem()
    {
        return origem;
    }

    private int inteiro(String chave, int padrao)
    {
        try
        {
            return Integer.parseInt(propriedades.getProperty(chave, String.valueOf(padrao)).trim());
        }
        catch (NumberFormatException e)
        {
            return padrao;
        }
    }

    private double decimal(String chave, double padrao)
    {
        try
        {
            return Double.parseDouble(propriedades.getProperty(chave, String.valueOf(padrao)).trim());
        }
        catch (NumberFormatException e)
        {
            return padrao;
        }
    }
}
