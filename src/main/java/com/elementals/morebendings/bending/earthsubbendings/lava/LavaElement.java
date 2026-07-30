package com.elementals.morebendings.bending.earthsubbendings.lava;

import com.mojang.logging.LogUtils;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.earth.EarthElement;
import org.slf4j.Logger;

/**
 * Lava Bending — sub-bending de Earth, registrada como um {@link Element}
 * de verdade no mod base.
 *
 * === PATCH DE DIAGNÓSTICO (temporário) ===
 * O padrão observado é: lavaPool, lavaJet e magmaSpike (índices 0-2)
 * aparecem e funcionam normalmente na skill tree; a partir de lavaShuriken
 * (índice 3) nada mais aparece -- nem lavaShuriken, nem lavaSurf,
 * volcanicEruption ou lavaArmor (índices 4-6).
 *
 * Isso é o comportamento clássico de uma exceção lançada no meio do
 * construtor: se addAbility(new LavaShurikenAbility(), 3) lançar
 * qualquer Throwable, tudo que vem DEPOIS dele no construtor nunca
 * executa -- os índices 4, 5 e 6 nunca são registrados. Isso bate
 * exatamente com o que foi reportado.
 *
 * O que eu NÃO sei, porque addAbility() vive dentro do jar compilado da
 * lib base (dev.saperate.elementals.elements.Element) e eu não tenho
 * acesso ao código-fonte dela, é qual é a condição exata que ela valida
 * e por que ela rejeita especificamente essa ability/upgrade. Pode ser,
 * por exemplo:
 *   - Um limite interno de abilities "bindáveis" por elemento (o README
 *     do addon original menciona só 4 keybinds -- bind1 a bind4 -- por
 *     elemento; se addAbility() valida isso e o índice 3 já estoura
 *     algum limite de slots diretos, é aí).
 *   - Alguma checagem de recurso (ícone, entrada de lang) atrelada ao
 *     upgrade "lavaShuriken" que falta.
 *   - Um problema de ordem de carregamento (ModEntities.LAVA_SHURIKEN
 *     ainda não registrado quando addAbility tenta resolver algo
 *     relacionado à entidade).
 *
 * Em vez de adivinhar, este construtor agora envolve CADA addAbility()
 * num try/catch que loga a exception completa (com stes de índice/nome)
 * antes de decidir se continua ou não. Rode o jogo com esse patch, abra
 * o latest.log e procure por "[LavaElement]" -- a mensagem vai dizer
 * exatamente qual ability falhou e o stacktrace real da causa. Depois
 * disso dá pra reverter esse try/catch e aplicar a correção definitiva
 * (que depende do que aparecer no log).
 */
public class LavaElement extends Element {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String NAME = "Lava";

    public static final String LAVA_POOL = "lavaPool";
    public static final String LAVA_JET = "lavaJet";
    public static final String MAGMA_SPIKE = "magmaSpike";
    public static final String LAVA_SHURIKEN = "lavaShuriken";
    public static final String LAVA_SURF = "lavaSurf";
    public static final String VOLCANIC_ERUPTION = "volcanicEruption";
    public static final String LAVA_ARMOR = "lavaArmor";

    public LavaElement() {
        super(NAME, new Upgrade[]{
                new Upgrade(LAVA_POOL, 0),
                new Upgrade(LAVA_JET, 0),
                new Upgrade(MAGMA_SPIKE, 0),
                new Upgrade(LAVA_SHURIKEN, 0),
                new Upgrade(LAVA_SURF, 0),
                new Upgrade(VOLCANIC_ERUPTION, 0),
                new Upgrade(LAVA_ARMOR, 0)
        });

        safeAddAbility(LAVA_POOL, new LavaPoolAbility(), 0);
        safeAddAbility(LAVA_JET, new LavaJetAbility(), 1);
        safeAddAbility(MAGMA_SPIKE, new MagmaSpikeAbility(), 2);
        safeAddAbility(LAVA_SHURIKEN, new LavaShurikenAbility(), 3);
        safeAddAbility(LAVA_SURF, new LavaSurfAbility(), 4);
        safeAddAbility(VOLCANIC_ERUPTION, new VolcanicEruptionAbility(), 5);
        safeAddAbility(LAVA_ARMOR, new LavaArmorAbility(), 6);
    }

    /**
     * Wrapper de diagnóstico em volta de addAbility(). Loga qualquer
     * exceção com o nome da ability e o índice, e RE-LANÇA em seguida --
     * ou seja, o comportamento de crash continua o mesmo de antes (pra
     * não mascarar o bug), só que agora com uma mensagem clara no log
     * dizendo qual ability e qual foi a causa raiz.
     *
     * Depois de identificar a causa pelo log, troque essa chamada de
     * volta para addAbility(ability, index) direto, já com a correção
     * aplicada.
     */
    private void safeAddAbility(String upgradeName, Ability ability, int index) {
        try {
            addAbility(ability, index);
            LOGGER.info("[LavaElement] OK registrando '{}' no índice {}", upgradeName, index);
        } catch (Throwable t) {
            LOGGER.error("[LavaElement] FALHOU ao registrar '{}' no índice {} -- causa raiz abaixo:", upgradeName, index, t);
            throw t; // não engole o erro -- mantém o crash original, só que agora com log claro
        }
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new LavaElement();
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

    public static boolean isLavaBender(Bender bender) {
        return bender.hasElement(get());
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this)
                && bender.getData().canUseUpgrade(LAVA_POOL)
                && bender.getData().canUseUpgrade(LAVA_JET)
                && bender.getData().canUseUpgrade(MAGMA_SPIKE)
                && bender.getData().canUseUpgrade(LAVA_SHURIKEN)
                && bender.getData().canUseUpgrade(LAVA_SURF)
                && bender.getData().canUseUpgrade(VOLCANIC_ERUPTION)
                && bender.getData().canUseUpgrade(LAVA_ARMOR);
    }
}