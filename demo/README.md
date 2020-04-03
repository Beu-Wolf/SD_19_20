# Sauron demonstration guide

Já com o projeto instalado:

Instanciar o servidor:
```
cd silo-server
mvn exec:java
cd ..
```

## Caso 1: Carregar Dados de teste usando o spotter

```
./spotter/target/appassembler/bin/spotter localhost 8080 < initSilo.txt
```

## Caso 2: Usar eye para carregar mais observações, com e sem erros

```
./eye/target/appassembler/bin/eye localhost 8080 testCam2 12.456789 -8.987654
person,89427
person,89399
person,89496
\n
```
Verificar resposta de sucesso

```
person R4a_
\n
```
Verificar resposta de id de pessoa inválido

```
car,20SD20
car,AA00AA
\n
```
Verificar resposta de sucesso

```
car, 124_87
\n
```
Verificar resposta de id de carro inválido

Premir `^C` para sair do cliente Eye

## Caso 3: Verificar operação sleep do Eye

```
./eye/target/appassembler/bin/eye localhost 8080 testCam2 12.456789 -8.987654
zzz,1000
\n
```
Verificar pausa de 1 segundo

## Caso 4: Uso do Spotter para executar pesquisas

```
./spotter/target/appassembler/bin/spotter localhost 8080
help
```
Verificar que aparece ecrã de ajuda com comandos suportados

```
spot person 1234
spot car 20SD20
spot person 0101
spot car 7T_Ea2
```
Verificar que

* Pessoa 1234 foi observada
* Carro 20SD20 foi observado
* Pessoa 0101 não foi observada
* Pesquisa de carro 7T_Ea2 é inválida

```
spot person *
spot person 123*
spot person *7
spot car 20*
spot car NE*
```
Verificar que

* todas as pessoas aparecem ordenadas pelo seu id
* todas as pessoas com id começado por 123 aparecem, ordenadas pelo seu id
* todas as pessoas acabadas em 7 aparecem ordenadas pelo seu id
* todos os carros começados por 20 aparecem ordenados pelo seu id
* Não existem observações de carros começados por NE


```
trail person 89427
trail car 20SD20
trail person 0101
trail car 7T_Ea2
```

Verificar que

* a pessoa 89427 foi identificada nas cameras testCam2, camera2, camera1
* o carro 20SD20 foi identificado nas cameras testCam2, camera4, camera3
* a pessoa com id 01010 nunca foi identificada
* o carro pedido tem id inválido

```
exit 
```

## Caso 5: Uso do spotter para operações de controlo

Executar novo spotter
```
./spotter/target/appassembler/bin/spotter localhost 8080
help
```

```
ping friend
```
Verificar que servidor responde com "Hello friend!"

```
clear
spot person *
spot car *
```
Verificar que já não existe qualquer pessoa ou carro no servidor

```
init cams
$ mockCamera1,14.645678,8.534568
$ mockCamera2,19.994536,7.789765
$ done
```
Verificar sucesso no registo das cameras

```
init obs
$ mockCamera1, person, 89399
$ mockCamera2, car, 20SD21
$ mockCamera1, car, 20SD21
$ mockCamera2, person 89399
$ done
```

Verificar sucesso no registo das operações

```
exit 
```