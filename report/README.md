# Relatório do projeto Sauron

Sistemas Distribuídos 2019-2020, segundo semestre


## Autores

**Grupo A04**

 

| Número | Nome              | User                             | Email                                                     |
| -------|-------------------|----------------------------------| ----------------------------------------------------------|
| 89399  | Afonso Gonçalves  | <https://github.com/afonsocrg>   | <mailto:afonso.corte-real.goncalves@tecnico.ulisboa.pt>   |
| 89427  | Daniel Seara      | <https://github.com/Beu-Wolf>    | <mailto:daniel.g.seara@tecnico.ulisboa.pt>                |
| 89496  | Marcelo Santos    | <https://github.com/tosmarcel>   | <mailto:marcelocmsantos@tecnico.ulisboa.pt>               |

 
![Afonso](ist189399.png) ![Daniel](ist189427.png) ![Marcelo](ist189496.png)

## Melhorias da primeira parte

 * O conjunto de testes foi melhorado (Commits: [Load testing](https://github.com/tecnico-distsys/A04-Sauron/commit/70a1cda17eb81cea50e55d32ae13052a0b54d1af) e [Verificação de coordenadas](https://github.com/tecnico-distsys/A04-Sauron/commit/a7f573348f1d560f1c656fc5e5258a5a4123c529))
 * A resposta ao `CamInfo` passa a retornar apenas as coordenadas da câmara, em vez da câmara em si ([Commit](https://github.com/tecnico-distsys/A04-Sauron/commit/c7bc13b00d9e540ea367eb94c47af874e8a7642b))
 * Os objetos de representação de informação usados no cliente (`...DTO`) foram renomeados para objetos do domínio. Os objetos do domínio do cliente e servidor não foram unificados para manter abstração e isolamento dos dois. (Commits: [rename dto](https://github.com/tecnico-distsys/A04-Sauron/commit/1146664b562161ba149b6084e0632dc6340382a5) e [dto package to domain](https://github.com/tecnico-distsys/A04-Sauron/commit/e77937cbea6a4430804c80e63f7396d58b455a6f)
 * Os requests de gRPC que eram feitos usando streams e metadados foram alterados para usar campos normais e `repeated`. Todos os argumentos eram explicitamente passados dentro do corpo da mensage. As alterações foram desenvolvidas incrementalmente ao longo de vários commits, no branch `grpcRefactor`. (Links para [Último commit](https://github.com/tecnico-distsys/A04-Sauron/commit/4101f632e92975d5170ee29e798c154b73eb18da) e [Histórico de commits](https://github.com/tecnico-distsys/A04-Sauron/commits/grpcRefactor) do branch)
 * A mensagem usada para fazer pedidos de `Track`, `Trace` e `TrackMatch` foi dividida em diferentes mensagens, uma para cada request. ([Commit](https://github.com/tecnico-distsys/A04-Sauron/commit/9fb98d61550271844a01b61e8fb640826241dbcb))
*(que correções ou melhorias foram feitas ao código da primeira parte -- incluir link para commits no GitHub onde a alteração foi feita)*

## Modelo de faltas
**TODO: STILL WIP**
_(que faltas são toleradas, que faltas não são toleradas)_

As faltas toleradas são as requeridas pelo Modelo de interação de faltas do enunciado deste projeto:

 * Crash de uma réplica sem updates críticos para o estado de outras réplicas
 * Updates enviados fora de ordem
 * Crash no fronted de cada cliente
 
Por outro lado, não toleramos:
 * Falha no ZooKeeper
 * Crash numa réplica com updates críticos para o estado de outras réplicas
 * Instâncias de réplicas atribuídas de forma não incremental

## Solução

_(Figura da solução de tolerância a faltas)_

_(Breve explicação da solução, suportada pela figura anterior)_


## Protocolo de replicação
_(Explicação do protocolo)_

_(descrição das trocas de mensagens)_

O protocolo seguido foi uma variante do _Gossip_ estudado nas aulas práticas. As réplicas respondem sempre ao cliente,
garantindo uma alta disponibilidade e tolerância a partições de rede. No entanto seguem uma filosofia de updates "relaxados",
em que apenas é garantido que eventualmente as réplicas estarão coerentes. O segredo do protocolo reside em **timestamps vetoriais**
que permitem representar o estado de cada réplica através dos seus updates mais atuais. Estes timestamps são trocados entre réplicas
e entre clientes e réplicas, de forma a poder responder a pedidos e atualizar cada réplica.

Outros dos aspetos importantes é o `update log`, uma lista dos updates aceites por uma réplica, mas ainda não aplicados ao
sistema. Cada entrada deste log contém:
* identificador da réplica que o enviou
* timestamp vetorial único
* identificador da operação
* `prev` (explicado com mais detalhe abaixo)
* update a ser executado

Existem 3 tipos de mensagens relevantes:

 **1 - Mensagens de Query**
 
 Nestas mensagens, o cliente faz um pedido por informação a uma réplica enviando o seu timestamp (`prev`) representativo da última
 versão do sistema vista por ele. A réplica responde com a informação pedida e outro timestamp (`ValueTS`), que representa os updates
 estáveis do sistema. O cliente, de seguida, verifica se a informação pedida é mais recente que a do seu pedido anterior. Se sim, apresenta-a,
 se não, aprensenta o que tinha guardado na sua "cache".
 
 **2 - Mensagens de Update**
 
 Nestas mensagens, o cliente, para além do `prev` anteriormente falado, envia também uma representação do update a executar e um identificador
 único do mesmo. Isto (aliado ao `update log`), garante que não se executará a mesma operação mais que
 1 vez. Ao receber esta mensagem, o servidor adiciona este update a uma estrutura de updates por executar.
 
 **3 - Mensagens de Gossip**
 
 Estas mensagens ocorrem sempre com um dado intervalo de tempo entre elas (30 segundos, por norma). Uma dada réplica junta os seus updates que estima 
 que a réplica a que quer enviar não tenha. Envia também o seu `replicaTS`, um timestamp indicativo dos updates aceites mas ainda não executados.
 A réplica que recebe esta mensagem, junta os updates aos que já tem, e tenta verificar quais podem ser executados. Todas as réplicas enviam a todas as 
 outras réplicas que conhecem.


## Opções de implementação

_(Descrição de opções de implementação, incluindo otimizações e melhorias introduzidas)_



## Notas finais

_(Algo mais a dizer?)_
