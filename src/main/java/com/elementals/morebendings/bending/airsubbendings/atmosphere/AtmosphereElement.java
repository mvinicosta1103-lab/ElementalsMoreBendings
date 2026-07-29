package com.elementals.morebendings.bending.airsubbendings.atmosphere;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.air.AirElement;
import net.minecraft.server.level.ServerPlayer;

/**
 * Atmosphere Bending — sub-bending de Air, mesmo padrão de {@link
 * com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalElement}:
 * Element de verdade, registrada no mod base, gated atrás da masterização
 * de Air. Só benders de Air podem obter.
 *
 * Quatro habilidades raiz, todas grátis (preço 0) e todas filhas diretas
 * da raiz sintética -- igual Sound/Temperature/Void, sem especialização
 * exclusiva (diferente de Gas/Mist). Cada uma tem seu próprio ramo linear
 * de níveis (raio/duração/dano/cooldown), lido pelas abilities via
 * {@link #hasUpgrade}:
 *
 * atmospherePressurePoint (campo de pressão em área -- ver {@link PressurePointAbility})
 *  ├─ pressureRadiusI ─ pressureRadiusII      (raio da zona)
 *  └─ pressureDurationI ─ pressureDurationII  (duração da zona)
 * atmosphericDome (cúpula defensiva -- ver {@link AtmosphericDomeAbility})
 *  ├─ domeRadiusI ─ domeRadiusII          (raio da cúpula)
 *  └─ domeEfficiencyI ─ domeEfficiencyII  (reduz o custo de chi/tick)
 * pressureSlam (golpe de pressão concentrada num alvo único -- ver {@link PressureSlamAbility})
 *  ├─ pressureSlamDamageI ─ pressureSlamDamageII
 *  └─ pressureSlamCooldownI
 * updraft (coluna de baixa pressão que ergue o caster -- ver {@link UpdraftAbility})
 *  ├─ updraftHeightI ─ updraftHeightII
 *  └─ updraftCooldownI
 *
 * IMPORTANTE -- cada um dos 4 nós acima TEM FILHOS, então cada um precisa
 * estar marcado como "comprado" antes que {@code canBuyUpgrade} desça pra
 * dentro dos seus próprios ramos (mesmo motivo de {@code
 * GasElement#autoUnlockRoot}, só que aqui são 4 nós em vez de 1 -- ver
 * {@link #autoUnlockRoots}).
 */
public class AtmosphereElement extends Element {

    public static final String NAME = "Atmosphere";

    // ---- pressurePoint ----
    public static final String PRESSURE_POINT = "atmospherePressurePoint";
    public static final String PRESSURE_RADIUS_I = "pressureRadiusI";
    public static final String PRESSURE_RADIUS_II = "pressureRadiusII";
    public static final String PRESSURE_DURATION_I = "pressureDurationI";
    public static final String PRESSURE_DURATION_II = "pressureDurationII";

    // ---- atmosphericDome ----
    public static final String ATMOSPHERIC_DOME = "atmosphericDome";
    public static final String DOME_RADIUS_I = "domeRadiusI";
    public static final String DOME_RADIUS_II = "domeRadiusII";
    public static final String DOME_EFFICIENCY_I = "domeEfficiencyI";
    public static final String DOME_EFFICIENCY_II = "domeEfficiencyII";

    // ---- pressureSlam (nova) ----
    public static final String PRESSURE_SLAM = "pressureSlam";
    public static final String PRESSURE_SLAM_DAMAGE_I = "pressureSlamDamageI";
    public static final String PRESSURE_SLAM_DAMAGE_II = "pressureSlamDamageII";
    public static final String PRESSURE_SLAM_COOLDOWN_I = "pressureSlamCooldownI";

    // ---- updraft (nova) ----
    public static final String UPDRAFT = "updraft";
    public static final String UPDRAFT_HEIGHT_I = "updraftHeightI";
    public static final String UPDRAFT_HEIGHT_II = "updraftHeightII";
    public static final String UPDRAFT_COOLDOWN_I = "updraftCooldownI";

