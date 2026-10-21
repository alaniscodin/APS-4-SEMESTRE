package com.mycompany.servidormeteorologico;

import com.mycompany.servidormeteorologico.DAL.HibernateUtil;
import com.mycompany.servidormeteorologico.apresentacao.ArquivosEstaticos;
import com.mycompany.servidormeteorologico.apresentacao.RotasAPI;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Aplicação 2 do trabalho: lê as leituras gravadas pelo coletor e as publica
 * numa página web, com gráfico em domínio do tempo.
 *
 * Usa o servidor HTTP que já vem no JDK (com.sun.net.httpserver): nenhum
 * framework, então o caminho do dado fica explícito, do DAO até a página.
 *
 * Para abrir: http://localhost:8080
 * Para encerrar: Ctrl+C, ou o botão de parar do NetBeans.
 */
public class ServidorMeteorologico
{
    private static final int PORTA_PADRAO = 8080;

    public static void main(String[] args)
    {
        int porta = Integer.getInteger("servidor.porta", PORTA_PADRAO);

        // Conecta no banco já na largada: senha errada ou tabela ausente
        // aparecem agora, e não só quando alguém abrir a página
        if (HibernateUtil.getFabricaSessoes() == null)
        {
            System.err.println("[AVISO] " + HibernateUtil.getMensagem());
            System.err.println("[AVISO] O servidor vai subir assim mesmo e tentará de novo a cada requisição.");
        }

        HttpServer servidor;

        try
        {
            servidor = HttpServer.create(new InetSocketAddress(porta), 0);
        }
        catch (BindException e)
        {
            System.err.println("[ERRO] A porta " + porta + " já está em uso. O servidor já está aberto em outra janela?");
            System.err.println("       Para usar outra porta, rode com -Dservidor.porta=8081");
            HibernateUtil.fechar();
            return;
        }
        catch (IOException e)
        {
            System.err.println("[ERRO] Não foi possível abrir o servidor: " + e.getMessage());
            HibernateUtil.fechar();
            return;
        }

        // Algumas threads atendem página e API em paralelo
        ExecutorService atendentes = Executors.newFixedThreadPool(4);

        servidor.createContext("/api/", new RotasAPI());
        servidor.createContext("/", new ArquivosEstaticos());
        servidor.setExecutor(atendentes);

        // Encerra com ordem mesmo quando o programa é interrompido
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
        {
            servidor.stop(1);
            atendentes.shutdown();
            HibernateUtil.fechar();
            System.out.println("Servidor encerrado.");
        }));

        servidor.start();

        System.out.println("Servidor da estação meteorológica no ar.");
        System.out.println("Abra no navegador: http://localhost:" + porta);
        System.out.println("Para encerrar, pressione Ctrl+C.");
    }
}
