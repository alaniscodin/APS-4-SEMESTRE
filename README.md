# Estação Meteorológica — como rodar na sua máquina

APS 2026/2 — Ciência da Computação (UNIP). O sistema tem **duas aplicações
separadas**, como pede o item 7 do enunciado:

| Pasta | Aplicação | O que faz |
|---|---|---|
| `ColetorMeteorologico` | **1 — Coletor** | A cada 10 s consulta a API de previsão do tempo (Open-Meteo), valida os dados e grava no banco. |
| `ServidorMeteorologico` | **2 — Servidor web** | Lê o banco e publica a página com os valores atuais e o gráfico em domínio do tempo. |

As duas só se comunicam **pelo banco**: o coletor grava, o servidor lê.

```
Open-Meteo ──► Coletor ──► SQL Server ──► Servidor web ──► Navegador
   (API)       (app 1)      (banco)        (app 2)        localhost:8080
```

A estação é fixa em **Sorocaba/SP** (latitude -23,50, longitude -47,46).

Siga os passos na ordem. Do zero, leva cerca de 40 minutos, quase tudo
esperando download.

---

## 1. O que você precisa instalar

| Programa | Observação |
|---|---|
| **SQL Server 2025 Express** | gratuito; o 2022 não está mais disponível para download |
| **SQL Server Management Studio (SSMS) 21** | para criar o banco |
| **Apache NetBeans 29** | já vem com o JDK e o Maven embutidos |

Se você já tem NetBeans com JDK 21 ou superior, pode usar o seu.

Os dois projetos precisam de **internet**: o coletor para consultar a API, e a
página para baixar a biblioteca do gráfico (Chart.js).

---

## 2. Instalar o SQL Server

Baixe o SQL Server 2025 Express em
<https://www.microsoft.com/pt-br/sql-server/sql-server-downloads> e escolha a
instalação **Básica**.

Três ajustes importam, e dois deles a instalação Básica já faz por padrão:

- **Instância padrão** (`MSSQLSERVER`), não `SQLEXPRESS`. Se o instalador
  perguntar, deixe a opção "Default instance".
- **Porta 1433** com TCP/IP habilitado.
- **Autenticação mista** (SQL + Windows). Se a instalação não perguntar, dá
  para ligar depois: SSMS → botão direito no servidor → Properties → Security
  → marque "SQL Server and Windows Authentication mode" → reinicie o serviço
  (botão direito no servidor → Restart).

Depois instale o **SSMS 21**, que é um download separado.

> **Por que isso importa:** os dois projetos se conectam em `localhost:1433`
> com usuário e senha. Se a sua instância tiver outro nome ou estiver sem
> autenticação mista, a conexão falha — e os erros do SQL Server não dizem
> isso com clareza (veja a seção "Problemas comuns").

---

## 3. Criar o banco

1. Abra o **SSMS**.
2. Na janela de conexão preencha:
   - **Server name:** `localhost` — só isso, sem `\SQLEXPRESS`
   - **Authentication:** Windows Authentication
   - **Trust server certificate:** **marque esta caixa**
3. Abra o arquivo **`banco.sql`** desta pasta (File → Open → File).
4. **Antes de executar**, procure a seção 4 do script e troque a senha
   `TroqueEstaSenha2026!` pela que você quiser. Anote: você vai precisar dela
   no passo seguinte.
5. Clique em **Execute** (ou F5).

No final, o script mostra uma tabelinha de conferência. Deve aparecer:

```
Colunas da tabela              13
Login coletor existe            1
Leituras gravadas ate agora     0
```

Se aparecer isso, o banco está pronto. O script pode ser executado de novo
sem problema: ele detecta o que já existe e não apaga nada.

---

## 4. Colocar a senha nos dois projetos

Cada projeto tem o seu `hibernate.cfg.xml`. Abra **os dois**

- `ColetorMeteorologico/src/main/resources/hibernate.cfg.xml`
- `ServidorMeteorologico/src/main/resources/hibernate.cfg.xml`

e troque `SUA_SENHA_AQUI` pela senha que você definiu no passo anterior:

```xml
<property name="hibernate.connection.password">SUA_SENHA_AQUI</property>
```

Essa é a **única** alteração obrigatória. O resto já aponta para
`localhost:1433` e para o banco `estacao_meteorologica`.

---

## 5. Rodar o coletor (aplicação 1)

1. No NetBeans: **File → Open Project** e escolha a pasta
   `ColetorMeteorologico`.
2. Espere o NetBeans baixar as dependências (Hibernate e o driver do SQL
   Server, cerca de 20 MB na primeira vez). A barra de progresso fica no
   canto inferior direito.
3. Botão direito no projeto → **Run**.

O programa coleta de **10 em 10 segundos** e continua rodando até você parar.
Cada coleta imprime algo assim:

