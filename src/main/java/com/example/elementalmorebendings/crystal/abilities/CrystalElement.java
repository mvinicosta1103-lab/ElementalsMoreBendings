package com.example.elementalmorebendings.crystal.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.data.PlayerData;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;

/**
 * CrystalElement ("Crystal")
 * <p>
 * Assim como {@code MudElement}, o elemento "Crystal" NÃO existe em nenhum
 * lugar do jar base (Elementals Subbending) — ele precisa ser uma
 * subclasse de Element nova, criada e registrada pelo próprio addon. As
 * habilidades (CrystalWall, CrystalArmor, CrystalPrison, CrystalDash,
 * CrystalStep, CrystalSpike) já existiam em {@code crystal.abilities}; esta
 * classe é o que faltava para uni-las numa árvore de upgrades de verdade e
 * permitir que "Crystal" seja concedido via bender.addElement(...).
 * <p>
 * Fusão base: Earth puro (cristalização de ametista a partir de
 * terra/pedra — ver {@code AbilitySupport.canDamageBlocks} e o uso de
 * {@code EarthElement.isBlockBendable} em CrystalWall/CrystalPrison/
 * CrystalSpike), no mesmo espírito de Sand/Metal serem subbendings de um
 * elemento só, em vez de uma fusão de dois como Mud (Earth + Water) ou
 * Lava (Earth + Fire).
 * <p>
 * IMPORTANTE — exatamente 4 ramos na raiz: mesmo limite documentado em
 * MudElement/ElementalMoreBendingsMod (a UpgradeTreeScreen do jar base só
 * desenha até 4 filhos saindo da raiz). Por isso "crystalMastery" (o
 * capstone) fica aninhado dentro do ramo 4 (Ataque), como filho de
 * "crystalSpikeRadiusI", em vez de ocupar um 5º slot na raiz.
 */
public class CrystalElement extends Element {

    public CrystalElement() {
        super("Crystal", new Upgrade[]{
                // Ramo 1 — Defesa: parede de ametista que sobe do chão, mais
                // a habilidade bônus "crystalArmor" (couraça instantânea de
                // Resistência + Absorção) anexada como filho extra do mesmo
                // ramo.
                new Upgrade("crystalWall", new Upgrade[]{
                        new Upgrade("crystalWallHeightI", new Upgrade[]{
                                new Upgrade("crystalWallHeightII", 2)
                        }, 1),
                        new Upgrade("crystalWallWidthI", 1),
                        new Upgrade("crystalArmor", new Upgrade[]{
                                new Upgrade("crystalArmorDurationI", 1),
                                new Upgrade("crystalArmorHardenedI", 2)
                        }, 2)
                }, 2),

                // Ramo 2 — Controle: gaiola de ametista que prende um alvo
                // único por uma duração fixa.
                new Upgrade("crystalPrison", new Upgrade[]{
                        new Upgrade("crystalPrisonRadiusI", new Upgrade[]{
                                new Upgrade("crystalPrisonRadiusII", 2)
                        }, 1),
                        new Upgrade("crystalPrisonGripI", 2)
                }, 3),

                // Ramo 3 — Mobilidade: investida de cristal pra frente, mais
                // a habilidade bônus "crystalStep" (blink curto/teleporte)
                // anexada como filho extra do mesmo ramo.
                new Upgrade("crystalDash", new Upgrade[]{
                        new Upgrade("crystalDashDistanceI", new Upgrade[]{
                                new Upgrade("crystalDashDistanceII", 2)
                        }, 1),
                        new Upgrade("crystalDashSpeedI", 1),
                        new Upgrade("crystalStep", new Upgrade[]{
                                new Upgrade("crystalStepRangeI", 1)
                        }, 2)
                }, 2),

                // Ramo 4 — Ataque: cacho de espinhos de ametista à distância
                // + capstone crystalMastery aninhado aqui pra não criar um
                // 5º ramo.
                new Upgrade("crystalSpike", new Upgrade[]{
                        new Upgrade("crystalSpikePowerI", new Upgrade[]{
                                new Upgrade("crystalSpikePowerII", 2)
                        }, 1),
                        new Upgrade("crystalSpikeRadiusI", new Upgrade[]{
                                new Upgrade("crystalMastery", 4)
                        }, 1)
                }, 3)
        });

        this.addAbility(new CrystalWallAbility(), true);
        this.addAbility(new CrystalPrisonAbility(), true);
        this.addAbility(new CrystalDashAbility(), true);
        this.addAbility(new CrystalSpikeAbility(), true);

        // Bônus (slots 4 e 5) — mesmo padrão de índice fixo usado em
        // MudElement/ElementalMoreBendingsMod para as habilidades extras.
        this.addAbility(new CrystalArmorAbility(), 4);
        this.addAbility(new CrystalStepAbility(), 5);
        this.registerUpgradeKeybind("crystalArmor", 4);
        this.registerUpgradeKeybind("crystalStep", 5);
    }

    public static Element get() {
        return CrystalElement.getElement("Crystal");
    }

    public boolean isSkillTreeComplete(Bender bender) {
        PlayerData data = bender.plrData;
        Element earth = Element.getElement("Earth");
        return earth != null
                && bender.hasElement(earth)
                && data.canUseUpgrade("crystalWallHeightII")
                && data.canUseUpgrade("crystalWallWidthI")
                && data.canUseUpgrade("crystalPrisonRadiusII")
                && data.canUseUpgrade("crystalPrisonGripI")
                && data.canUseUpgrade("crystalDashDistanceII")
                && data.canUseUpgrade("crystalDashSpeedI")
                && data.canUseUpgrade("crystalSpikePowerII")
                && data.canUseUpgrade("crystalSpikeRadiusI")
                && data.canUseUpgrade("crystalMastery");
    }

    // Tons de ametista (primário, secundário, terciário)
    public int getColor() {
        return 10053324; // #9966CC
    }

    public int getSecondaryColor() {
        return 6950317;  // #6A0DAD
    }

    public int getTertiaryColor() {
        return 4915330;  // #4B0082
    }
}