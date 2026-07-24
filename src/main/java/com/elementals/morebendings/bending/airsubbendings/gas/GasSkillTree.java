package com.elementals.morebendings.bending.airsubbendings.gas;

import dev.saperate.elementals.elements.Upgrade;

/**
 * Árvore de upgrades da sub-bending Gas, no mesmo formato que o mod base
 * (Elementals) usa pra Air/Water/Earth/Fire — reaproveitamos a própria
 * classe {@link Upgrade} do mod base porque ela já resolve pai/filho,
 * preço em pontos e serialização em NBT (ver {@link Upgrade#onSave} /
 * {@link Upgrade#onRead}), então não precisamos reinventar isso.
 *
 * IMPORTANTE: isso NÃO registra um {@code Element} novo no mod base — Gas
 * continua sendo uma sub-bending (precisa de Air primeiro), não uma bending
 * própria selecionável na tela principal. Por isso construímos e
 * gerenciamos essa árvore por conta própria em vez de estender
 * {@code dev.saperate.elementals.elements.Element}.
 *
 * Estrutura (espelhando o estilo do mod original: uma habilidade base
 * gratuita, um ramo de "crescimento" que melhora ela, e um ramo de
 * "especialização" exclusivo — só uma escolha entre Sufocamento / Vazamento
 * / Ignição pode ficar ativa por vez, igual o "exclusive" da airStream no
 * mod base):
 *
 * gasCloud (grátis, é a habilidade em si — nuvem de gás em volta do bender)
 *  ├─ gasCloudSizeI ─ gasCloudSizeII        (aumenta raio/duração da nuvem)
 *  ├─ gasVentI ─ gasVentII                  (reduz o cooldown)
 *  └─ gasSpecialization (nó guarda-chuva, exclusive=true)
 *      ├─ gasSuffocate ─ gasSuffocateDamageI ─ gasSuffocateDamageII
 *      ├─ gasLeak ─ gasLeakDurationI
 *      └─ gasIgnite
 *
 * O "exclusive" fica no nó gasSpecialization (não nos 3 filhos individuais),
 * porque no mod base a flag {@code exclusive} vive no PAI e vale pros
 * filhos dele — ver {@code UpgradeTreeScreen.renderTitle}, que mostra o
 * aviso de "exclusivo" quando {@code hoveredUpgrade.parent.exclusive}.
 *
 * O preço de cada nó é em "pontos de sub-bending" (ver
 * {@link com.elementals.morebendings.data.PlayerSubbendingData}), não em
 * XP vanilla — dá pra trocar depois se preferirem usar níveis de XP como o
 * mod base faz.
 */
public final class GasSkillTree {

    // ---- nomes dos nós (usados como chave de save/keybind/tradução) ----
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
    /** Nó "guarda-chuva" (preço 0) só pra agrupar as 3 especializações como exclusivas entre si. */
    public static final String GAS_SPECIALIZATION = "gasSpecialization";

    /**
     * Raiz da árvore. É um nó "gasRoot" invisível (preço 0, nunca comprável
     * sozinho) que só existe pra ter um único ponto de entrada — igual o
     * mod base faz internamente quando você passa {@code Upgrade[]} pro
     * construtor de {@code Element} em vez de um {@code Upgrade} já pronto.
     */
    public static final Upgrade ROOT = new Upgrade("gasRoot", new Upgrade[]{
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
    }, 0);

    /** Nó raiz de fato da sub-bending (o "gasCloud"), pra não expor o gasRoot artificial pra fora. */
    public static Upgrade cloudNode() {
        return ROOT.children[0];
    }

    public static Upgrade byName(String name) {
        return ROOT.getUpgradeByNameRecursive(name);
    }

    private GasSkillTree() {
    }
}