```
========== LEITURA METEOROLÓGICA ==========
Coletada em .......... 12/10/2026 18:30:26
Observação da fonte .. 12/10/2026 18:30:00
Origem ............... API
Temperatura .......... 20.6 °C
Umidade .............. 91 %
Precipitação ......... 0.2 mm
Chovendo ............. Sim
Vento ................ 6.8 km/h
Pressão .............. 946.1 hPa
===========================================
Leitura gravada com id 1.
```

Para parar: o botão vermelho de stop do NetBeans.

> **Rode um coletor só.** A API gratuita aceita até 10.000 consultas por dia;
> um coletor a cada 10 s faz 8.640. Dois coletores ao mesmo tempo (mesmo em
> computadores diferentes da mesma rede) estouram o limite.

Para conferir no banco, rode isto no SSMS:

```sql
USE estacao_meteorologica;
SELECT TOP 20 * FROM Leituras ORDER BY id DESC;
```

---

## 6. Rodar o servidor web (aplicação 2)

1. No NetBeans: **File → Open Project** e escolha a pasta
   `ServidorMeteorologico`. Pode deixar o coletor aberto e rodando.
2. Botão direito no projeto → **Run**.
3. Quando aparecer no console:

   ```
   Servidor da estação meteorológica no ar.
   Abra no navegador: http://localhost:8080
   ```

   abra **<http://localhost:8080>** em qualquer navegador.

Na primeira vez, o Windows pode perguntar se permite o Java na rede
(Firewall). Pode permitir em **redes privadas**: isso deixa outros aparelhos
da mesma rede abrirem a página pelo IP do computador.

### O que a página mostra

- **Condição atual:** temperatura, sensação térmica, descrição do tempo
  ("Céu limpo", "Garoa moderada"…) com ícone, umidade, pressão, vento com
  direção e precipitação.
- **Gráfico em domínio do tempo:** as últimas 60 leituras do banco
  (10 minutos) de temperatura, umidade, vento e precipitação. A umidade usa o
  eixo da direita (0 a 100 %); as outras, o da esquerda, que se ajusta sozinho
  aos valores.
- **Resumo do dia:** quantas leituras de hoje tiveram chuva, mínima e máxima.
- **Barra de estado** (rodapé):

  | Bolinha | Mensagem | Significa |
  |---|---|---|
  | verde | Ao vivo · leitura nº … | tudo funcionando |
  | amarela | Coletor parado? A última leitura foi há … | o servidor está ok, mas o coletor não grava há mais de 30 s |
  | amarela | Nenhuma leitura no banco ainda | o banco está vazio: rode o coletor |
  | vermelha | Sem conexão com o servidor | o ServidorMeteorologico foi encerrado |
  | vermelha | Falha ao iniciar o Hibernate / Erro ao consultar… | problema no banco (senha, serviço parado) |

A página **Configurações** guarda preferências só deste navegador: de quanto
em quanto tempo a página atualiza, °C ou °F, e quais variáveis aparecem no
gráfico. A coleta em si (fonte, intervalo) é configurada no coletor — seção 7.

### A API do servidor

A página busca os dados nestes endereços, que também podem ser abertos
direto no navegador para conferir:

| Endereço | Devolve |
|---|---|
| <http://localhost:8080/api/leituras/ultima> | a leitura mais recente |
| <http://localhost:8080/api/leituras?limite=60> | as últimas N leituras, da mais antiga para a mais nova (N de 1 a 360) |
| <http://localhost:8080/api/resumo> | totais de hoje |

Exemplo de leitura:

```json
{
  "id": 76,
  "dataHoraColeta": "2026-10-17T22:01:13",
  "dataHoraObservacao": "2026-10-17T22:00:00",
  "temperatura": 18.4,
  "umidade": 86,
  "precipitacao": 0.0,
  "chovendo": false,
  "velocidadeVento": 3.8,
  "direcaoVento": 177,
  "pressao": 944.9,
  "sensacaoTermica": 19.9,
  "codigoTempo": 0,
  "origem": "API",
  "condicao": "Céu limpo",
  "icone": "sol"
}
```

Unidades: °C, %, mm, km/h, graus, hPa. Datas no horário de Brasília.

Em caso de erro, a resposta é `{"erro": "mensagem"}` com o código HTTP:
400 (parâmetro inválido), 404 (sem leituras ou endereço inexistente),
405 (método diferente de GET), 503 (banco indisponível).

---

## 7. Testar com o simulador (item 9e)

O item 9e do enunciado pede testes simulando condições meteorológicas para
verificar a **coleta e a publicação**. O coletor tem um **simulador** com
cinco cenários fixos:

