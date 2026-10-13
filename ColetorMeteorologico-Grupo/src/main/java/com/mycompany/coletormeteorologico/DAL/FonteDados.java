package com.mycompany.coletormeteorologico.DAL;

import com.mycompany.coletormeteorologico.modelo.Leitura;

/**
 * Contrato de qualquer fonte de dados meteorológicos.
 *
 * Implementações:
 *  - OpenMeteoAPI: dados reais da API Open-Meteo;
 *  - (próxima etapa) Simulador: cenários fictícios para os testes do item 9e.
 *
 * O Controle conhece apenas esta interface, então trocar a fonte
 * não exige mudar o resto do sistema.
 */
public interface FonteDados
{
    /**
     * Obtém uma leitura do clima.
     *
     * @return a leitura obtida, ou null em caso de erro (ver getMensagem()).
     */
    Leitura obterLeitura();

    /**
     * Mensagem da última operação (vazia quando deu certo).
     */
    String getMensagem();
}
