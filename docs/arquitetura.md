# Arquitetura do backend

O backend é um monólito modular: uma aplicação Spring Boot, um processo de implantação e o mesmo banco. A organização segue responsabilidades de negócio. A reorganização preserva endpoints, DTOs HTTP, consultas, cálculos, relacionamentos JPA, rotinas de inicialização e transações existentes.

## Módulos

| Módulo | Responsabilidade |
| --- | --- |
| `ativo` | Catálogo de situações dos cadastros |
| `secretaria` | Secretarias e departamentos |
| `servidor` | Cadastro de servidores |
| `contrato` | Contratos, atas, anexos e notificações |
| `equipe` | Equipes, membros e funções vinculados a contratos e atas |
| `ferias` | Períodos aquisitivos, afastamentos e escala |
| `impressora` | Inventário, medições, faturamento e coleta |

`common` contém exceções e seu tratamento HTTP. Código de negócio deve pertencer a um módulo; por isso `Ativo` saiu de `common`. `config` contém configurações transversais.

## Estrutura de cada módulo

```text
modules/<modulo>/
├── api/          # Interfaces Java para outros módulos
├── controller/   # Endpoints HTTP e validação de entrada
├── dto/          # Contratos de entrada e saída existentes
├── model/        # Entidades e enums
├── repository/   # Persistência com Spring Data JPA
├── service/      # Casos de uso e regras de negócio
└── scheduler/    # Rotinas agendadas, quando necessárias
```

Um módulo pode acessar `api`, `model` e `dto` dos módulos permitidos. Não pode acessar seus controllers, serviços, schedulers ou repositórios. Controllers delegam aos serviços. Modelos e DTOs não dependem das camadas de execução ou persistência.

### Interfaces entre módulos

`SecretariaConsulta`, `ServidorConsulta`, `ContratoConsulta`, `AtaConsulta`, `FuncaoEquipeConsulta`, `EquipesVinculadas` e `AtivoConsulta` publicam somente operações utilizadas por outros módulos. Os repositórios internos implementam essas interfaces e o próprio proxy Spring Data existente atende a injeção. As interfaces não dependem do Spring Data.

Isso preserva consultas, entity graphs, bloqueios pessimistas, flushes e participação na transação do chamador. Os contratos mantêm os nomes dos métodos existentes para facilitar a revisão. Não expor todas as operações de `JpaRepository` em uma interface genérica.

### Relacionamentos preservados

As entidades JPA continuam compartilhando os relacionamentos existentes. `contrato` e `equipe`, por exemplo, possuem referências em ambas as direções. A arquitetura protege as implementações, mas não exige isolamento completo dos modelos nem ausência de ciclos entre entidades. Esses relacionamentos não foram substituídos por IDs, eventos ou chamadas remotas, pois isso alteraria persistência e carregamento.

As dependências permitidas estão em `ModularArchitectureTest`. Adicionar uma dependência exige revisar e documentar sua necessidade.

## Impressoras

```text
impressora/service/
├── inventario/   # Impressoras, instalações e locais
├── medicao/      # Leituras, grade e consumo
├── financeiro/
│   ├── EmpenhoImpressaoService       # Cadastro, sincronização e fachada
│   ├── ExecucaoOrcamentariaService   # Matriz, projeções e saldos
│   ├── FaturamentoImpressaoService   # Espelhos, faturas e consolidado
│   └── LoteImpressaoService          # Lotes, franquias e balanço
└── coleta/       # Coordenação, SNMP, HTTP e comprovantes
```

`EmpenhoImpressaoService` mantém as operações chamadas pelos controllers e delega consultas aos serviços especializados. As anotações transacionais da fachada e os corpos dos cálculos foram preservados. As rotinas de inicialização continuam no mesmo serviço. A coleta foi agrupada sem reescrever concorrência, comunicação ou geração de arquivos.

## Validação e manutenção

`ModularArchitectureTest` analisa dependências de bytecode e protege as fronteiras descritas. `FinanceiroImpressaoTest` verifica locação proporcional, excedentes, códigos dos itens, competência padrão, leituras sem vínculo, consolidado, projeções, saldos e a exceção existente do empenho SMED.

Os testes anteriores continuam presentes, com imports ajustados. No teste de substituição de impressora foi removido um stub não utilizado que já causava falha do Mockito antes da reorganização; as verificações de comportamento foram mantidas.

Executar a suíte local sem carregar a configuração real do banco:

```powershell
mvn clean test '-Dtest=!ApiApplicationTests'
```

`ApiApplicationTests` usa a configuração normal e precisa de ambiente próprio com banco e demais dependências. A aplicação possui rotinas de inicialização que modificam dados; esse teste não deve usar o banco em operação.

Após movimentar classes, compilar de forma limpa para remover bytecodes antigos. Para novas funcionalidades, escolher primeiro o módulo responsável e manter pequenos os contratos entre módulos. Mudanças em faturamento, SQL, startup ou estratégias de coleta são trabalhos separados desta reorganização.
