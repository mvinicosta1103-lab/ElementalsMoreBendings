package com.elementals.morebendings.bending.firesubbendings.combustion;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.fire.FireElement;
import net.minecraft.server.level.ServerPlayer;

/**
 * Combustion Bending — sub-bending de Fire, mesmo padrão de {@link
 * com.elementals.morebendings.bending.firesubbendings.plasma.PlasmaElement}:
 * Element de verdade, registrada no mod base, gated atrás da masterização
 * de Fire inteira.
 *
 * Conceito (fiel ao lore de Combustion Man / P'Li): o bender concentra
 * calor num único ponto de foco (canonicamente, o "terceiro olho" na
 * testa) e só libera a explosão depois de mirar com cuidado. Uma mira
 * apressada ou uma concentração longa demais sem soltar é perigosa PRA
 * QUEM ESTÁ CANALIZANDO — daí o autodano em {@link CombustionExplosionAbility}.
 *
 * Árvore (mesma estrutura de Mud: dois filhos diretos gratuitos da raiz
 * sintética, cada um sendo já a própria ability — não precisa do hack de
 * autoUnlockRoot pro nó que não tem filhos, só pro que tem):
 *
 * combustionExplosion (grátis, é a habilidade principal -- TEM filhos,
 * então precisa de {@link #autoUnlockRoot} quando concedida por comando)
 *  ├─ combustionChargeI ─ combustionChargeII   (reduz o tempo mínimo de
 *  │                                            mira e o cooldown)
 *  ├─ combustionPowerI ─ combustionPowerII     (aumenta dano/raio da
 *  │                                            explosão)
 *  └─ combustionGuidance                        (capstone -- troca o tiro
 *                                                instantâneo por um
 *                                                projétil guiado/teleguiado,
 *                                                estilo P'Li / Combustion Man)
 * combustionVent (grátis, habilidade secundária MENOR -- folha, sem
 *                 filhos, sem risco, não precisa de unlock manual)
 */
public class CombustionElement extends Element {

    public static final String NAME = "Combustion";

    // ---- nomes dos nós (chave de save / lang / canUseUpgrade) ----
    public static final String COMBUSTION_EXPLOSION = "combustionExplosion";
    public static final String COMBUSTION_CHARGE_I = "combustionChargeI";
    public static final String COMBUSTION_CHARGE_II = "combustionChargeII";
    public static final String COMBUSTION_POWER_I = "combustionPowerI";
    public static final String COMBUSTION_POWER_II = "combustionPowerII";
    public static final String COMBUSTION_GUIDANCE = "combustionGuidance";
    public static final String COMBUSTION_VENT = "combustionVent";

    public CombustionElement() {
        super(NAME, new Upgrade[]{
                new Upgrade(COMBUSTION_EXPLOSION, new Upgrade[]{
                        new Upgrade(COMBUSTION_CHARGE_I, new Upgrade[]{
                                new Upgrade(COMBUSTION_CHARGE_II, 1)
                        }, 1),
                        new Upgrade(COMBUSTION_POWER_I, new Upgrade[]{
                                new Upgrade(COMBUSTION_POWER_II, 1)
                        }, 1),
                        new Upgrade(COMBUSTION_GUIDANCE, 2)
                }, 0),
                new Upgrade(COMBUSTION_VENT, 0)
        });
        addAbility(new CombustionExplosionAbility(), 0);
        addAbility(new CombustionVentAbility(), 1);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new CombustionElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    /**
     * @return true se o jogador já tem Fire E já comprou todos os nós da
     * árvore de skills de Fire (masterizou o elemento base) — mesma regra
     * do Plasma.
     */
    public static boolean canAcquire(Bender bender) {
        Element fire = FireElement.get();
        return bender.hasElement(fire) && fire.isSkillTreeComplete(bender);
    }

    public static boolean isCombustionBender(Bender bender) {
        return bender.hasElement(get());
    }

    public static boolean isCombustionBender(ServerPlayer player) {
        Bender bender = Bender.getBender(player);
        return bender != null && isCombustionBender(bender);
    }

    /** Atalho pras abilities: o nó precisa ter sido comprado pelo jogador. */
    public static boolean hasUpgrade(ServerPlayer player, String upgradeName) {
        Bender bender = Bender.getBender(player);
        return bender != null && bender.getData().canUseUpgrade(upgradeName);
    }

    /**
     * Marca o nó raiz "combustionExplosion" (preço 0) como já comprado.
     * Chame logo depois de {@code bender.addElement(CombustionElement.get(), true)}
     * no momento da concessão (ver MoreBendingCommand) -- mesmo motivo do
     * {@code PlasmaElement#autoUnlockRoot}: como esse nó TEM filhos
     * (charge/power/guidance), sem esse desbloqueio manual eles ficam
     * inacessíveis mesmo com level de sobra. "combustionVent" não precisa
     * disso porque é uma folha (sem filhos) -- mesmo caso de mudSurge/mudTrap.
     */
    public static void autoUnlockRoot(Bender bender) {
        Upgrade explosionNode = get().root.children[0]; // combustionExplosion
        bender.getData().upgrades.put(explosionNode, true);
    }

    /**
     * "Masterizado" = crescimento todo comprado (charge + power no máximo)
     * E o capstone de guidance levado, além das duas habilidades raiz
     * terem sido "compradas" (preço 0, mas ainda passam pelo fluxo normal
     * de clique quando adquiridas organicamente em jogo).
     */
    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        if (!bender.hasElement(this)) {
            return false;
        }
        return bender.getData().canUseUpgrade(COMBUSTION_EXPLOSION)
                && bender.getData().canUseUpgrade(COMBUSTION_VENT)
                && bender.getData().canUseUpgrade(COMBUSTION_CHARGE_II)
                && bender.getData().canUseUpgrade(COMBUSTION_POWER_II)
                && bender.getData().canUseUpgrade(COMBUSTION_GUIDANCE);
    }
}