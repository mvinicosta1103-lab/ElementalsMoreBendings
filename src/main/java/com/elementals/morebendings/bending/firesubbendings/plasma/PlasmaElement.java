package com.elementals.morebendings.bending.firesubbendings.plasma;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.fire.FireElement;
import net.minecraft.server.level.ServerPlayer;

/**
 * Plasma Bending — sub-bending de Fire, mesmo padrão de {@link
 * com.elementals.morebendings.bending.airsubbendings.gas.GasElement}:
 * Element de verdade, registrada no mod base, gated atrás da
 * masterização de Fire. Só benders de Fire (com a árvore de Fire
 * inteira masterizada) podem obter.
 *
 * Conceito: em vez de projéteis/áreas à distância como o resto do Fire,
 * Plasma é uma sub-bending de curto alcance -- o bender super-aquece o
 * ar ao redor das próprias mãos até virar plasma e rasga o que estiver
 * na frente dele com as "garras" resultantes.
 *
 * Árvore (mesma estrutura de Gas/Mist -- "plasmaClaws" é o único filho
 * direto da raiz sintética, então precisa do mesmo hack de
 * autoUnlockRoot):
 *
 * plasmaClaws (grátis, é a habilidade em si)
 *  ├─ plasmaReachI ─ plasmaReachII        (alcance)
 *  ├─ plasmaHeatI ─ plasmaHeatII          (cooldown)
 *  └─ plasmaSpecialization (exclusive=true)
 *      ├─ plasmaSear ─ plasmaSearDamageI ─ plasmaSearDamageII
 *      ├─ plasmaFlare ─ plasmaFlareDurationI
 *      └─ plasmaSuperheat
 */
public class PlasmaElement extends Element {

    public static final String NAME = "Plasma";

    // ---- nomes dos nós (chave de save / lang / canUseUpgrade) ----
    public static final String PLASMA_CLAWS = "plasmaClaws";
    public static final String PLASMA_REACH_I = "plasmaReachI";
    public static final String PLASMA_REACH_II = "plasmaReachII";
    public static final String PLASMA_HEAT_I = "plasmaHeatI";
    public static final String PLASMA_HEAT_II = "plasmaHeatII";
    public static final String PLASMA_SPECIALIZATION = "plasmaSpecialization";
    public static final String PLASMA_SEAR = "plasmaSear";
    public static final String PLASMA_SEAR_DAMAGE_I = "plasmaSearDamageI";
    public static final String PLASMA_SEAR_DAMAGE_II = "plasmaSearDamageII";
    public static final String PLASMA_FLARE = "plasmaFlare";
    public static final String PLASMA_FLARE_DURATION_I = "plasmaFlareDurationI";
    public static final String PLASMA_SUPERHEAT = "plasmaSuperheat";

    public PlasmaElement() {
        super(NAME, new Upgrade[]{
                new Upgrade(PLASMA_CLAWS, new Upgrade[]{
                        new Upgrade(PLASMA_REACH_I, new Upgrade[]{
                                new Upgrade(PLASMA_REACH_II, 1)
                        }, 1),
                        new Upgrade(PLASMA_HEAT_I, new Upgrade[]{
                                new Upgrade(PLASMA_HEAT_II, 1)
                        }, 1),
                        new Upgrade(PLASMA_SPECIALIZATION, new Upgrade[]{
                                new Upgrade(PLASMA_SEAR, new Upgrade[]{
                                        new Upgrade(PLASMA_SEAR_DAMAGE_I, new Upgrade[]{
                                                new Upgrade(PLASMA_SEAR_DAMAGE_II, 1)
                                        }, 1)
                                }, 2),
                                new Upgrade(PLASMA_FLARE, new Upgrade[]{
                                        new Upgrade(PLASMA_FLARE_DURATION_I, 1)
                                }, 2),
                                new Upgrade(PLASMA_SUPERHEAT, 3)
                        }, true, 0)
                }, 0)
        });
        addAbility(new PlasmaClawsAbility(), 0);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new PlasmaElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    /**
     * @return true se o jogador já tem Fire E já comprou todos os nós da
     * árvore de skills de Fire (masterizou o elemento base) — mesma
     * regra de Gas/Mist em relação a Air.
     */
    public static boolean canAcquire(Bender bender) {
        Element fire = FireElement.get();
        return bender.hasElement(fire) && fire.isSkillTreeComplete(bender);
    }

    public static boolean isPlasmaBender(Bender bender) {
        return bender.hasElement(get());
    }

    /**
     * Marca o nó raiz "plasmaClaws" (preço 0) como já comprado. Chame
     * logo depois de {@code bender.addElement(PlasmaElement.get(), true)}
     * no momento da concessão (ver MoreBendingCommand) -- mesmo motivo do
     * {@code GasElement#autoUnlockRoot}: "plasmaClaws" é o único filho
     * direto da raiz sintética do Element, então sem esse desbloqueio
     * manual a árvore inteira fica travada mesmo com level de sobra.
     */
    public static void autoUnlockRoot(Bender bender) {
        Upgrade clawsNode = get().root.children[0]; // plasmaClaws
        bender.getData().upgrades.put(clawsNode, true);
    }

    /** Atalho pras abilities, que só têm o ServerPlayer em mãos. */
    public static boolean isPlasmaBender(ServerPlayer player) {
        Bender bender = Bender.getBender(player);
        return bender != null && isPlasmaBender(bender);
    }

    /** Atalho pras abilities: o nó precisa ter sido comprado pelo jogador. */
    public static boolean hasUpgrade(ServerPlayer player, String upgradeName) {
        Bender bender = Bender.getBender(player);
        return bender != null && bender.getData().canUseUpgrade(upgradeName);
    }

    /**
     * "Masterizado" = crescimento todo comprado (alcance + calor no
     * máximo) E uma das três especializações levada até o fim — não dá
     * pra exigir as três porque são mutuamente exclusivas. Mesmo
     * critério do Gas/Mist.
     */
    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        if (!bender.hasElement(this)) {
            return false;
        }
        boolean growthMaxed = bender.getData().canUseUpgrade(PLASMA_REACH_II)
                && bender.getData().canUseUpgrade(PLASMA_HEAT_II);
        boolean specializationMaxed = bender.getData().canUseUpgrade(PLASMA_SEAR_DAMAGE_II)
                || bender.getData().canUseUpgrade(PLASMA_FLARE_DURATION_I)
                || bender.getData().canUseUpgrade(PLASMA_SUPERHEAT);
        return growthMaxed && specializationMaxed;
    }
}