package com.elementals.morebendings.bending.airsubbendings.gas;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.air.AirElement;
import net.minecraft.server.level.ServerPlayer;

/**
 * Gas Bending — sub-bending de Air, mesmo padrão de {@link
 * com.elementals.morebendings.bending.airsubbendings.atmosphere.AtmosphereElement}:
 * Element de verdade, registrada no mod base, gated atrás da masterização
 * de Air. Só benders de Air (com a árvore de Air inteira masterizada)
 * podem obter.
 *
 * Árvore (mesma estrutura de antes, só sem o "gasRoot" artificial — o
 * próprio array passado pro super() já cumpre esse papel):
 *
 * gasCloud (grátis, é a habilidade em si)
 *  ├─ gasCloudSizeI ─ gasCloudSizeII        (raio)
 *  ├─ gasVentI ─ gasVentII                  (cooldown)
 *  └─ gasSpecialization (exclusive=true)
 *      ├─ gasSuffocate ─ gasSuffocateDamageI ─ gasSuffocateDamageII
 *      ├─ gasLeak ─ gasLeakDurationI
 *      └─ gasIgnite
 */
public class GasElement extends Element {

    public static final String NAME = "Gas";

    // ---- nomes dos nós (chave de save / lang / canUseUpgrade) ----
    public static final String GAS_CLOUD = "gasCloud";
    public static final String GAS_CLOUD_SIZE_I = "gasCloudSizeI";
    public static final String GAS_CLOUD_SIZE_II = "gasCloudSizeII";
    public static final String GAS_VENT_I = "gasVentI";
    public static final String GAS_VENT_II = "gasVentII";
    public static final String GAS_SUFFOCATE = "gasSuffocate";
    public static final String GAS_SUFFOCATE_DAMAGE_I = "gasSuffocateDamageI";
    public static final String GAS_SUFFOCATE_DAMAGE_II = "gasSuffocateDamageII";
    public static final String GAS_LEAK = "gasLeak";
    public static final String GAS_LEAK_DURATION_I = "gasLeakDurationI";
    public static final String GAS_IGNITE = "gasIgnite";
    public static final String GAS_SPECIALIZATION = "gasSpecialization";

    public GasElement() {
        super(NAME, new Upgrade[]{
                new Upgrade(GAS_CLOUD, new Upgrade[]{
                        new Upgrade(GAS_CLOUD_SIZE_I, new Upgrade[]{
                                new Upgrade(GAS_CLOUD_SIZE_II, 1)
                        }, 1),
                        new Upgrade(GAS_VENT_I, new Upgrade[]{
                                new Upgrade(GAS_VENT_II, 1)
                        }, 1),
                        new Upgrade(GAS_SPECIALIZATION, new Upgrade[]{
                                new Upgrade(GAS_SUFFOCATE, new Upgrade[]{
                                        new Upgrade(GAS_SUFFOCATE_DAMAGE_I, new Upgrade[]{
                                                new Upgrade(GAS_SUFFOCATE_DAMAGE_II, 1)
                                        }, 1)
                                }, 2),
                                new Upgrade(GAS_LEAK, new Upgrade[]{
                                        new Upgrade(GAS_LEAK_DURATION_I, 1)
                                }, 2),
                                new Upgrade(GAS_IGNITE, 3)
                        }, true, 0)
                }, 0)
        });
        addAbility(new GasCloudAbility(), 0);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new GasElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    /**
     * @return true se o jogador já tem Air E já comprou todos os nós da
     * árvore de skills de Air (masterizou o elemento base) — mesma regra
     * de Mud/Crystal/Atmosphere.
     */
    public static boolean canAcquire(Bender bender) {
        Element air = AirElement.get();
        return bender.hasElement(air) && air.isSkillTreeComplete(bender);
    }

    public static boolean isGasBender(Bender bender) {
        return bender.hasElement(get());
    }

    /**
     * Marca o nó raiz "gasCloud" (preço 0) como já comprado. Chame logo
     * depois de {@code bender.addElement(GasElement.get(), true)} no
     * momento da concessão (ver MoreBendingCommand).
     *
     * Por quê isso é necessário: no jar base, {@code Upgrade.nextUpgrades}
     * só "desce" para dentro dos filhos de um nó depois que esse nó já
     * está marcado como comprado no mapa do jogador (upgrades HashMap).
     * Como {@code gasCloud} é o ÚNICO filho direto da raiz sintética do
     * Element (Element(String, Upgrade[]) embrulha o array que a gente
     * passa em cima de um Upgrade invisível), TODOS os outros nós da
     * árvore (gasCloudSizeI, gasVentI, gasSpecialization, ...) ficam
     * fora do alcance de {@code canBuyUpgrade} até alguém clicar
     * manualmente em gasCloud primeiro — e como o preço dele é 0, esse
     * clique é fácil de passar batido (o nó parece "só decorativo").
     * Sem esse clique inicial, a árvore inteira parece travada mesmo
     * com level de sobra: o clique em qualquer outro nó cai no branch
     * de "toggle" (que não faz nada pra upgrade nunca comprado) em vez
     * do branch de "buy".
     */
    public static void autoUnlockRoot(Bender bender) {
        Upgrade cloudNode = get().root.children[0]; // gasCloud
        bender.getData().upgrades.put(cloudNode, true);
    }

    /** Atalho pras abilities, que só têm o ServerPlayer em mãos. */
    public static boolean isGasBender(ServerPlayer player) {
        Bender bender = Bender.getBender(player);
        return bender != null && isGasBender(bender);
    }

    /** Atalho pras abilities: o nó precisa ter sido comprado pelo jogador. */
    public static boolean hasUpgrade(ServerPlayer player, String upgradeName) {
        Bender bender = Bender.getBender(player);
        return bender != null && bender.getData().canUseUpgrade(upgradeName);
    }

    /**
     * "Masterizado" = crescimento todo comprado (tamanho + vent no máximo)
     * E uma das três especializações levada até o fim — não dá pra exigir
     * as três porque são mutuamente exclusivas.
     */
    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        if (!bender.hasElement(this)) {
            return false;
        }
        boolean growthMaxed = bender.getData().canUseUpgrade(GAS_CLOUD_SIZE_II)
                && bender.getData().canUseUpgrade(GAS_VENT_II);
        boolean specializationMaxed = bender.getData().canUseUpgrade(GAS_SUFFOCATE_DAMAGE_II)
                || bender.getData().canUseUpgrade(GAS_LEAK_DURATION_I)
                || bender.getData().canUseUpgrade(GAS_IGNITE);
        return growthMaxed && specializationMaxed;
    }
}