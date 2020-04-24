# Relatório do projeto Sauron

Sistemas Distribuídos 2019-2020, segundo semestre


## Autores

**Grupo A04**

 

| Número | Nome              | User                             | Email                                                     |
| -------|-------------------|----------------------------------| ----------------------------------------------------------|
| 89399  | Afonso Gonçalves  | <https://github.com/afonsocrg>   | <mailto:afonso.corte-real.goncalves@tecnico.ulisboa.pt>   |
| 89427  | Daniel Seara      | <https://github.com/Beu-Wolf>    | <mailto:daniel.g.seara@tecnico.ulisboa.pt>                |
| 89496  | Marcelo Santos    | <https://github.com/tosmarcel>   | <mailto:marcelocmsantos@tecnico.ulisboa.pt>               |


*(usar imagens com 150px de altura; e depois apagar esta linha)*  
![Alice](alice.png) ![Bob](bob.png) ![Charlie](charlie.png)

## Modelo de faltas

_(que faltas são toleradas, que faltas não são toleradas)_


## Solução

_(Figura da solução de tolerância a faltas)_

 A nossa solução passa por implementar a "Gossip architecture", como descrito no livro da cadeira. Cada Silo terá um conjunto de Câmeras e
 Observações considerados estáveis, que são o resultado de aplicar operações de atualização à réplica. O `ValueTS` é um `timestamp vetorial` que reflete os updates já aplicados,
contendo uma entrada por réplica.
 
 Para além disto, existe um log de update que guarda todas as operações de atualização que ainda não podem ser aplicadas. O `replicaTS` (também ele um `timestamp vetorial`) reflete todas
as operações de atualização aceites pela réplica , ou seja, todas as que já estão pelo menos no log de update.
 
As operações de atualização são `ctrl_clear`, `ctrl_init`, `cam_join` e `report`. Cada entrada no log de update e formada por um tuplo `<i, ts, op, prev, id>`, sendo que:
  * *i* - id da réplica que recebeu esta operação de atualização (este id é dado pelo número de instância da réplica);
  * *ts* - `timestamp vetorial` único desta operação (Dado por *prev* com a entrada correspondente à réplica *i* incrementada em 1 unidade)
  * *op* - operação de atualização e os seus argumentos
  * *prev* - `timestamp vetorial` do frontend que enviou este update
  * *id* - id único desta operação (dado pelo nome da câmera que enviou este update mais o número desta operação)
  
 Este id é importante para a estrutura final da réplica, o `Executed operation table`. Aqui guardam-se os ids das operações de atualização já estáveis, para garantir que não são executadas
mais do que uma vez.
 
 O frontend, por outro lado, contém apenas um `timestamp vetorial`, que reflete as operações de atualização mais recentes que conhece, e também um conjunto de Câmeras e Observações. Este 
conjunto guarda a versão mais atualizada de dados que o frontend recebeu. Tal garante que, caso seja contactada uma réplica desatualizada, o cliente vê sempre o mais recente, evitando leituras
incoerentes.


## Protocolo de replicação

_(Explicação do protocolo)_

_(descrição das trocas de mensagens)_


## Opções de implementação

_(Descrição de opções de implementação, incluindo otimizações e melhorias introduzidas)_



## Notas finais

_(Algo mais a dizer?)_
