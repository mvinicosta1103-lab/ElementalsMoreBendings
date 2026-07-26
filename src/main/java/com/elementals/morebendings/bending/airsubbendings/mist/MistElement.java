package com.elementals.morebendings.bending.airsubbendings.mist;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.air.AirElement;
import net.minecraft.server.level.ServerPlayer;

/**
 * Mist Bending — sub-bending de Air, mesmo padrão de {@link
 * com.elementals.morebendings.bending.airsubbendings.gas.GasElement}:
 * Element de verdade, registrada no mod base, gated atrás da masterização
 * de Air. Só benders de Air (com a árvore de Air inteira masterizada)
 * podem obter.
 *
 * Diferença de design em relação ao Gas: a habilidade raiz (Heavy Fog) não
 * é um burst instantâneo -- ela planta uma névoa que fica ativa por um
 * tempo, aplicando Cegueira + Escuridão em qualquer um que fique dentro
 * (o próprio caster NUNCA é afetado, mesma regra do Gas). Como Cegueira já
 * faz mobs perderem o alvo/reduzir alcance de detecção no vanilla, isso
 * cobre o pedido de "reduzir alcance de mira/detecção de mobs" sem
 * precisar mexer em IA de mob.
 *
 * Árvore (mesmo formato do Gas -- "mistCloud" é o único filho direto da
 * raiz sintética, então precisa do mesmo hack de autoUnlockRoot):
 *
 * mistCloud (grátis, é a habilidade em si -- Heavy Fog)
 *  ├─ mistCloudSizeI ─ mistCloudSizeII      (raio)
 *  ├─ mistVentI ─ mistVentII                (cooldown)
 *  └─ mistSpecialization (exclusive=true)
 *      ├─ mistChoke ─ mistChokeDamageI ─ mistChokeDamageII  (dano contínuo)
 *      ├─ mistVeil ─ mistVeilDurationI                      (névoa dura mais)
 *      └─ mistFreeze                                        (lentidão pesada)
 */
public class MistElement extends Element {

    public static final String NAME = "Mist";

    // ---- nomes dos nós (chave de save / lang / canUseUpgrade) ----
    public static final String MIST_CLOUD = "mistCloud";
    public static final String MIST_CLOUD_SIZE_I = "mistCloudSizeI";
    public static final String MIST_CLOUD_SIZE_II = "mistCloudSizeII";
    public static final String MIST_VENT_I = "mistVentI";
    public static final String MIST_VENT_II = "mistVentII";
    public static final String MIST_SPECIALIZATION = "mistSpecialization";
    public static final String MIST_CHOKE = "mistChoke";
    public static final String MIST_CHOKE_DAMAGE_I = "mistChokeDamageI";
    public static final String MIST_CHOKE_DAMAGE_II = "mistChokeDamageII";
    public static final String MIST_VEIL = "mistVeil";
    public static final String MIST_VEIL_DURATION_I = "mistVeilDurationI";
    public static final String MIST_FREEZE = "mistFreeze";

    public MistElement() {
        super(NAME, new Upgrade[]{
                new Upgrade(MIST_CLOUD, new Upgrade[]{
                        new Upgrade(MIST_CLOUD_SIZE_I, new Upgrade[]{
                                new Upgrade(MIST_CLOUD_SIZE_II, 1)
                        }, 1),
                        new Upgrade(MIST_VENT_I, new Upgrade[]{
                                new Upgrade(MIST_VENT_II, 1)
                        }, 1),
                        new Upgrade(MIST_SPECIALIZATION, new Upgrade[]{
                                new Upgrade(MIST_CHOKE, new Upgrade[]{
                                        new Upgrade(MIST_CHOKE_DAMAGE_I, new Upgrade[]{
                                                new Upgrade(MIST_CHOKE_DAMAGE_II, 1)
                                        }, 1)
                                }, 2),
                                new Upgrade(MIST_VEIL, new Upgrade[]{
                                        new Upgrade(MIST_VEIL_DURATION_I, 1)
                                }, 2),
                                new Upgrade(MIST_FREEZE, 3)
                        }, true, 0)
                }, 0)
        });
        addAbility(new HeavyFogAbility(), 0);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new MistElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    /**
     * @return true se o jogador já tem Air E já comprou todos os nós da
     * árvore de skills de Air (masterizou o elemento base) — mesma regra
     * de Gas/Mud/Crystal/Atmosphere.
     */
    public static boolean canAcquire(Bender bender) {
        Element air = AirElement.get();
        return bender.hasElement(air) && air.isSkillTreeComplete(bender);
    }

    public static boolean isMistBender(Bender bender) {
        return bender.hasElement(get());
    }

    /**
     * Marca o nó raiz "mistCloud" (preço 0) como já comprado. Chame logo
     * depois de {@code bender.addElement(MistElement.get(), true)} no
     * momento da concessão (ver MoreBendingCommand) -- mesmo motivo do
     * {@code GasElement#autoUnlockRoot}: "mistCloud" é o único filho
     * direto da raiz sintética do Element, então sem esse desbloqueio
     * manual a árvore inteira fica travada mesmo com level de sobra.
     */
    public static void autoUnlockRoot(Bender bender) {
        Upgrade cloudNode = get().root.children[0]; // mistCloud
        bender.getData().upgrades.put(cloudNode, true);
    }

    /** Atalho pras abilities, que só têm o ServerPlayer em mãos. */
    public static boolean isMistBender(ServerPlayer player) {
        Bender bender = Bender.getBender(player);
        return bender != null && isMistBender(bender);
    }

    /** Atalho pras abilities: o nó precisa ter sido comprado pelo jogador. */
    public static boolean hasUpgrade(ServerPlayer player, String upgradeName) {
        Bender bender = Bender.getBender(player);
        return bender != null && bender.getData().canUseUpgrade(upgradeName);
    }

    /**
     * "Masterizado" = crescimento todo comprado (tamanho + vent no máximo)
     * E uma das três especializações levada até o fim — não dá pra exigir
     * as três porque são mutuamente exclusivas. Mesmo critério do Gas.
     */
    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        if (!bender.hasElement(this)) {
            return false;
        }
        boolean growthMaxed = bender.getData().canUseUpgrade(MIST_CLOUD_SIZE_II)
                && bender.getData().canUseUpgrade(MIST_VENT_II);
        boolean specializationMaxed = bender.getData().canUseUpgrade(MIST_CHOKE_DAMAGE_II)
                || bender.getData().canUseUpgrade(MIST_VEIL_DURATION_I)
                || bender.getData().canUseUpgrade(MIST_FREEZE);
        return growthMaxed && specializationMaxed;
    }
}