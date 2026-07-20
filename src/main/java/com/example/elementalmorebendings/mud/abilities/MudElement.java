package com.example.elementalmorebendings.mud.abilities;

import com.example.elementalmorebendings.mud.abilities.MudWallAbility;
import com.example.elementalmorebendings.mud.abilities.QuicksandAbility;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.data.PlayerData;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;

/**
 * MudElement ("Mud")
 * <p>
 * Diferente de Lava e Plant — que o addon apenas ESTENDE porque o jar base
 * (Elementals Subbending) já cria esses dois Elements em
 * {@code ElementalsSubbending.onInitialize()} — o elemento "Mud" NÃO existe
 * em nenhum lugar do jar base. Por isso ele não pode ser injetado via
 * attachToBranch() num root que não existe; ele precisa ser uma subclasse de
 * Element nova, criada e registrada pelo próprio addon, exatamente como
 * LavaElement e PlantElement fazem — só que o "new MudElement()" é chamado
 * por ElementalMoreBendingsMod em vez de ElementalsSubbending.
 * <p>
 * Fusão base: Earth + Water (lama = terra + água), no mesmo espírito de
 * Lava (Earth + Fire) e Plant (Water).
 * <p>
 * IMPORTANTE — exatamente 4 ramos na raiz: a UpgradeTreeScreen do jar base
 * só sabe desenhar até 4 filhos saindo da raiz (mesmo limite documentado em
 * ElementalMoreBendingsMod, onde as habilidades extras de Lava/Plant são
 * anexadas DENTRO de ramos existentes em vez de na raiz). A árvore antiga
 * de Mud tinha 5 ramos na raiz (mudWall, quicksand, mudSlide, mudBall e
 * mudMastery), o que também contribuía pra árvore não desenhar direito.
 * Por isso "mudMastery" agora é um nó-folha DENTRO do ramo "mudBall" (o
 * capstone continua exigindo os upgrades dos 4 ramos via
 * isSkillTreeComplete, só que sem ocupar um 5º slot na raiz).
 */
public class MudElement extends Element {

    public MudElement() {
        super("Mud", new Upgrade[]{
                // Ramo 1 — Defesa: parede de lama que sobe do chão, mais a
                // habilidade bônus "mudShell" (casca de lama endurecida,
                // Resistência a dano) anexada como filho extra do mesmo ramo
                new Upgrade("mudWall", new Upgrade[]{
                        new Upgrade("mudWallHeightI", new Upgrade[]{
                                new Upgrade("mudWallHeightII", 2)
                        }, 1),
                        new Upgrade("mudWallWidthI", 1),
                        new Upgrade("mudShell", new Upgrade[]{
                                new Upgrade("mudShellDurationI", 1),
                                new Upgrade("mudShellHardenedI", 2)
                        }, 2)
                }, 2),

                // Ramo 2 — Controle: poça de areia movediça que afunda e prende
                new Upgrade("quicksand", new Upgrade[]{
                        new Upgrade("quicksandRadiusI", new Upgrade[]{
                                new Upgrade("quicksandRadiusII", 2)
                        }, 1),
                        new Upgrade("quicksandGripI", 2)
                }, 3),

                // Ramo 3 — Mobilidade: onda de lama que arrasta o jogador e
                // empurra quem estiver no caminho, mais a habilidade bônus
                // "mudSurge" (puxão rápido pra frente/cima)
                new Upgrade("mudSlide", new Upgrade[]{
                        new Upgrade("mudSlideDistanceI", new Upgrade[]{
                                new Upgrade("mudSlideDistanceII", 2)
                        }, 1),
                        new Upgrade("mudSlideSpeedI", 1),
                        new Upgrade("mudSurge", new Upgrade[]{
                                new Upgrade("mudSurgePowerI", 1)
                        }, 2)
                }, 2),

                // Ramo 4 — Ataque: bola de lama endurecida arremessada à
                // distância (modelo igual ao Air Ball, com textura de mud) +
                // capstone mudMastery aninhado aqui pra não criar um 5º ramo
                new Upgrade("mudBall", new Upgrade[]{
                        new Upgrade("mudBallPowerI", new Upgrade[]{
                                new Upgrade("mudBallPowerII", 2)
                        }, 1),
                        new Upgrade("mudBallBlindnessI", new Upgrade[]{
                                new Upgrade("mudMastery", 4)
                        }, 1)
                }, 3)
        });

        this.addAbility(new MudWallAbility(), true);
        this.addAbility(new QuicksandAbility(), true);
        this.addAbility(new MudSlideAbility(), true);
        this.addAbility(new MudBallAbility(), true);

        // Bônus (slots 4 e 5) — mesmo padrão de índice fixo usado em
        // ElementalMoreBendingsMod para as habilidades extras de Lava/Plant
        this.addAbility(new MudShellAbility(), 4);
        this.addAbility(new MudSurgeAbility(), 5);
        this.registerUpgradeKeybind("mudShell", 4);
        this.registerUpgradeKeybind("mudSurge", 5);
    }

    public static Element get() {
        return MudElement.getElement("Mud");
    }

    public boolean isSkillTreeComplete(Bender bender) {
        PlayerData data = bender.plrData;
        Element earth = Element.getElement("Earth");
        Element water = Element.getElement("Water");
        return earth != null && water != null
                && bender.hasElement(earth)
                && bender.hasElement(water)
                && data.canUseUpgrade("mudWallHeightII")
                && data.canUseUpgrade("mudWallWidthI")
                && data.canUseUpgrade("quicksandRadiusII")
                && data.canUseUpgrade("quicksandGripI")
                && data.canUseUpgrade("mudSlideDistanceII")
                && data.canUseUpgrade("mudSlideSpeedI")
                && data.canUseUpgrade("mudBallPowerII")
                && data.canUseUpgrade("mudBallBlindnessI")
                && data.canUseUpgrade("mudMastery");
    }

    // Tons de marrom-lama (primário, secundário, terciário)
    public int getColor() {
        return 7294519;   // #6F4E37
    }

    public int getSecondaryColor() {
        return 9132587;   // #8B5A2B
    }

    public int getTertiaryColor() {
        return 4862498;   // #4A3222
    }
}