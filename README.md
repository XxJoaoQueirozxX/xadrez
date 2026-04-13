# Chess System (Jogo de Xadrez)

Este é um projeto completo de um sistema de jogo de xadrez desenvolvido em **Java**. O projeto abrange desde a lógica core do tabuleiro e movimentos das peças até interfaces de usuário (CLI e GUI) e integração com inteligência artificial.

## 🚀 Funcionalidades

- **Motor de Jogo Completo**: Implementação de todas as regras do xadrez, incluindo movimentos especiais como:
  - **Roque** (Castling)
  - **En Passant**
  - **Promoção de Peão**
  - **Check e Checkmate**
- **Duas Interfaces de Usuário**:
  - **Console (CLI)**: Interface baseada em texto com cores ANSI para terminais.
  - **Interface Gráfica (GUI)**: Interface moderna desenvolvida com **Java Swing**, incluindo suporte a mouse e visualização em tempo real.
- **Inteligência Artificial (IA)**:
  - **IA Nativa**: Algoritmo de busca e avaliação de jogadas baseado em valor de peças e posicionamento central.
  - **Integração Stockfish**: Suporte para o motor Stockfish (via protocolo UCI) para enfrentar uma das IAs mais fortes do mundo diretamente na interface gráfica.

## 🧠 Conceitos de Programação Utilizados

Este projeto foi construído aplicando conceitos avançados de Programação Orientada a Objetos (POO) e boas práticas:
- **Herança e Polimorfismo**: Para a estrutura genérica de peças e comportamentos específicos.
- **Encapsulamento**: Proteção da lógica do tabuleiro e estado da partida.
- **Classes e Métodos Abstratos**: Definição de contratos para as peças do jogo.
- **Tratamento de Exceções**: Camada personalizada para erros de tabuleiro e jogadas inválidas.
- **Enumerações (Enums)**: Para representação de cores e estados.
- **Expressões Lambda e Streams**: Utilizados para manipulação eficiente de coleções de peças capturadas.

## 📸 Demonstração

### Interface Gráfica (GUI)
<p align="center">
  <img src="https://github.com/XxJoaoQueirozxX/Jogo-xadrez/blob/master/print/CHESS-GUI-01.png?raw=true" width="400" alt="Tabuleiro GUI">
  <img src="https://github.com/XxJoaoQueirozxX/Jogo-xadrez/blob/master/print/CHESS-GUI-02.png?raw=true" width="400" alt="Movimento GUI">
  <img src="https://github.com/XxJoaoQueirozxX/Jogo-xadrez/blob/master/print/CHESS-GUI-OPTIONS.png?raw=true" width="400" alt="Opções da GUI">
</p>

### Interface Console (CLI)
#### Tabuleiro Inicial
<img src="https://github.com/XxJoaoQueirozxX/xadrez/blob/master/print/inicial.png?raw=true" width="400">

### Movimentos Possíveis
<img src="https://github.com/XxJoaoQueirozxX/xadrez/blob/master/print/source.png?raw=true" width="400">

### Captura de Peças
<img src="https://github.com/XxJoaoQueirozxX/xadrez/blob/master/print/captura.png?raw=true" width="400">

## 🛠️ Como Executar

O projeto possui dois pontos de entrada principais localizados no pacote `applications`:

### 1. Interface Gráfica (Recomendado)
Execute a classe `SwingUI.java`. Esta versão oferece a experiência completa com suporte a IA e integração com Stockfish.
```bash
java applications.SwingUI
```

### 2. Interface Console
Execute a classe `Teste.java`. Esta versão é ideal para jogar diretamente no terminal.
```bash
java applications.Teste
```

---
Desenvolvido por João Queiroz.
