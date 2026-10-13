package com.mycompany.coletormeteorologico;

import com.mycompany.coletormeteorologico.DAL.FonteDados;
import com.mycompany.coletormeteorologico.DAL.HibernateUtil;
import com.mycompany.coletormeteorologico.DAL.OpenMeteoAPI;
import com.mycompany.coletormeteorologico.DAL.Simulador;
import com.mycompany.coletormeteorologico.apresentacao.ConsoleColetor;
import com.mycompany.coletormeteorologico.modelo.Agendador;
import com.mycompany.coletormeteorologico.modelo.Configuracao;
import com.mycompany.coletormeteorologico.modelo.Controle;
import com.mycompany.coletormeteorologico.modelo.Leitura;

/**
 * Ponto de entrada do coletor.
 *
 * Lê a configuração, escolhe a fonte (API real ou simulador) e passa a coletar
 * no intervalo configurado, gravando cada leitura no banco.
 *
 * Para encerrar: Ctrl+C, ou o botão de parar do NetBeans.
 */
public class ColetorMeteorologico
{
    public static void main(String[] args)
    {
        Configuracao configuracao = new Configuracao();
        ConsoleColetor console = new ConsoleColetor();
        FonteDados fonte = criarFonte(configuracao);
        Controle controle = new Controle(fonte);
        Agendador agendador = new Agendador(configuracao.getIntervaloSegundos());

        console.exibirMensagem("Configuração: " + configuracao.getOrigem());
        console.exibirMensagem("Fonte: " + configuracao.getFonte()
                + " | intervalo: " + configuracao.getIntervaloSegundos() + " s");
        console.exibirMensagem("Para encerrar, pressione Ctrl+C.");

        // Encerra com ordem mesmo quando o programa é interrompido
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
        {
            agendador.parar();
            HibernateUtil.fechar();
            System.out.println("Coletor encerrado.");
        }));

        agendador.iniciar(() -> coletarUmaVez(controle, console));
    }

    /**
     * Uma coleta: obtém, valida, grava e mostra o resultado.
     */
    private static void coletarUmaVez(Controle controle, ConsoleColetor console)
    {
        Leitura leitura = controle.coletar();

        if (leitura != null)
        {
            console.exibirLeitura(leitura);
            console.exibirMensagem(controle.getMensagem());
        }
        else
        {
            // Uma coleta com erro não derruba o coletor: a próxima tenta de novo
            console.exibirErro(controle.getMensagem());
        }
    }

    /**
     * Escolhe a implementação de FonteDados conforme a configuração.
     * O resto do programa não sabe qual das duas está em uso.
     */
    private static FonteDados criarFonte(Configuracao configuracao)
    {
        if (configuracao.getFonte().equals("SIMULADOR"))
        {
            Simulador.Cenario cenario = Simulador.cenarioPorNome(configuracao.getCenarioSimulador());

            return cenario == null ? new Simulador() : new Simulador(cenario);
        }

        return new OpenMeteoAPI(configuracao.getLatitude(), configuracao.getLongitude());
    }
}
