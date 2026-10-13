/* =============================================================================
   APS 2026/2 - Estacao Meteorologica
   Script de criacao do banco para o coletor.

   Como usar:
     1. Abra o SQL Server Management Studio (SSMS).
     2. Conecte em  localhost  com Windows Authentication,
        marcando "Trust server certificate".
     3. Abra este arquivo (File > Open > File) e clique em Execute (F5).

   ATENCAO: na secao 4, troque a senha antes de executar.
   ============================================================================= */


/* ---------------------------------------------------------------------------
   1. Banco
   --------------------------------------------------------------------------- */

IF DB_ID('estacao_meteorologica') IS NOT NULL
BEGIN
    PRINT 'O banco estacao_meteorologica ja existe. Nada foi criado.';
    PRINT 'Para comecar do zero, apague-o antes com:  DROP DATABASE estacao_meteorologica;';
END
ELSE
BEGIN
    PRINT 'Criando o banco estacao_meteorologica...';
END
GO

IF DB_ID('estacao_meteorologica') IS NULL
    CREATE DATABASE estacao_meteorologica;
GO

USE estacao_meteorologica;
GO


/* ---------------------------------------------------------------------------
   2. Tabela de leituras

   Uma linha por coleta. O coletor grava a cada 10 segundos, entao esta tabela
   cresce cerca de 8.640 linhas por dia de execucao continua.

   Duas datas diferentes, de proposito:
     data_hora_coleta      - quando o coletor perguntou (muda a cada 10 s);
     data_hora_observacao  - quando a API mediu (a Open-Meteo so atualiza a
                             cada 15 minutos, entao esta se repete).
   --------------------------------------------------------------------------- */

IF OBJECT_ID('dbo.Leituras', 'U') IS NOT NULL
BEGIN
    PRINT 'A tabela Leituras ja existe. Nada foi criado.';
END
ELSE
BEGIN
    CREATE TABLE Leituras
    (
        id                   INT IDENTITY(1,1) PRIMARY KEY,
        data_hora_coleta     DATETIME2   NOT NULL,
        data_hora_observacao DATETIME2   NULL,
        temperatura          FLOAT       NOT NULL,
        umidade              INT         NOT NULL CHECK (umidade BETWEEN 0 AND 100),
        precipitacao         FLOAT       NOT NULL CHECK (precipitacao >= 0),
        chovendo             BIT         NOT NULL,
        velocidade_vento     FLOAT       NOT NULL CHECK (velocidade_vento >= 0),
        direcao_vento        INT         NULL,
        pressao              FLOAT       NULL,
        sensacao_termica     FLOAT       NULL,
        codigo_tempo         INT         NULL,
        origem               VARCHAR(20) NOT NULL CHECK (origem IN ('API', 'SIMULADOR'))
    );

    PRINT 'Tabela Leituras criada.';
END
GO


/* ---------------------------------------------------------------------------
   3. Indice

   A pagina web (aplicacao 2) vai buscar as leituras por intervalo de tempo
   para montar o grafico. Sem este indice, cada consulta varreria a tabela
   inteira.
   --------------------------------------------------------------------------- */

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE name = 'IX_Leituras_DataHoraColeta'
                 AND object_id = OBJECT_ID('dbo.Leituras'))
BEGIN
    CREATE INDEX IX_Leituras_DataHoraColeta ON Leituras (data_hora_coleta);
    PRINT 'Indice IX_Leituras_DataHoraColeta criado.';
END
GO


/* ---------------------------------------------------------------------------
   4. Login do coletor

   >>> TROQUE 'TroqueEstaSenha2026!' PELA SUA SENHA, NAS DUAS LINHAS ABAIXO <<<

   A senha precisa ser forte (maiuscula, minuscula e numero, no minimo), senao
   o SQL Server recusa por causa da politica de senha do Windows.

   O usuario recebe db_owner apenas neste banco, e nao sysadmin no servidor:
   o coletor nao tem motivo para mandar em mais nada.

   Depois de definir a senha aqui, coloque a MESMA senha no arquivo
   src/main/resources/hibernate.cfg.xml do projeto.
   --------------------------------------------------------------------------- */

IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = 'coletor')
BEGIN
    CREATE LOGIN coletor WITH PASSWORD = '1234aa';
    PRINT 'Login coletor criado.';
END
ELSE
BEGIN
    PRINT 'O login coletor ja existe. Se esqueceu a senha, use:';
    PRINT '    ALTER LOGIN coletor WITH PASSWORD = ''NovaSenha'';';
END
GO

USE estacao_meteorologica;
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'coletor')
BEGIN
    CREATE USER coletor FOR LOGIN coletor;
    ALTER ROLE db_owner ADD MEMBER coletor;
    PRINT 'Usuario coletor liberado no banco.';
END
GO


/* ---------------------------------------------------------------------------
   5. Conferencia

   Se tudo deu certo, o resultado abaixo mostra 13 colunas e o login criado.
   --------------------------------------------------------------------------- */

USE estacao_meteorologica;
GO

SELECT 'Colunas da tabela' AS verificacao, COUNT(*) AS total
FROM sys.columns WHERE object_id = OBJECT_ID('dbo.Leituras')
UNION ALL
SELECT 'Login coletor existe', COUNT(*)
FROM sys.server_principals WHERE name = 'coletor'
UNION ALL
SELECT 'Leituras gravadas ate agora', COUNT(*) FROM Leituras;
GO
