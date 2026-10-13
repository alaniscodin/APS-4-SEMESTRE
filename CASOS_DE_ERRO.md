# Caso de erro — Coletor Meteorológico

Use este guia quando o projeto não conectar ao SQL Server, o Hibernate não iniciar ou a coleta falhar. Resolva **na ordem**: serviço → porta → autenticação → usuário/senha → banco/tabela → API.

## 1. Hibernate: `Connection refused` em `localhost:1433`

**O que significa:** o programa não conseguiu abrir uma conexão TCP com o SQL Server. Não é, em geral, um erro de senha.

### Verificações

1. Pressione `Win + R`, digite `services.msc` e pressione Enter.
2. Procure `SQL Server (MSSQLSERVER)` e confirme que está **Running/Em execução**.
3. Abra o PowerShell e execute:

   ```powershell
   Test-NetConnection localhost -Port 1433
   ```

4. Se retornar `TcpTestSucceeded : False`, execute:

   ```powershell
   netstat -ano | findstr :1433
   ```

   Se não aparecer nenhuma linha com `LISTENING`, o SQL Server provavelmente não está escutando nessa porta.

5. Abra **SQL Server Configuration Manager** → **SQL Server Network Configuration** → **Protocols for MSSQLSERVER**. Habilite `TCP/IP`, se estiver desabilitado.
6. Em `TCP/IP` → **Properties** → **IP Addresses** → `IPAll`, confira a porta configurada. Para este projeto, use a porta fixa `1433` e deixe `TCP Dynamic Ports` vazio.
7. Reinicie `SQL Server (MSSQLSERVER)` e teste a porta de novo.

> Se `Test-NetConnection` retornar `True`, não continue alterando a porta. Passe para o diagnóstico de autenticação.

## 2. Hibernate: `Login failed for user 'coletor'` / `Falha de logon do usuário 'coletor'`

**O que significa:** o SQL Server foi alcançado, mas recusou a autenticação do login SQL. As causas mais comuns são senha divergente, login inexistente/desabilitado ou autenticação mista desativada.

### A. Confirmar autenticação mista

1. Abra o SSMS e conecte-se usando **Windows Authentication**.
2. Clique com o botão direito no servidor → **Properties** → **Security**.
3. Selecione **SQL Server and Windows Authentication mode**.
4. Confirme e reinicie o serviço `SQL Server (MSSQLSERVER)`.

Execute esta consulta no SSMS:

```sql
SELECT SERVERPROPERTY('IsIntegratedSecurityOnly') AS SomenteWindows;
```

- `0` = autenticação mista habilitada.
- `1` = somente autenticação do Windows; habilite o modo misto e reinicie o serviço.

### B. Conferir se o login existe e está habilitado

Conectado ao SSMS com uma conta administrativa, execute:

```sql
SELECT name, is_disabled
FROM sys.sql_logins
WHERE name = 'coletor';
```

- Nenhuma linha = o login não existe nessa instância.
- `is_disabled = 1` = login desabilitado.
- `is_disabled = 0` = login habilitado; confirme a senha e o acesso ao banco.

### C. Redefinir a senha, se necessário

Somente se você tiver permissão administrativa e quiser definir uma nova senha, execute no SSMS:

```sql
ALTER LOGIN [coletor]
WITH PASSWORD = 'Substitua_Por_Uma_Senha_Forte';
ALTER LOGIN [coletor] ENABLE;
```

Use uma senha própria e não a compartilhe. Em seguida, atualize `hibernate.cfg.xml` com **exatamente a mesma senha**:

```xml
<property name="hibernate.connection.username">coletor</property>
<property name="hibernate.connection.password">SUA_SENHA_AQUI</property>
```

Se `ALTER LOGIN` falhar porque o login não existe, execute o `banco.sql` do projeto após revisar as instruções de senha nele.

### D. Testar o login fora do Hibernate

No SSMS, tente conectar com:

- Server name: `localhost,1433`
- Authentication: `SQL Server Authentication`
- Login: `coletor`
- Password: a mesma senha configurada no `hibernate.cfg.xml`

Se falhar aqui, o problema ainda é de autenticação SQL Server; não é necessário alterar o Hibernate até o login funcionar.

## 3. Erro SSL/TLS: `The driver could not establish a secure connection`

