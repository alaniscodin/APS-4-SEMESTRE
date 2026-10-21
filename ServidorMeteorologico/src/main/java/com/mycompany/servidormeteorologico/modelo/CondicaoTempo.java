package com.mycompany.servidormeteorologico.modelo;

/**
 * Tradução do código de tempo WMO (coluna codigo_tempo) para texto em
 * português e para o ícone exibido na página.
 *
 * A Open-Meteo informa a condição do céu pelo código da Organização
 * Meteorológica Mundial (tabela WMO 4677, reduzida). Os números sozinhos não
 * dizem nada ao usuário; aqui cada um ganha uma descrição.
 *
 * O ícone é o nome de um dos desenhos do front (estacao.js → ICONES).
 */
public enum CondicaoTempo
{
    CEU_LIMPO                    (0,  "Céu limpo",                        "sol"),
    PREDOMINANTEMENTE_LIMPO      (1,  "Predominantemente limpo",          "sol"),
    PARCIALMENTE_NUBLADO         (2,  "Parcialmente nublado",             "parcial"),
    NUBLADO                      (3,  "Nublado",                          "nublado"),
    NEVOEIRO                     (45, "Nevoeiro",                         "neblina"),
    NEVOEIRO_COM_GEADA           (48, "Nevoeiro com geada",               "neblina"),
    GAROA_FRACA                  (51, "Garoa fraca",                      "chuva"),
    GAROA_MODERADA               (53, "Garoa moderada",                   "chuva"),
    GAROA_INTENSA                (55, "Garoa intensa",                    "chuva"),
    GAROA_CONGELANTE_FRACA       (56, "Garoa congelante fraca",           "chuva"),
    GAROA_CONGELANTE_INTENSA     (57, "Garoa congelante intensa",         "chuva"),
    CHUVA_FRACA                  (61, "Chuva fraca",                      "chuva"),
    CHUVA_MODERADA               (63, "Chuva moderada",                   "chuva"),
    CHUVA_FORTE                  (65, "Chuva forte",                      "chuvaForte"),
    CHUVA_CONGELANTE_FRACA       (66, "Chuva congelante fraca",           "chuva"),
    CHUVA_CONGELANTE_FORTE       (67, "Chuva congelante forte",           "chuvaForte"),
    NEVE_FRACA                   (71, "Neve fraca",                       "neve"),
    NEVE_MODERADA                (73, "Neve moderada",                    "neve"),
    NEVE_FORTE                   (75, "Neve forte",                       "neve"),
    GRAOS_DE_NEVE                (77, "Grãos de neve",                    "neve"),
    PANCADAS_FRACAS              (80, "Pancadas de chuva fracas",         "chuva"),
    PANCADAS_MODERADAS           (81, "Pancadas de chuva moderadas",      "chuva"),
    PANCADAS_VIOLENTAS           (82, "Pancadas de chuva violentas",      "chuvaForte"),
    PANCADAS_DE_NEVE_FRACAS      (85, "Pancadas de neve fracas",          "neve"),
    PANCADAS_DE_NEVE_FORTES      (86, "Pancadas de neve fortes",          "neve"),
    TROVOADA                     (95, "Trovoada",                         "tempestade"),
    TROVOADA_COM_GRANIZO_FRACO   (96, "Trovoada com granizo fraco",       "tempestade"),
    TROVOADA_COM_GRANIZO_FORTE   (99, "Trovoada com granizo forte",       "tempestade");

    private final int codigo;
    private final String descricao;
    private final String icone;

    CondicaoTempo(int codigo, String descricao, String icone)
    {
        this.codigo = codigo;
        this.descricao = descricao;
        this.icone = icone;
    }

    /**
     * @return a condição do código, ou null se o código for nulo ou
     *         desconhecido (a página então deduz a condição pelos valores).
     */
    public static CondicaoTempo porCodigo(Integer codigo)
    {
        if (codigo == null)
        {
            return null;
        }

        for (CondicaoTempo condicao : values())
        {
            if (condicao.codigo == codigo)
            {
                return condicao;
            }
        }

        return null;
    }

    public int getCodigo()
    {
        return codigo;
    }

    public String getDescricao()
    {
        return descricao;
    }

    public String getIcone()
    {
        return icone;
    }
}