    public AtmosphereElement() {
        super(NAME, new Upgrade[]{
                new Upgrade(PRESSURE_POINT, new Upgrade[]{
                        new Upgrade(PRESSURE_RADIUS_I, new Upgrade[]{
                                new Upgrade(PRESSURE_RADIUS_II, 1)
                        }, 1),
                        new Upgrade(PRESSURE_DURATION_I, new Upgrade[]{
                                new Upgrade(PRESSURE_DURATION_II, 1)
                        }, 1)
                }, 0),
                new Upgrade(ATMOSPHERIC_DOME, new Upgrade[]{
                        new Upgrade(DOME_RADIUS_I, new Upgrade[]{
                                new Upgrade(DOME_RADIUS_II, 1)
                        }, 1),
                        new Upgrade(DOME_EFFICIENCY_I, new Upgrade[]{
                                new Upgrade(DOME_EFFICIENCY_II, 1)
                        }, 1)
                }, 0),
                new Upgrade(PRESSURE_SLAM, new Upgrade[]{
                        new Upgrade(PRESSURE_SLAM_DAMAGE_I, new Upgrade[]{
                                new Upgrade(PRESSURE_SLAM_DAMAGE_II, 1)
                        }, 1),
                        new Upgrade(PRESSURE_SLAM_COOLDOWN_I, 1)
                }, 0),
                new Upgrade(UPDRAFT, new Upgrade[]{
                        new Upgrade(UPDRAFT_HEIGHT_I, new Upgrade[]{
                                new Upgrade(UPDRAFT_HEIGHT_II, 1)
                        }, 1),
                        new Upgrade(UPDRAFT_COOLDOWN_I, 1)
                }, 0)
        });
        addAbility(new PressurePointAbility(), 0);
        addAbility(new AtmosphericDomeAbility(), 1);
        addAbility(new PressureSlamAbility(), 2);
        addAbility(new UpdraftAbility(), 3);
    }

    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new AtmosphereElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    public static boolean canAcquire(Bender bender) {
        Element air = AirElement.get();
        return bender.hasElement(air) && air.isSkillTreeComplete(bender);
    }

    public static boolean isAtmosphereBender(Bender bender) {
        return bender.hasElement(get());
    }

    /** Atalho pras abilities, que só têm o ServerPlayer em mãos. */
    public static boolean isAtmosphereBender(ServerPlayer player) {
        Bender bender = Bender.getBender(player);
        return bender != null && isAtmosphereBender(bender);
    }

    /** Atalho pras abilities: o nó precisa ter sido comprado pelo jogador. */
    public static boolean hasUpgrade(ServerPlayer player, String upgradeName) {
        Bender bender = Bender.getBender(player);
        return bender != null && bender.getData().canUseUpgrade(upgradeName);
    }

    /**
     * Marca os 4 nós raiz (pressurePoint, atmosphericDome, pressureSlam,
     * updraft) como já comprados. Chame logo depois de {@code
     * bender.addElement(AtmosphereElement.get(), true)} no momento da
     * concessão (ver MoreBendingCommand) -- mesmo motivo do {@code
     * GasElement#autoUnlockRoot}: como cada um desses 4 nós agora TEM
     * FILHOS (ramos de nível), sem esse desbloqueio manual cada ramo
     * fica travado mesmo com level de sobra, porque o próprio nó raiz
     * (preço 0, parece "decorativo") nunca é clicado de propósito.
     */
    public static void autoUnlockRoots(Bender bender) {
        Upgrade root = get().root;
        for (Upgrade child : root.children) {
            bender.getData().upgrades.put(child, true);
        }
    }

    /**
     * "Masterizado" = todo ramo de nível de todas as 4 habilidades no
     * máximo. Diferente de Gas/Mist não tem especialização exclusiva pra
     * escolher, então precisa exigir todos os ramos, não só um.
     */
    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        if (!bender.hasElement(this)) {
            return false;
        }
        return bender.getData().canUseUpgrade(PRESSURE_RADIUS_II)
                && bender.getData().canUseUpgrade(PRESSURE_DURATION_II)
                && bender.getData().canUseUpgrade(DOME_RADIUS_II)
                && bender.getData().canUseUpgrade(DOME_EFFICIENCY_II)
                && bender.getData().canUseUpgrade(PRESSURE_SLAM_DAMAGE_II)
                && bender.getData().canUseUpgrade(PRESSURE_SLAM_COOLDOWN_I)
                && bender.getData().canUseUpgrade(UPDRAFT_HEIGHT_II)
                && bender.getData().canUseUpgrade(UPDRAFT_COOLDOWN_I);
    }
}