O driver JDBC pode exigir criptografia e rejeitar o certificado local.

- No SSMS, ao conectar, marque **Trust server certificate**, se a opção estiver disponível.
- Confira a URL JDBC do projeto. Para um ambiente local de desenvolvimento, a configuração pode usar `encrypt=true;trustServerCertificate=true`, por exemplo:

  ```text
  jdbc:sqlserver://localhost:1433;databaseName=estacao_meteorologica;encrypt=true;trustServerCertificate=true
  ```

`trustServerCertificate=true` não resolve porta fechada nem senha incorreta. Em produção, prefira um certificado confiável em vez de ignorar a validação do certificado.

## 4. `Schema-validation: missing table [Leituras]`

**O que significa:** o Hibernate não encontrou a tabela esperada no banco selecionado.

1. No SSMS, confirme que existe o banco `estacao_meteorologica`.
2. Expanda **Databases → estacao_meteorologica → Tables**.
3. Confirme que existe `dbo.Leituras`.
4. Se o banco ou a tabela não existir, abra e execute o `banco.sql` do projeto.
5. Confira se a URL JDBC aponta para `databaseName=estacao_meteorologica`.

O projeto usa validação de schema para detectar divergências antes de iniciar. Não mude para criação automática de tabelas apenas para esconder o erro.

## 5. A tabela existe, mas o login não consegue acessá-la

Conectado ao SSMS com uma conta administrativa, confirme que o login `coletor` está associado a um usuário no banco:

```sql
USE [estacao_meteorologica];

SELECT name, type_desc
FROM sys.database_principals
WHERE name = 'coletor';
```

Se o usuário do banco não existir, e o login de servidor já existir, um administrador pode criá-lo e dar permissões de leitura/escrita:

```sql
USE [estacao_meteorologica];

CREATE USER [coletor] FOR LOGIN [coletor];
ALTER ROLE db_datareader ADD MEMBER [coletor];
ALTER ROLE db_datawriter ADD MEMBER [coletor];
```

Execute `CREATE USER` somente se o usuário ainda não existir. Se o script `banco.sql` já configura esse acesso, prefira executá-lo e seguir as instruções do projeto.

## 6. NetBeans não lista tabelas ao criar conexão

Ao usar o assistente de entidades, selecione o schema `dbo`. Itens como `db_datareader` e `db_datawriter` são roles, não schemas.

A entidade `Leitura` já faz parte deste projeto; normalmente não é necessário gerar outra entidade.

## 7. API: `Tempo esgotado ao consultar a API` / `Não foi possível conectar`

Pode ser falta de internet, bloqueio de rede ou indisponibilidade temporária da API.

- O coletor pode tentar novamente na próxima coleta.
- Para testar sem internet, abra `src/main/resources/coletor.properties` e defina:

  ```properties
  coletor.fonte=SIMULADOR
  ```

- Para testar um cenário fixo:

  ```properties
  coletor.simulador.cenario=CHUVA_FORTE
  ```

Cenários disponíveis: `CHUVA_FORTE`, `CALOR_SECO`, `VENTANIA`, `GEADA`, `TEMPO_BOM`.

## 8. A configuração de `coletor.properties` parece ser ignorada

- Confira a grafia exata das propriedades.
- Salve o arquivo em UTF-8.
- Reinicie a aplicação após editar.
- Verifique se editou `src/main/resources/coletor.properties` dentro do projeto correto.

## 9. NetBeans não baixa dependências ou não compila

1. Confira a conexão com a internet.
2. Aguarde o Maven terminar de baixar as dependências.
3. Clique com o botão direito no projeto → **Clean and Build**.
4. Leia a primeira mensagem de erro de compilação no painel Output; erros posteriores podem ser consequência do primeiro.

## 10. Antes de pedir ajuda, envie estas informações

Não envie senhas, tokens ou dados pessoais. Copie:

- A mensagem de erro completa.
- O resultado de `Test-NetConnection localhost -Port 1433`.
- O valor de `SomenteWindows` (0 ou 1).
- Se o login `coletor` consegue conectar no SSMS.
- Se o banco `estacao_meteorologica` e a tabela `dbo.Leituras` existem.
- A versão do Java e do SQL Server, se souber.

**Nunca publique a senha real** do SQL Server ou um `hibernate.cfg.xml` que a contenha.
