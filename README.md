# Coletor Meteorológico — como rodar na sua máquina

APS 2026/2 — Estação Meteorológica

Esta é a **aplicação 1** do trabalho: consulta a API de previsão do tempo, valida os dados e grava no banco. A aplicação 2 (página web com gráfico) é separada.

> **Comece por aqui:** siga as seções em ordem. Se ocorrer um erro, consulte [`CASOS_DE_ERRO.md`](CASOS_DE_ERRO.md).

## 1. O que instalar

| Programa | Observação |
|---|---|
| SQL Server 2025 Express | Gratuito |
| SQL Server Management Studio (SSMS) 21 | Para criar e administrar o banco |
| Apache NetBeans 29 | Pode usar uma instalação existente com JDK 21+ |

Downloads oficiais:
- SQL Server: https://www.microsoft.com/sql-server/sql-server-downloads
- SSMS: https://learn.microsoft.com/sql/ssms/download-sql-server-management-studio-ssms

## 2. Instalar e configurar o SQL Server

- **Instância padrão** (`MSSQLSERVER`), não `SQLEXPRESS`. Se o instalador
  perguntar, deixe a opção "Default instance".

Confira estes três pontos:

1. O serviço `SQL Server (MSSQLSERVER)` está em execução.
2. O protocolo TCP/IP está habilitado e a instância escuta na porta `1433`.
3. A autenticação mista (SQL Server + Windows) está habilitada, pois o projeto usa o login SQL `coletor`.

### Habilitar autenticação mista

No SSMS, conecte-se com **Windows Authentication**. Clique com o botão direito no servidor → **Properties** → **Security** → selecione **SQL Server and Windows Authentication mode**. Confirme e reinicie o serviço `SQL Server (MSSQLSERVER)`.

Para confirmar o modo, execute no SSMS:

```sql
SELECT SERVERPROPERTY('IsIntegratedSecurityOnly') AS SomenteWindows;
```

O resultado deve ser `0`. Se retornar `1`, o servidor ainda aceita somente autenticação do Windows.

### Conferir a porta

No PowerShell:

```powershell
Test-NetConnection localhost -Port 1433
```

O resultado esperado é `TcpTestSucceeded : True`. Se retornar `False`, consulte [`CASOS_DE_ERRO.md`](CASOS_DE_ERRO.md).

## 3. Criar o banco

1. Abra o SSMS e conecte-se ao servidor `localhost` usando **Windows Authentication**.
2. Abra `banco.sql` (File → Open → File).
3. Leia o script antes de executar e defina a senha do login `coletor` conforme indicado nele.
4. Execute com **Execute** ou `F5`.

O script deve exibir uma conferência indicando, entre outros itens, que o login `coletor` existe e que a tabela `Leituras` foi criada.

> Não execute scripts desconhecidos em um banco com dados importantes. Confira o conteúdo antes de executar.

## 4. Configurar a senha no projeto

Abra `src/main/resources/hibernate.cfg.xml` e substitua o marcador de senha pela **mesma senha definida no `banco.sql`**:

```xml
<property name="hibernate.connection.username">coletor</property>
<property name="hibernate.connection.password">SUA_SENHA_AQUI</property>
```

Não envie o arquivo com sua senha real para colegas ou para o repositório. Para a entrega, mantenha um marcador e instrua cada pessoa a configurar sua própria senha local.

A URL deve apontar para o banco correto, normalmente:

```text
jdbc:sqlserver://localhost:1433;databaseName=estacao_meteorologica;encrypt=true;trustServerCertificate=true
```

Use a URL que já está no projeto como referência e evite trocar parâmetros sem necessidade.

## 5. Abrir e executar no NetBeans

1. Selecione **File → Open Project** e abra a pasta do projeto.
2. Aguarde o Maven baixar as dependências na primeira execução.
3. Clique com o botão direito no projeto → **Clean and Build**.
4. Depois selecione **Run**.

O coletor executa uma coleta a cada 10 segundos até ser interrompido pelo botão Stop do NetBeans.

## 6. Conferir dados no banco

No SSMS, execute:

```sql
USE estacao_meteorologica;
SELECT TOP 20 * FROM dbo.Leituras ORDER BY id DESC;
```

## 7. Testar sem internet: simulador

Abra `src/main/resources/coletor.properties` e configure:

```properties
coletor.fonte=SIMULADOR
```

Para repetir sempre o mesmo cenário, configure também:

```properties
coletor.simulador.cenario=CHUVA_FORTE
```

Cenários disponíveis: `CHUVA_FORTE`, `CALOR_SECO`, `VENTANIA`, `GEADA` e `TEMPO_BOM`.

As leituras simuladas são gravadas com `origem = 'SIMULADOR'`. Para contar por origem:

```sql
SELECT origem, COUNT(*) AS quantidade
FROM dbo.Leituras
GROUP BY origem;
```

O intervalo e a localização podem ser ajustados no mesmo arquivo:

```properties
coletor.intervalo.segundos=10
coletor.latitude=-23.50
coletor.longitude=-47.46
```

Salve `coletor.properties` em UTF-8. Se uma propriedade parecer ser ignorada, confira a codificação e se a chave foi escrita corretamente.

## 8. Organização do projeto

```text
modelo         Leitura (entidade), Controle, Validacao, Configuracao, Agendador
DAL            FonteDados, OpenMeteoAPI, Simulador, LeituraDAO, HibernateUtil
apresentacao   ConsoleColetor
```

Fluxo dos dados:

```text
main → Controle → Validacao → LeituraDAO → Hibernate → SQL Server
```

O `LeituraDAO` isola o acesso ao banco. A interface `FonteDados` permite alternar entre API real e simulador pela configuração, sem alterar as classes de controle.

## 9. Antes de entregar

- Não inclua senha real no `hibernate.cfg.xml`, no código-fonte entregue ou na monografia.
- Inclua `banco.sql` como documentação da estrutura do banco.
- Registre os cinco cenários do simulador, o resultado esperado e o que foi gravado.
- Consulte [`CASOS_DE_ERRO.md`](CASOS_DE_ERRO.md) para diagnosticar problemas de instalação e execução.
