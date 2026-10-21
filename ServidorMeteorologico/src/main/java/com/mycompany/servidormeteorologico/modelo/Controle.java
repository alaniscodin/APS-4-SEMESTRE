package com.mycompany.servidormeteorologico.modelo;

import com.mycompany.servidormeteorologico.DAL.LeituraDAO;
import java.time.LocalDate;
import java.util.List;

/**
 * Intermediário entre a apresentação (rotas da API) e o banco (LeituraDAO).
 *
 * Mesmo fluxo do projeto-molde: apresentação → Controle → Validacao → DAO.
 * O resultado volta como valor; em caso de problema, o valor é null e
 * getFalha() / getMensagem() dizem o que aconteceu.
 *
 * Cada requisição cria o seu Controle: o servidor atende várias ao mesmo
 * tempo, e a mensagem de uma não pode vazar para a outra.
 */
public class Controle
{
    /**
     * Por que uma consulta não trouxe resultado. A apresentação usa isto para
     * escolher a resposta HTTP (400, 404 ou 503).
     */
    public enum Falha
    {
        NENHUMA,
        PARAMETRO_INVALIDO,
        SEM_DADOS,
        BANCO_INDISPONIVEL
    }

    private final LeituraDAO dao = new LeituraDAO();
    private final Validacao validacao = new Validacao();
    private String mensagem = "";
    private Falha falha = Falha.NENHUMA;

    public Leitura ultimaLeitura()
    {
        limpar();

        Leitura leitura = dao.buscarUltima();

        if (leitura == null)
        {
            registrar(dao.houveErro() ? Falha.BANCO_INDISPONIVEL : Falha.SEM_DADOS, dao.getMensagem());
        }

        return leitura;
    }

    /**
     * @param limiteTexto quantidade pedida pela página (texto da URL).
     */
    public List<Leitura> historico(String limiteTexto)
    {
        limpar();

        int limite = validacao.validarLimite(limiteTexto);

        if (limite < 0)
        {
            registrar(Falha.PARAMETRO_INVALIDO, validacao.getMensagem());
            return null;
        }

        List<Leitura> leituras = dao.listarUltimas(limite);

        if (leituras == null)
        {
            registrar(Falha.BANCO_INDISPONIVEL, dao.getMensagem());
        }

        return leituras;
    }

    public ResumoDia resumoDeHoje()
    {
        limpar();

        ResumoDia resumo = dao.resumir(LocalDate.now());

        if (resumo == null)
        {
            registrar(Falha.BANCO_INDISPONIVEL, dao.getMensagem());
        }

        return resumo;
    }

    private void limpar()
    {
        mensagem = "";
        falha = Falha.NENHUMA;
    }

    private void registrar(Falha tipo, String texto)
    {
        falha = tipo;
        mensagem = texto;
    }

    public Falha getFalha()
    {
        return falha;
    }

    public String getMensagem()
    {
        return mensagem;
    }
}
