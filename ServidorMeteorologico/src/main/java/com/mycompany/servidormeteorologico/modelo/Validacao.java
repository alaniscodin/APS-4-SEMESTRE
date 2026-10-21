package com.mycompany.servidormeteorologico.modelo;

/**
 * Valida os parâmetros recebidos da página antes de consultar o banco.
 *
 * O parâmetro vem da URL, ou seja, de fora: qualquer um pode digitar
 * ?limite=abc ou ?limite=99999999 no navegador. Sem esta checagem, o primeiro
 * daria erro interno e o segundo carregaria a tabela inteira na memória.
 */
public class Validacao
{
    public static final int LIMITE_PADRAO = 60;    // 10 minutos de coleta a cada 10 s
    public static final int LIMITE_MAXIMO = 360;   // 1 hora

    private String mensagem = "";

    /**
     * Converte e confere a quantidade de leituras pedida para o gráfico.
     *
     * @param texto o valor de ?limite= (pode ser null: usa o padrão).
     * @return a quantidade, ou -1 se for inválida (ver getMensagem()).
     */
    public int validarLimite(String texto)
    {
        mensagem = "";

        if (texto == null || texto.isBlank())
        {
            return LIMITE_PADRAO;
        }

        int limite;

        try
        {
            limite = Integer.parseInt(texto.trim());
        }
        catch (NumberFormatException e)
        {
            mensagem = "O limite deve ser um número inteiro; recebido: \"" + texto + "\".";
            return -1;
        }

        if (limite < 1 || limite > LIMITE_MAXIMO)
        {
            mensagem = "O limite deve estar entre 1 e " + LIMITE_MAXIMO + "; recebido: " + limite + ".";
            return -1;
        }

        return limite;
    }

    public String getMensagem()
    {
        return mensagem;
    }
}
