package com.elementals.morebendings.bending.earthsubbendings.mud;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.earth.EarthElement;

/**
 * Mud Bending — sub-bending de Earth, registrada como um {@link Element}
 * de verdade no mod base (não um sistema paralelo). Isso é o que faz ela
 * aparecer no ciclo de elementos (tecla de trocar bending) e usar a mesma
 * árvore de skills / sistema de bind que Air, Water, Earth, Fire, etc.
 *
 * REGRA DE AQUISIÇÃO: só pode ser concedida a quem já tem Earth E já
 * masterizou a árvore de Earth inteira (ver {@link #canAcquire}). Isso é
 * checado no momento da concessão (ver MoreBendingCommand) — o Element em
 * si não impede o jogador de "ter" o elemento por fora, só documentamos a
 * regra aqui pra ficar num lugar só.
 *
 * Exatamente 4 filhos diretos na raiz -- é o máximo que
 * {@code UpgradeTreeScreen#render()} desenha (ver o comentário detalhado
 * em {@code LavaElement} sobre essa limitação do mod base). mudWall e
 * mudShell entraram DEPOIS que os 4 slots já estavam ocupados por
 * mudSurge/mudTrap/mudBall/mudSpikes -- por isso, igual LavaElement, elas
 * entram como {@code children} aninhados de dois dos ramos existentes em
 * vez de um 5º/6º Upgrade solto na raiz.
 */
public class MudElement extends Element {

    public static final String NAME = "Mud";

    public static final String MUD_SURGE = "mudSurge";
    public static final String MUD_TRAP = "mudTrap";
    public static final String MUD_BALL = "mudBall";
    public static final String MUD_SPIKES = "mudSpikes";
    public static final String MUD_WALL = "mudWall";
    public static final String MUD_SHELL = "mudShell";

    public MudElement() {
        super(NAME, new Upgrade[]{
                new Upgrade(MUD_SURGE, 0),  // grátis
                new Upgrade(MUD_TRAP, new Upgrade[]{
                        new Upgrade(MUD_WALL, 0)   // ver MudWallAbility
                }, 0),
                new Upgrade(MUD_BALL, 0),   // grátis -- ver MudBallAbility
                new Upgrade(MUD_SPIKES, new Upgrade[]{
                        new Upgrade(MUD_SHELL, 0)  // ver MudShellAbility
                }, 0)
        });

        addAbility(new MudSurgeAbility(), 0);
        addAbility(new MudTrapAbility(), 1);
        addAbility(new MudBallAbility(), 2);
        addAbility(new MudSpikesAbility(), 3);
        addAbility(new MudWallAbility(), 4);
        addAbility(new MudShellAbility(), 5);

        // Sem isso, Element#getKeybindSlotForUpgrade() sobe a árvore, não
        // acha nada em upgradeKeybinds e cai pro índice do RAMO da raiz
        // (0-3) em vez do índice real da ability (0-5) -- mudWall e
        // mudShell (aninhados) mostrariam a mesma tecla de mudTrap/
        // mudSpikes na tooltip. Registrando explicitamente cada upgrade
        // -> índice real da ability, exatamente como LavaElement faz.
        registerUpgradeKeybind(MUD_SURGE, 0);
        registerUpgradeKeybind(MUD_TRAP, 1);
        registerUpgradeKeybind(MUD_BALL, 2);
        registerUpgradeKeybind(MUD_SPIKES, 3);
        registerUpgradeKeybind(MUD_WALL, 4);
        registerUpgradeKeybind(MUD_SHELL, 5);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new MudElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    /**
     * @return true se o jogador já tem Earth E já comprou todos os nós da
     * árvore de skills de Earth (masterizou o elemento base).
     */
    public static boolean canAcquire(Bender bender) {
        Element earth = EarthElement.get();
        return bender.hasElement(earth) && earth.isSkillTreeComplete(bender);
    }

    public static boolean isMudBender(Bender bender) {
        return bender.hasElement(get());
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this)
                && bender.getData().canUseUpgrade(MUD_SURGE)
                && bender.getData().canUseUpgrade(MUD_TRAP)
                && bender.getData().canUseUpgrade(MUD_BALL)
                && bender.getData().canUseUpgrade(MUD_SPIKES)
                && bender.getData().canUseUpgrade(MUD_WALL)
                && bender.getData().canUseUpgrade(MUD_SHELL);
    }
}