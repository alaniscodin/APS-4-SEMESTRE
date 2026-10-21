package com.mycompany.servidormeteorologico.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Uma leitura meteorológica: vira uma linha da tabela Leituras.
 *
 * Gerada a partir do banco (assistente "Entity Classes from Database") e
 * ajustada em três pontos:
 *  - datas em LocalDateTime, no lugar de java.util.Date + @Temporal;
 *  - wrappers (Double, Integer, Boolean) em vez de tipos primitivos,
 *    para aceitar null caso a fonte deixe de enviar algum valor;
 *  - sem @NamedQuery: o LeituraDAO grava pela sessão do Hibernate.
 */
@Entity
@Table(name = "Leituras")
public class Leitura implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "data_hora_coleta", nullable = false)
    private LocalDateTime dataHoraColeta;      // relógio do coletor

    @Column(name = "data_hora_observacao")
    private LocalDateTime dataHoraObservacao;  // horário informado pela API

    @Column(name = "temperatura", nullable = false)
    private Double temperatura;                // °C

    @Column(name = "umidade", nullable = false)
    private Integer umidade;                   // %

    @Column(name = "precipitacao", nullable = false)
    private Double precipitacao;               // mm

    @Column(name = "chovendo", nullable = false)
    private Boolean chovendo;                  // derivado: precipitacao > 0

    @Column(name = "velocidade_vento", nullable = false)
    private Double velocidadeVento;            // km/h

    @Column(name = "direcao_vento")
    private Integer direcaoVento;              // graus (0 a 360)

    @Column(name = "pressao")
    private Double pressao;                    // hPa (na superfície)

    @Column(name = "sensacao_termica")
    private Double sensacaoTermica;            // °C

    @Column(name = "codigo_tempo")
    private Integer codigoTempo;               // código WMO

    @Column(name = "origem", nullable = false, length = 20)
    private String origem;                     // "API" ou "SIMULADOR"

    public Leitura()
    {
    }

    public Integer getId()
    {
        return id;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public LocalDateTime getDataHoraColeta()
    {
        return dataHoraColeta;
    }

    public void setDataHoraColeta(LocalDateTime dataHoraColeta)
    {
        this.dataHoraColeta = dataHoraColeta;
    }

    public LocalDateTime getDataHoraObservacao()
    {
        return dataHoraObservacao;
    }

    public void setDataHoraObservacao(LocalDateTime dataHoraObservacao)
    {
        this.dataHoraObservacao = dataHoraObservacao;
    }

    public Double getTemperatura()
    {
        return temperatura;
    }

    public void setTemperatura(Double temperatura)
    {
        this.temperatura = temperatura;
    }

    public Integer getUmidade()
    {
        return umidade;
    }

    public void setUmidade(Integer umidade)
    {
        this.umidade = umidade;
    }

    public Double getPrecipitacao()
    {
        return precipitacao;
    }

    public void setPrecipitacao(Double precipitacao)
    {
        this.precipitacao = precipitacao;
    }

    public Boolean getChovendo()
    {
        return chovendo;
    }

    public void setChovendo(Boolean chovendo)
    {
        this.chovendo = chovendo;
    }

    public Double getVelocidadeVento()
    {
        return velocidadeVento;
    }

    public void setVelocidadeVento(Double velocidadeVento)
    {
        this.velocidadeVento = velocidadeVento;
    }

    public Integer getDirecaoVento()
    {
        return direcaoVento;
    }

    public void setDirecaoVento(Integer direcaoVento)
    {
        this.direcaoVento = direcaoVento;
    }

    public Double getPressao()
    {
        return pressao;
    }

    public void setPressao(Double pressao)
    {
        this.pressao = pressao;
    }

    public Double getSensacaoTermica()
    {
        return sensacaoTermica;
    }

    public void setSensacaoTermica(Double sensacaoTermica)
    {
        this.sensacaoTermica = sensacaoTermica;
    }

    public Integer getCodigoTempo()
    {
        return codigoTempo;
    }

    public void setCodigoTempo(Integer codigoTempo)
    {
        this.codigoTempo = codigoTempo;
    }

    public String getOrigem()
    {
        return origem;
    }

    public void setOrigem(String origem)
    {
        this.origem = origem;
    }

    @Override
    public boolean equals(Object objeto)
    {
        if (this == objeto)
        {
            return true;
        }

        if (!(objeto instanceof Leitura))
        {
            return false;
        }

        Leitura outra = (Leitura) objeto;

        // Duas leituras só são a mesma se já têm id do banco e ele coincide
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(id);
    }

    @Override
    public String toString()
    {
        return "Leitura[id=" + id + ", coleta=" + dataHoraColeta + ", origem=" + origem + "]";
    }
}
