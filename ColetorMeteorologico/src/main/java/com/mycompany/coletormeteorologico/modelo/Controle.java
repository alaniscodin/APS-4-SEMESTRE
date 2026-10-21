package com.mycompany.coletormeteorologico.modelo;

import com.mycompany.coletormeteorologico.DAL.FonteDados;
import com.mycompany.coletormeteorologico.DAL.LeituraDAO;
import java.time.LocalDateTime;

/**
 * Regra de negócio da coleta: pede a leitura à fonte, completa, valida e grava.
 *
 * Recebe a fonte pela interface FonteDados, então funciona igual
 * com a API real ou com o simulador (próxima etapa).
 */
public class Controle
{
    private final FonteDados fonte;
    private final LeituraDAO leituraDAO;
    private String mensagem = "";

    public Controle(FonteDados fonte)
    {
        this.fonte = fonte;
        this.leituraDAO = new LeituraDAO();
    }

    /**
     * Executa uma coleta completa: obtém, completa, valida e grava no banco.
     *
     * @return a leitura gravada, ou null em caso de erro (ver getMensagem()).
     */
    public Leitura coletar()
    {
        mensagem = "";

        Leitura leitura = fonte.obterLeitura();

        if (leitura == null)
        {
            mensagem = fonte.getMensagem();
            return null;
        }

        // Dados que o coletor define, independentemente da fonte
        leitura.setDataHoraColeta(LocalDateTime.now());
        leitura.setChovendo(leitura.getPrecipitacao() != null && leitura.getPrecipitacao() > 0);

        Validacao validacao = new Validacao();
        validacao.validarLeitura(leitura);

        if (!validacao.getMensagem().isEmpty())
        {
            mensagem = "Leitura descartada por dados inválidos: " + validacao.getMensagem();
            return null;
        }

        // Só grava o que passou na validação
        if (!leituraDAO.gravar(leitura))
        {
            mensagem = leituraDAO.getMensagem();
            return null;
        }

        mensagem = leituraDAO.getMensagem();
        return leitura;
    }

    /**
     * Quantas leituras já existem no banco.
     */
    public long contarLeituras()
    {
        return leituraDAO.contar();
    }

    public String getMensagem()
    {
        return mensagem;
    }
}
