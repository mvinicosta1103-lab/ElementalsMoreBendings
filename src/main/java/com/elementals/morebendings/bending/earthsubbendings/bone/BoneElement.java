package com.elementals.morebendings.bending.earthsubbendings.bone;

import com.elementals.morebendings.data.PlayerSubbendingData;
import com.elementals.morebendings.registry.ModAttachments;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.earth.EarthElement;
import net.minecraft.server.level.ServerPlayer;

/**
 * Bone Bending -- sub-bending de Earth, registrada como {@link Element} de
 * verdade no mod base (mesmo esquema que {@link
 * com.elementals.morebendings.bending.earthsubbendings.mud.MudElement} e
 * {@link com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalElement}
 * já usam).
 *
 * REGRA DE AQUISIÇÃO (ver {@link #canAcquire}): diferente de Mud/Crystal
 * (que exigem masterizar a árvore de Earth inteira), Bone só exige:
 *  1. já ter Earth; e
 *  2. já ter estado a até {@link #BLOOD_PROXIMITY_RANGE} blocos de um Blood
 *     bender em algum momento -- não precisa ser AGORA, é um evento
 *     histórico. Ver {@link BloodProximityTracker}, que roda em background
 *     no servidor e marca isso permanentemente em {@link
 *     PlayerSubbendingData#setMetBloodBender} assim que acontece (e {@link
 *     com.elementals.morebendings.commands.MoreBendingCommand}, que é quem
 *     de fato checa essa regra na hora de conceder via comando).
 *
 * Duas habilidades raiz, ambas grátis (preço 0):
 *  - boneControl: conjura e guia uma farpa de osso telecineticamente antes
 *    de arremessar. Ver {@link BoneControlAbility}.
 *  - bonePuppeteer: controle de verdade -- mira uma criatura; se for um
 *    morto-vivo de verdade, vira um fantoche literal por um tempo; se for
 *    um player ou outra criatura viva, trava os ossos dela num debuff
 *    pesado. Ver {@link BonePuppeteerAbility}.
 */
public class BoneElement extends Element {

    public static final String NAME = "Bone";

    public static final String BONE_CONTROL = "boneControl";
    public static final String BONE_PUPPETEER = "bonePuppeteer";

    /** Distância (em blocos) que conta como "perto" de um Blood bender pra
     * fins do pré-requisito de aquisição. Ver {@link BloodProximityTracker}. */
    public static final double BLOOD_PROXIMITY_RANGE = 20.0;

    public BoneElement() {
        super(NAME, new Upgrade[]{
                new Upgrade(BONE_CONTROL, 0),   // grátis -- ver BoneControlAbility
                new Upgrade(BONE_PUPPETEER, 0)  // grátis -- ver BonePuppeteerAbility
        });
        addAbility(new BoneControlAbility(), 0);
        addAbility(new BonePuppeteerAbility(), 1);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new BoneElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    /**
     * @return true se o jogador já tem Earth E já foi detectado, em algum
     * momento, a até {@link #BLOOD_PROXIMITY_RANGE} blocos de um Blood
     * bender (ver {@link PlayerSubbendingData#hasMetBloodBender}).
     *
     * EXCEÇÃO -- concessão via {@code /morebending grant ... bone}: o
     * comando marca {@link PlayerSubbendingData#setMetBloodBender} como
     * {@code true} logo antes de chamar este método (ver {@code
     * MoreBendingCommand.runRealElement}), então o evento de proximidade
     * em si não precisa ter acontecido de verdade -- o comando já é a
     * autorização que o substitui. O requisito de já ter Earth continua
     * valendo normalmente mesmo nesse caminho.
     */
    public static boolean canAcquire(Bender bender) {
        if (!(bender.player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        Element earth = EarthElement.get();
        if (!bender.hasElement(earth)) {
            return false;
        }
        PlayerSubbendingData data = serverPlayer.getData(ModAttachments.SUBBENDINGS);
        return data.hasMetBloodBender();
    }

    public static boolean isBoneBender(Bender bender) {
        return bender.hasElement(get());
    }

    /**
     * Marca os dois nós raiz ("boneControl" e "bonePuppeteer", ambos preço
     * 0) como já comprados. Chame logo depois de {@code
     * bender.addElement(BoneElement.get(), true)} no momento da concessão
     * (ver MoreBendingCommand) -- SEM isso, a skill tree do Bone fica
     * travada pra sempre, mesmo com o jogador elegível e com o elemento
     * concedido. Mesmo bug/mesma correção do {@code
     * GasElement.autoUnlockRoot} -- ver o javadoc completo lá pro motivo.
     */
    public static void autoUnlockRoot(Bender bender) {
        for (Upgrade rootChild : get().root.children) {
            bender.getData().upgrades.put(rootChild, true);
        }
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this)
                && bender.getData().canUseUpgrade(BONE_CONTROL)
                && bender.getData().canUseUpgrade(BONE_PUPPETEER);
    }
}