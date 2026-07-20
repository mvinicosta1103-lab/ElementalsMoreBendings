# Obsidian Wake — mod addon próprio (projeto NeoForge completo)

Este é um projeto de mod **de verdade**, no formato oficial do NeoForge
(baixei o template deles pra 1.21.1). Ele NÃO modifica o addon do Jsumpter
— ele roda do lado, e quando o jogo termina de carregar todos os mods, ele
"planta" a habilidade Obsidian Wake dentro da árvore de Lava que o addon
dele já criou.

## Passo 1 — Pré-requisitos

- **JDK 21** instalado (Minecraft 1.21.1 exige Java 21). Baixe em
  https://adoptium.net/pt-BR/temurin/releases/?version=21 se não tiver.
- IntelliJ IDEA (você já tem ✅)

## Passo 2 — Conseguir o `elementals-neoforge.jar`

Esse é o único jar que você precisa colocar na pasta `libs/` deste projeto
(pra compilar contra as classes `Bender`, `Element`, `Ability`, `Upgrade`).

Você já deve ter esse arquivo instalado — ele é pré-requisito do addon que
você já usa. Normalmente fica em:
`<sua instalação do Minecraft>/mods/elementals-neoforge-<versão>.jar`

Copie esse arquivo pra dentro de `obsidianwake/libs/`.

(Se não achar, dá pra baixar de novo em
https://www.curseforge.com/minecraft/mc-mods/saps-elementals — pegue a
versão NeoForge compatível com 1.21.1.)

## Passo 3 — Abrir no IntelliJ

1. Extraia o zip que te mandei numa pasta, ex: `C:\dev\obsidianwake` (ou
   `~/dev/obsidianwake` no Mac/Linux).
2. Coloque o `elementals-neoforge.jar` dentro de `obsidianwake/libs/`
   (passo 2).
3. Abra o IntelliJ → **File → Open** → selecione a pasta `obsidianwake`.
4. O IntelliJ vai detectar que é um projeto Gradle e perguntar se quer
   importar — clique em **Trust Project** / **Load Gradle Project**.
5. Espere o Gradle baixar tudo (primeira vez demora, baixa o NeoForge
   inteiro — é normal levar alguns minutos).

## Passo 4 — Testar

No IntelliJ, na aba **Gradle** (lateral direita) → `obsidianwake` → `Tasks`
→ `neoforge` → dê duplo clique em **runClient**.

Isso abre uma instância de teste do Minecraft já com seu mod carregado.
Só que ela não terá o addon do Jsumpter nem o `elementals` instalados de
verdade — pra testar com eles juntos, você tem duas opções:

- **Mais simples:** compile (`Passo 5`) e jogue seu jar junto com os outros
  na sua instalação normal do Minecraft.
- **Testar direto pelo Gradle:** copie `elementals-neoforge.jar` e o
  `ElementalsSubbending-NEOFORGE-1_21_1-INSTALL.jar` original pra dentro de
  `obsidianwake/run/mods/` (essa pasta `run/` só aparece depois da primeira
  vez que você roda `runClient`).

## Passo 5 — Compilar o `.jar` final

No terminal, dentro da pasta do projeto:

```
./gradlew build
```

(No Windows, use `gradlew.bat build` no PowerShell/CMD, ou rode a task
`build` pela aba Gradle do IntelliJ.)

O jar final sai em `build/libs/obsidianwake-1.0.0.jar`. Copie ele pra sua
pasta `mods/` de verdade, junto com `elementals-neoforge.jar` e o addon do
Jsumpter — os três juntos.

## O que o mod faz

Quando todos os mods terminam de carregar, `ObsidianWakeMod` procura o
elemento `"Lava"` (criado pelo addon do Jsumpter). Se achar, ele:

1. Cria o nó `obsidianForm` (com os sub-upgrades `obsidianFormRadiusI/II` e
   `obsidianFormSpeedI`) e o encaixa como filho da raiz da árvore de Lava.
2. Registra `ObsidianFormAbility` como habilidade vinculável do Lava.

Se o addon do Jsumpter não estiver instalado, o mod avisa no log e não faz
nada — não quebra o jogo.

## Habilidades incluídas agora

- **Obsidian Wake** (`obsidianForm`) — seca lava próxima e endurece em obsidiana.
- **Eruption** (`eruption`) — golpe de área, queima e empurra tudo perto do impacto.
- **Lava Surf** (`lavaSurf`) — toggle: imunidade a fogo/lava + velocidade enquanto surfa em lava.
- **Molten Grip** (`moltenGrip`) — agarra e puxa o inimigo mais próximo à frente.
- **Obsidian Pillar** (`obsidianPillar`) — junta obsidiana próxima e a lança pra cima em forma de espinho. É a habilidade que dá "controle" de obsidiana pro Lava bender, do mesmo jeito que Terra controla pedra — só que só funciona com obsidiana, e só pra quem tem Lava desbloqueado.

## Sobre o problema do keybind

O Elementals só tem **4 teclas de habilidade no total por elemento**
(bind1 a bind4) — não é "uma tecla por habilidade". O addon do Jsumpter já
usa as 4 (`lavaFlow`, `lavaSpike`, `magmaArmor`, `lavaShuriken`). Isso
significa que **nenhuma habilidade nova, de nenhum addon, ganha tecla
própria automaticamente** — nem as suas, nem se o Jsumpter mesmo
adicionasse uma quinta habilidade no mod dele.

Pra usar qualquer uma das 5 habilidades novas, você precisa abrir o mesmo
menu que já usou pra vincular as 4 originais (a tela da árvore de skills,
normalmente clicando no ícone da habilidade) e trocar um dos 4 slots pra
uma das novas. Isso não é um bug — é assim que o mod base funciona.

## Pendências

- **Ícone**: por enquanto reaproveita o ícone do `lavaFlow`
  (`upgrade.elementals.obsidianForm.icon` = `lava_flow`). Pra ter um ícone
  próprio, crie `assets/elementals/textures/gui/symbol/obsidian_form.png`
  neste projeto (mesmo tamanho dos outros ícones) e troque o valor no
  `lang/en_us.json`.
- **Ordem de carregamento**: a extensão da árvore acontece no
  `FMLLoadCompleteEvent`, que roda depois que TODOS os mods (incluindo o
  addon do Jsumpter) já terminaram o próprio setup — isso deveria ser
  seguro em qualquer ordem de carregamento dos mods.
