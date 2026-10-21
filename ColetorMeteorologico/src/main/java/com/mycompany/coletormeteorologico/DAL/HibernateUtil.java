package com.mycompany.coletormeteorologico.DAL;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Guarda a SessionFactory do Hibernate, que é cara de criar e deve existir
 * uma única vez na aplicação inteira.
 *
 * O coletor roda por horas gravando a cada 10 s: criar uma fábrica por coleta
 * esgotaria a memória.
 */
public class HibernateUtil
{
    private static SessionFactory fabricaSessoes;
    private static String mensagem = "";

    private HibernateUtil()
    {
    }

    /**
     * Devolve a fábrica, criando-a na primeira chamada.
     *
     * @return a fábrica, ou null se a configuração falhar (ver getMensagem()).
     */
    public static synchronized SessionFactory getFabricaSessoes()
    {
        if (fabricaSessoes == null)
        {
            try
            {
                fabricaSessoes = new Configuration().configure().buildSessionFactory();
                mensagem = "";
            }
            catch (RuntimeException e)
            {
                // Sem isto o erro real (senha errada, tabela ausente) ficaria escondido
                mensagem = "Falha ao iniciar o Hibernate: " + e.getMessage();
                return null;
            }
        }

        return fabricaSessoes;
    }

    /**
     * Fecha a fábrica ao encerrar o coletor.
     */
    public static synchronized void fechar()
    {
        if (fabricaSessoes != null && !fabricaSessoes.isClosed())
        {
            fabricaSessoes.close();
        }
    }

    public static String getMensagem()
    {
        return mensagem;
    }
}
