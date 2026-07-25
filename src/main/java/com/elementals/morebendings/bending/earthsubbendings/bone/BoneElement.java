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
 */
public class BoneElement extends Element {

    public static final String NAME = "Bone";

    /** Distância (em blocos) que conta como "perto" de um Blood bender pra
     * fins do pré-requisito de aquisição. Ver {@link BloodProximityTracker}. */
    public static final double BLOOD_PROXIMITY_RANGE = 20.0;

    public BoneElement() {
        super(NAME, new Upgrade[]{
                new Upgrade("boneControl", 0) // grátis -- ver BoneControlAbility
        });
        addAbility(new BoneControlAbility(), 0);
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

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this) && bender.getData().canUseUpgrade("boneControl");
    }
}