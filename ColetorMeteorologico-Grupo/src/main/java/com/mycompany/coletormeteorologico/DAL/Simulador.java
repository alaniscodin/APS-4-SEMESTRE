package com.mycompany.coletormeteorologico.DAL;

import com.mycompany.coletormeteorologico.modelo.Leitura;
import java.time.LocalDateTime;

/**
 * Fonte de dados fictícia: devolve condições meteorológicas conhecidas,
 * sem depender da internet.
 *
 * Existe para o item 9e do enunciado, que pede testes simulando condições
 * meteorológicas para verificar a coleta e a publicação. Como implementa
 * FonteDados, a leitura simulada percorre exatamente o mesmo caminho da real:
 * validação, gravação no banco e, depois, a página web.
 *
 * Os valores são fixos de propósito. Números aleatórios dariam um teste
 * diferente a cada execução, impossível de conferir ou de descrever na
 * monografia; com cenários nomeados, o resultado esperado é conhecido de
 * antemão e o teste pode ser repetido.
 */
public class Simulador implements FonteDados
{
    public static final String ORIGEM = "SIMULADOR";

    /**
     * Condições meteorológicas de referência.
     * Todos os valores estão dentro das faixas aceitas pela Validacao:
     * o simulador testa o caminho completo, não a rejeição de dados inválidos.
     */
    public enum Cenario
    {
        //                     temp  umid  precip  vento  dir  pressão  sensação  WMO
        CHUVA_FORTE   (18.4,   98,   12.5,  35.2,  180,   940.2,   18.0,    65),
        CALOR_SECO    (36.5,   22,    0.0,   8.1,   90,   948.7,   38.4,     0),
        VENTANIA      (24.1,   55,    0.0,  78.3,  230,   941.5,   23.2,     3),
        GEADA         (-1.5,   85,    0.0,   5.4,  160,   952.3,   -5.8,    48),
        TEMPO_BOM     (25.3,   60,    0.0,  10.2,  120,   946.0,   25.1,     1);

        private final double temperatura;
        private final int umidade;
        private final double precipitacao;
        private final double velocidadeVento;
        private final int direcaoVento;
        private final double pressao;
        private final double sensacaoTermica;
        private final int codigoTempo;

        Cenario(double temperatura, int umidade, double precipitacao, double velocidadeVento,
                int direcaoVento, double pressao, double sensacaoTermica, int codigoTempo)
        {
            this.temperatura = temperatura;
            this.umidade = umidade;
            this.precipitacao = precipitacao;
            this.velocidadeVento = velocidadeVento;
            this.direcaoVento = direcaoVento;
            this.pressao = pressao;
            this.sensacaoTermica = sensacaoTermica;
            this.codigoTempo = codigoTempo;
        }
    }

    private final Cenario cenarioFixo;   // null = percorre todos
    private int proximo = 0;
    private String mensagem = "";

    /**
     * Percorre todos os cenários, um por coleta, em ordem.
     */
    public Simulador()
    {
        this.cenarioFixo = null;
    }

    /**
     * Repete sempre o mesmo cenário.
     */
    public Simulador(Cenario cenario)
    {
        this.cenarioFixo = cenario;
    }

    @Override
    public Leitura obterLeitura()
    {
        mensagem = "";

        Cenario cenario = cenarioFixo;

        if (cenario == null)
        {
            Cenario[] todos = Cenario.values();
            cenario = todos[proximo % todos.length];
            proximo++;
        }

        Leitura leitura = new Leitura();
        leitura.setDataHoraObservacao(LocalDateTime.now());
        leitura.setTemperatura(cenario.temperatura);
        leitura.setUmidade(cenario.umidade);
        leitura.setPrecipitacao(cenario.precipitacao);
        leitura.setVelocidadeVento(cenario.velocidadeVento);
        leitura.setDirecaoVento(cenario.direcaoVento);
        leitura.setPressao(cenario.pressao);
        leitura.setSensacaoTermica(cenario.sensacaoTermica);
        leitura.setCodigoTempo(cenario.codigoTempo);
        leitura.setOrigem(ORIGEM);

        mensagem = "Cenário simulado: " + cenario.name();
        return leitura;
    }

    @Override
    public String getMensagem()
    {
        return mensagem;
    }

    /**
     * Converte o texto do arquivo de configuração em um cenário.
     *
     * @return o cenário, ou null se o texto estiver vazio ou não corresponder
     *         a nenhum (nesse caso o simulador percorre todos).
     */
    public static Cenario cenarioPorNome(String nome)
    {
        if (nome == null || nome.isBlank())
        {
            return null;
        }

        try
        {
            return Cenario.valueOf(nome.trim().toUpperCase());
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }
}