| Cenário | Temp. | Umid. | Precip. | Vento | Pressão | Código WMO | A página mostra |
|---|---|---|---|---|---|---|---|
| `CHUVA_FORTE` | 18,4 °C | 98 % | 12,5 mm | 35,2 km/h | 940,2 hPa | 65 | Chuva forte |
| `CALOR_SECO` | 36,5 °C | 22 % | 0 | 8,1 km/h | 948,7 hPa | 0 | Céu limpo |
| `VENTANIA` | 24,1 °C | 55 % | 0 | 78,3 km/h | 941,5 hPa | 3 | Nublado |
| `GEADA` | -1,5 °C | 85 % | 0 | 5,4 km/h | 952,3 hPa | 48 | Nevoeiro com geada |
| `TEMPO_BOM` | 25,3 °C | 60 % | 0 | 10,2 km/h | 946,0 hPa | 1 | Predominantemente limpo |

Os valores são sempre os mesmos, de propósito: assim o teste pode ser
repetido e o resultado conferido. Dados aleatórios dariam um resultado
diferente a cada execução, impossível de descrever na monografia.

Para usar, abra **`ColetorMeteorologico/src/main/resources/coletor.properties`**
e mude:

```properties
coletor.fonte=SIMULADOR
```

Rodando assim, cada coleta usa um cenário diferente, em ordem. Para repetir
sempre o mesmo, preencha também:

```properties
coletor.simulador.cenario=CHUVA_FORTE
```

A leitura simulada faz **o caminho completo**: coletor → validação → banco →
servidor → página. Na página, aparece o selo **"dados simulados"** ao lado de
"Sorocaba, SP", e o gráfico mostra o salto de valores. No banco, essas
leituras ficam com `origem = 'SIMULADOR'`, separadas das reais:

```sql
SELECT origem, COUNT(*) FROM Leituras GROUP BY origem;
```

**Roteiro sugerido para a monografia**, para cada cenário:
1. Configure o cenário e rode o coletor.
2. Registre a linha gravada no banco (consulta acima ou `SELECT TOP 1 ...`).
3. Tire um print da página com o valor e o ícone.
4. Compare com a tabela acima: o esperado tem que bater com o banco e com a página.

O mesmo arquivo permite mudar o intervalo e o local:

```properties
coletor.intervalo.segundos=10
coletor.latitude=-23.50
coletor.longitude=-47.46
```

Ao terminar os testes, volte para `coletor.fonte=API`.

> **Cuidado ao editar:** salve o arquivo em **UTF-8 sem BOM**. O Bloco de
> Notas do Windows salva *com* BOM, e isso faz a **primeira** propriedade do
> arquivo ser ignorada em silêncio. O projeto já contorna esse caso, mas se
> você editar por outro programa e algo não pegar, é a primeira coisa a
> checar. Use o editor do NetBeans e não terá esse problema.

---

## 8. Problemas comuns

As mensagens de erro do SQL Server raramente dizem a causa real. Esta lista
traduz as que mais aparecem.

### "Could not open a connection to SQL Server (error: 40)" / "Erro 53"

Nome do servidor errado. Use `localhost`, sem `\SQLEXPRESS`. Esse sufixo é
para instância nomeada; a instalação Básica cria a instância padrão, que não
leva sufixo.

Se persistir, verifique se o serviço está rodando: tecle Win+R, digite
`services.msc` e procure **SQL Server (MSSQLSERVER)**.

### "The driver could not establish a secure connection ... (SSL)"

Falta o `encrypt=false` na URL, ou o "Trust server certificate" desmarcado no
SSMS. O driver liga criptografia por padrão e o certificado do servidor é
autoassinado.

### "Login failed for user 'coletor'"

A senha do `hibernate.cfg.xml` não bate com a do banco, ou a autenticação
mista não está ligada. Lembre que são **dois** arquivos, um em cada projeto.
Para redefinir a senha, rode no SSMS:

```sql
ALTER LOGIN coletor WITH PASSWORD = 'NovaSenha2026!';
```

e coloque a mesma senha nos dois `hibernate.cfg.xml`.

### "Schema-validation: missing table [Leituras]"

O `banco.sql` não foi executado, ou foi executado em outro banco. Confira no
SSMS se `estacao_meteorologica` aparece em Databases e se tem a tabela
`Leituras` dentro de `dbo`.

Essa mensagem vem do `hbm2ddl.auto=validate`: o projeto confere se a tabela
bate com a classe **antes** de começar, de propósito. É melhor falhar na
largada do que no meio do uso.

### "A porta 8080 já está em uso"

O servidor já está aberto em outra janela do NetBeans (pare a outra), ou
outro programa usa a porta 8080. Para usar outra porta: botão direito no
projeto → Properties → Run → VM Options: `-Dservidor.porta=8081`, e abra
`http://localhost:8081`.

### A página mostra "Coletor parado?"

