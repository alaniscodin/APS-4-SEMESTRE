package com.mycompany.coletormeteorologico.modelo;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Repete uma tarefa em intervalo fixo — no coletor, a cada 10 segundos,
 * conforme o item 6 do enunciado.
 *
 * Usa uma única thread: duas coletas nunca acontecem ao mesmo tempo. Se uma
 * consulta à API demorar mais que o intervalo, a próxima espera a vez em vez
 * de se sobrepor — o que também evita estourar o limite de chamadas da API.
 */
public class Agendador
{
    private final int intervaloSegundos;
    private final ScheduledExecutorService executor;

    public Agendador(int intervaloSegundos)
    {
        this.intervaloSegundos = intervaloSegundos;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Passa a executar a tarefa, começando imediatamente.
     */
    public void iniciar(Runnable tarefa)
    {
        executor.scheduleAtFixedRate(() ->
        {
            try
            {
                tarefa.run();
            }
            catch (RuntimeException e)
            {
                // Indispensável: se uma exceção escapar daqui, o agendador
                // cancela as execuções seguintes em silêncio e o coletor
                // simplesmente para de coletar, sem avisar ninguém.
                System.err.println("[ERRO] Falha na coleta agendada: " + e);
            }
        }, 0, intervaloSegundos, TimeUnit.SECONDS);
    }

    /**
     * Encerra o agendador, esperando a coleta em andamento terminar.
     */
    public void parar()
    {
        executor.shutdown();

        try
        {
            if (!executor.awaitTermination(15, TimeUnit.SECONDS))
            {
                executor.shutdownNow();
            }
        }
        catch (InterruptedException e)
        {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public int getIntervaloSegundos()
    {
        return intervaloSegundos;
    }
}
