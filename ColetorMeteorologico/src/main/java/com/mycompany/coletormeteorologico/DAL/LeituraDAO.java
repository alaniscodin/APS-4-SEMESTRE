package com.mycompany.coletormeteorologico.DAL;

import com.mycompany.coletormeteorologico.modelo.Leitura;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 * Acesso ao banco para a entidade Leitura (padrão DAO).
 *
 * Diferenças propositais em relação ao projeto-molde:
 *  - a sessão fecha sempre, via try-with-resources, mesmo se der erro;
 *  - a mensagem de erro carrega a causa real, em vez de um texto genérico.
 */
public class LeituraDAO
{
    private String mensagem = "";

    /**
     * Grava uma leitura.
     *
     * @return true se gravou; false em caso de erro (ver getMensagem()).
     */
    public boolean gravar(Leitura leitura)
    {
        mensagem = "";

        SessionFactory fabrica = HibernateUtil.getFabricaSessoes();

        if (fabrica == null)
        {
            mensagem = HibernateUtil.getMensagem();
            return false;
        }

        Transaction transacao = null;

        try (Session sessao = fabrica.openSession())
        {
            transacao = sessao.beginTransaction();
            sessao.persist(leitura);
            transacao.commit();

            mensagem = "Leitura gravada com id " + leitura.getId() + ".";
            return true;
        }
        catch (RuntimeException e)
        {
            desfazer(transacao);
            mensagem = "Erro ao gravar a leitura: " + causaRaiz(e);
            return false;
        }
    }

    /**
     * Conta quantas leituras já existem. Útil para conferir a gravação.
     *
     * @return a quantidade, ou -1 em caso de erro (ver getMensagem()).
     */
    public long contar()
    {
        mensagem = "";

        SessionFactory fabrica = HibernateUtil.getFabricaSessoes();

        if (fabrica == null)
        {
            mensagem = HibernateUtil.getMensagem();
            return -1;
        }

        try (Session sessao = fabrica.openSession())
        {
            return sessao.createSelectionQuery("SELECT COUNT(l) FROM Leitura l", Long.class)
                    .getSingleResult();
        }
        catch (RuntimeException e)
        {
            mensagem = "Erro ao contar as leituras: " + causaRaiz(e);
            return -1;
        }
    }

    private void desfazer(Transaction transacao)
    {
        if (transacao != null && transacao.isActive())
        {
            try
            {
                transacao.rollback();
            }
            catch (RuntimeException e)
            {
                // Se até o rollback falhou, a conexão já caiu; o erro original é o que importa
            }
        }
    }

    /**
     * Percorre a cadeia de causas até a origem.
     * O Hibernate embrulha o erro do banco em várias camadas, e só a última
     * diz o que realmente aconteceu (ex.: violação de CHECK, login inválido).
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

    public String getMensagem()
    {
        return mensagem;
    }
}