O servidor e o banco estão funcionando, mas o coletor não está gravando.
Confira se ele está rodando e se o console dele mostra algum `[ERRO]`.

### A página mostra "Abra a página pelo servidor"

O `index.html` foi aberto com dois cliques, direto do disco. A página precisa
ser aberta por <http://localhost:8080>, senão não consegue buscar os dados.

### O gráfico não aparece, mas os valores sim

A biblioteca do gráfico (Chart.js) é baixada da internet ao abrir a página.
Sem internet, a página avisa no rodapé e continua mostrando os valores.

### "Tempo esgotado ao consultar a API" ou "Não foi possível conectar"

Sem internet, ou a API fora do ar. Não quebra o coletor: a coleta seguinte
tenta de novo. Para trabalhar sem internet, use o simulador (seção 7).

### O assistente de entidades do NetBeans não lista nenhuma tabela

Ao criar a conexão, na tela **Select database schema**, escolha **`dbo`**. A
lista mostra também `db_accessadmin`, `db_datareader` e outros, que são
*roles*, não schemas — e não contêm tabela nenhuma.

Você não precisa gerar entidade: a classe `Leitura` já está pronta nos dois
projetos. Isso só importa se for recriar do zero.

---

## 9. Como o sistema está organizado

Os dois projetos usam os três pacotes do modelo das aulas.

### Coletor (`com.mycompany.coletormeteorologico`)

```
modelo         Leitura (entidade), Controle, Validacao, Configuracao, Agendador
DAL            FonteDados, OpenMeteoAPI, Simulador, LeituraDAO, HibernateUtil
apresentacao   ConsoleColetor
```

Caminho do dado: `main → Controle → Validacao → LeituraDAO → Hibernate → SQL Server`

### Servidor web (`com.mycompany.servidormeteorologico`)

```
modelo         Leitura (entidade), Controle, Validacao, CondicaoTempo, ResumoDia
DAL            LeituraDAO, HibernateUtil
apresentacao   RotasAPI (API JSON), ArquivosEstaticos (entrega a página)
recursos/web   index.html, configuracoes.html, estacao.css, estacao.js
```

Caminho do dado: `navegador → RotasAPI → Controle → Validacao → LeituraDAO → Hibernate → SQL Server`

O servidor web usa o servidor HTTP que já vem no Java
(`com.sun.net.httpserver`), sem framework: o caminho do dado fica explícito do
DAO até a página. Página e API saem do mesmo endereço, então o navegador não
bloqueia as chamadas (sem problema de CORS).

### Pontos de projeto que vão aparecer na monografia

**Padrão DAO** (exigido pelo item 4 do enunciado). Nos dois projetos, só o
`LeituraDAO` sabe que existe um banco. O `Controle` pede "grave esta leitura"
ou "me dê as últimas 60" e não faz ideia se por trás tem SQL Server, MySQL ou
arquivo texto.

**Interface `FonteDados`** (coletor). A API real e o simulador implementam a
mesma interface, então trocar um pelo outro é mudar uma linha do
`coletor.properties` — nenhuma classe precisa ser alterada. É o que permite
o teste do item 9e passar pelo caminho completo, igualzinho aos dados reais.

**Duas datas por leitura.** `dataHoraColeta` é quando o coletor perguntou
(muda a cada 10 s); `dataHoraObservacao` é quando a Open-Meteo mediu (ela só
atualiza a cada 15 minutos, então esta se repete). Por isso várias leituras
seguidas têm os mesmos valores: o coletor grava todas, como o enunciado manda.

**Código WMO.** A API informa a condição do céu por um número da Organização
Meteorológica Mundial (0 = céu limpo, 65 = chuva forte, 95 = trovoada…). O
enum `CondicaoTempo` traduz cada código para português e escolhe o ícone.

**Tratamento de erros** (item 10). Erros viram mensagem, não travam o
programa: uma coleta que falha não para o coletor, e um banco fora do ar não
derruba o servidor — a página mostra o problema no rodapé e volta sozinha
quando o banco volta.

**Pressão.** A pressão mostrada (~945 hPa) é a pressão na superfície, a 589 m
de altitude, e não a pressão ao nível do mar (~1013 hPa) que aparece em
boletins. Por isso parece "baixa".

---

## 10. Antes de entregar

- A senha **não** pode ir nos `hibernate.cfg.xml` do material entregue: o
  item 9 do enunciado manda anexar o código-fonte como apêndice da
  monografia, e ela ficaria impressa no documento.
- O arquivo `banco.sql` serve como anexo da monografia: ele documenta a
  estrutura do banco.
- Os cinco cenários do simulador são o material do item 9e (seção 7). Para
  cada um, registre o que foi simulado, o que era esperado e o que apareceu
  no banco e na página.
