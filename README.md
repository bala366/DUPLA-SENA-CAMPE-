# Dupla Sena Campeã - Grupo 25 V1

Aplicativo Android em Java puro para GitHub Actions.

## Ideia

- Universo: 50 dezenas.
- Cada concurso da Dupla Sena contém 2 sorteios de 6 dezenas.
- Pesquisa um Grupo Campeão de 25 que maximize fechamentos 6/6 nos dois sorteios.
- O campeão é salvo no aparelho até o usuário estudar novamente.
- Procura também um Grupo Forte Mais Atrasado de 25.
- Cada grupo contém C(25,6) = 177.100 jogos, e todos são percorridos.
- Gera 2 jogos finais: Jogo 1 no campeão e Jogo 2 no atrasado.

## Perímetro duplo

O motor não mistura cegamente os dois sorteios. No ranking final mede separadamente:
- duques, ternos, quadras e quinas do 1º sorteio;
- duques, ternos, quadras e quinas do 2º sorteio;
- repetição contra o último 1º e o último 2º sorteio;
- frequência histórica e recente, atraso e tendência das dezenas;
- filtro estrutural aprendido: pares, primos, Fibonacci, 01-25/26-50, soma e sequência.

Jogos que já fizeram sena dentro do perímetro recente são eliminados.

## Grupo atrasado

C(50,25) é gigantesco. A busca do Grupo Campeão e do Grupo Atrasado usa pesquisa determinística por mutações/trocas e milhares de tentativas; portanto o aplicativo fala em "melhor grupo encontrado". O grupo atrasado precisa preservar uma fração relevante da força histórica do campeão para evitar escolher um grupo ruim apenas por estar atrasado.

## PDF

Até 2 páginas:
- verde = as 6 dezenas do jogo;
- vermelho = as 25 dezenas fora do grupo;
- branco = as outras 19 dezenas pertencentes ao grupo.

O PDF mostra também as métricas separadas do 1º e 2º sorteios.

## Compilação

O workflow usa Ubuntu, Temurin Java 17, Android SDK 35 e Gradle 8.9.
Rode `.github/workflows/build-apk.yml`.

Artefato esperado: `DUPLA-SENA-CAMPEA-GRUPO25-V1-APK`.
