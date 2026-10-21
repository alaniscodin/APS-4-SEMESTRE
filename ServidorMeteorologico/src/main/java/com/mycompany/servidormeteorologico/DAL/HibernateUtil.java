package com.mycompany.servidormeteorologico.DAL;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Guarda a SessionFactory do Hibernate, que é cara de criar e deve existir
 * uma única vez na aplicação inteira.
 *
 * A página consulta o servidor a cada 10 s: criar uma fábrica por requisição
 * esgotaria a memória e deixaria cada resposta lenta.
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
     * Se a criação falhar (banco desligado, senha errada), a próxima chamada
     * tenta de novo: assim o servidor se recupera sozinho quando o banco volta,
     * sem precisar ser reiniciado.
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
     * Fecha a fábrica ao encerrar o servidor.
     */
    public static synchronized void fechar()
    {
        if (fabricaSessoes != null && !fabricaSessoes.isClosed())
        {
            fabricaSessoes.close();
        }
    }

    public static synchronized String getMensagem()
    {
        return mensagem;
    }
}
