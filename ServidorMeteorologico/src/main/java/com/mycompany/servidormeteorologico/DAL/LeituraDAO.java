package com.mycompany.servidormeteorologico.DAL;

import com.mycompany.servidormeteorologico.modelo.Leitura;
import com.mycompany.servidormeteorologico.modelo.ResumoDia;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * Acesso ao banco para a entidade Leitura (padrão DAO), do lado do servidor web.
 *
 * Só lê: quem grava é o coletor (aplicação 1). Por isso não há transação;
 * cada consulta abre uma sessão e a fecha ao terminar, mesmo se der erro.
 *
 * Como no coletor, o erro não é lançado como exceção: o método devolve null
 * e a causa fica em getMensagem(). houveErro() separa "o banco falhou" de
 * "não há nada para mostrar", que a página trata de formas diferentes.
 */
public class LeituraDAO
{
    private String mensagem = "";
    private boolean erro = false;

    /**
     * A leitura mais recente gravada pelo coletor.
     *
     * @return a leitura, ou null se não houver nenhuma ou se der erro.
     */
    public Leitura buscarUltima()
    {
        List<Leitura> leituras = listarUltimas(1);

        if (leituras == null)
        {
            return null;
        }

        if (leituras.isEmpty())
        {
            mensagem = "Nenhuma leitura gravada ainda. O coletor está rodando?";
            return null;
        }

        return leituras.get(0);
    }

    /**
     * As últimas leituras, da mais antiga para a mais nova (a ordem do gráfico).
     *
     * Busca pelo id decrescente porque o id é a chave primária clusterizada:
     * pegar as N últimas linhas é o acesso mais barato que a tabela oferece.
     *
     * @return a lista (pode ser vazia), ou null em caso de erro.
     */
    public List<Leitura> listarUltimas(int quantidade)
    {
        iniciar();

        SessionFactory fabrica = HibernateUtil.getFabricaSessoes();

        if (fabrica == null)
        {
            return falhar(HibernateUtil.getMensagem());
        }

        try (Session sessao = fabrica.openSession())
        {
            List<Leitura> leituras = new ArrayList<>(sessao
                    .createSelectionQuery("FROM Leitura l ORDER BY l.id DESC", Leitura.class)
                    .setMaxResults(quantidade)
                    .getResultList());

            Collections.reverse(leituras);
            return leituras;
        }
        catch (RuntimeException e)
        {
            return falhar("Erro ao consultar as leituras: " + causaRaiz(e));
        }
    }

    /**
     * Totais de um dia: quantas leituras, quantas com chuva, mínima e máxima.
     * Calculado pelo banco numa só consulta, sem trazer as linhas.
     *
     * @return o resumo, ou null em caso de erro.
     */
    public ResumoDia resumir(LocalDate dia)
    {
        iniciar();

        SessionFactory fabrica = HibernateUtil.getFabricaSessoes();

        if (fabrica == null)
        {
            return falhar(HibernateUtil.getMensagem());
        }

        try (Session sessao = fabrica.openSession())
        {
            Object[] linha = sessao.createSelectionQuery(
                    "SELECT COUNT(l), "
                    + "SUM(CASE WHEN l.chovendo = true THEN 1 ELSE 0 END), "
                    + "MIN(l.temperatura), MAX(l.temperatura) "
                    + "FROM Leitura l "
                    + "WHERE l.dataHoraColeta >= :inicio AND l.dataHoraColeta < :fim", Object[].class)
                    .setParameter("inicio", dia.atStartOfDay())
                    .setParameter("fim", dia.plusDays(1).atStartOfDay())
                    .getSingleResult();

            // Num dia sem leituras, SUM, MIN e MAX voltam null
            return new ResumoDia(dia,
                    numero(linha[0]),
                    numero(linha[1]),
                    (Double) linha[2],
                    (Double) linha[3]);
        }
        catch (RuntimeException e)
        {
            return falhar("Erro ao calcular o resumo do dia: " + causaRaiz(e));
        }
    }

    private long numero(Object valor)
    {
        return valor == null ? 0 : ((Number) valor).longValue();
    }

    private void iniciar()
    {
        mensagem = "";
        erro = false;
    }

    private <T> T falhar(String texto)
    {
        mensagem = texto;
        erro = true;
        return null;
    }

    /**
     * Percorre a cadeia de causas até a origem.
     * O Hibernate embrulha o erro do banco em várias camadas, e só a última
     * diz o que realmente aconteceu (ex.: login inválido, banco fora do ar).
     */
    private String causaRaiz(Throwable erro)
    {
        Throwable atual = erro;

        while (atual.getCause() != null && atual.getCause() != atual)
        {
            atual = atual.getCause();
        }

        return atual.getMessage() == null ? erro.toString() : atual.getMessage();
    }

    /**
     * @return true se a última operação falhou por erro de banco.
     */
    public boolean houveErro()
    {
        return erro;
    }

    public String getMensagem()
    {
        return mensagem;
    }
}
