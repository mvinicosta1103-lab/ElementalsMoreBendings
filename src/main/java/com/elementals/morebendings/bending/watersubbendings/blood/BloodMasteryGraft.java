package com.elementals.morebendings.bending.watersubbendings.blood;

import com.elementals.morebendings.Constants;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;

import java.util.Arrays;

/**
 * Enxerta 8 novas habilidades direto na árvore de skills REAL do Blood
 * Bending base ({@code dev.saperate.elementals.elements.blood.BloodElement}),
 * em vez de mantê-las como um sub-bending/Element separado deste addon --
 * exatamente o mesmo esquema de {@link
 * com.elementals.morebendings.bending.earthsubbendings.metal.MetalMasteryGraft}
 * e {@link com.elementals.morebendings.bending.firesubbendings.lightning.LightningMasteryGraft}
 * (ver aquelas classes pra explicação completa do "porquê funciona sem
 * tocar no mod base").
 * <br><br>
 * Árvore REAL de Blood (4 ramos-raiz, preços copiados 1:1 da árvore
 * original via decompilação de {@code BloodElement.class}):
 * <pre>
 * Blood (0)
 * ├── bloodPush (2)
 * │    └── bloodPushPowerI (4)
 * │         ├── bloodControl (4)
 * │         │    └── bloodControlPrecisionI (2, exclusive)
 * │         │         ├── bloodControlPrecisionII (2)  [leaf -> bloodVeinLock]
 * │         │         └── bloodControlPowerI (2)        [leaf -> bloodForcedGrasp]
 * │         └── bloodShield (4)                          [leaf livre, sem graft --
 * │                                                        ver nota de slots abaixo]
 * ├── bloodShot (4)
 * │    └── bloodShotEfficiencyI (2)
 * │         ├── bloodShotEfficiencyII (2)                [leaf -> bloodTwinShot]
 * │         └── bloodShotPrecisionI (2)                  [leaf -> bloodMarkedVein]
 * ├── bloodStep (4)
 * │    └── bloodStepRangeI (2)
 * │         ├── bloodStepRangeII (2)                     [leaf -> bloodRush]
 * │         └── bloodOvercharge (4)
 * │              └── bloodOverchargeStrengthI (2)        [leaf -> bloodAdrenalNumbness]
 * └── bloodBag (4)
 *      └── bloodParalysis (4)
 *           ├── bloodParalysisEfficiencyI (2)
 *           │    └── bloodParalysisEfficiencyII (2)      [leaf -> bloodSilentGrip]
 *           └── bloodParalysisRangeI (2)                 [leaf -> bloodWideGrasp]
 * </pre>
 * Cada graft pendura numa folha DIFERENTE já existente -- nenhum nó passa
 * de 4 filhos (limite do {@code UpgradeTreeScreen#render()} do mod base,
 * mesmo comentário em {@code MetalMasteryGraft}/{@code LightningMasteryGraft}):
 * a raiz "Blood" já tem 4 filhos (não mexemos nela) e todo pai escolhido
 * acima tinha 0 filhos antes do enxerto, então fica só com 1. Como
 * {@code Upgrade#canBuy} exige o pai já comprado, isso recria sozinho a
 * regra "precisa ter dominado aquele ramo inteiro" sem nenhum
 * {@code canAcquire()} customizado.
 * <br><br>
 * Por que só 8 e não os 9 leaves disponíveis: {@code Ability.MAX_KEYBINDS
 * = 12} é um limite físico do mod base (12 slots de tecla, ver
 * {@code KeyAbility1..12}). Blood já ocupa os slots 0-3 com
 * {@code AbilityBlood1..4} -- sobram exatamente 8 slots livres (4-11).
 * {@code bloodShield} (o único leaf sem novo filho) ficou de fora pra não
 * estourar esse limite; foi o escolhido por já ser o mais próximo,
 * em espírito, de uma habilidade nova de área que já cobrimos noutro
 * lugar (fica reservado caso um addon futuro libere mais slots).
 * <br><br>
 * Timing: idêntico ao documentado em {@code MetalMasteryGraft} -- chame
 * {@link #graft()} uma única vez, em {@code CommonClass.init()}, depois
 * que o mod base já registrou {@code BloodElement}. Cada uma das 8
 * abilities grafadas chama {@code bender.getData().canUseUpgrade(...)}
 * logo no início do {@code onCall} -- sem isso o player usaria a
 * habilidade só por ter Blood selecionado, sem precisar comprar o
 * upgrade.
 */
public final class BloodMasteryGraft {

    public static final String BLOOD_VEIN_LOCK = "bloodVeinLock";
    public static final String BLOOD_FORCED_GRASP = "bloodForcedGrasp";
    public static final String BLOOD_TWIN_SHOT = "bloodTwinShot";
    public static final String BLOOD_MARKED_VEIN = "bloodMarkedVein";
    public static final String BLOOD_RUSH = "bloodRush";
    public static final String BLOOD_ADRENAL_NUMBNESS = "bloodAdrenalNumbness";
    public static final String BLOOD_SILENT_GRIP = "bloodSilentGrip";
    public static final String BLOOD_WIDE_GRASP = "bloodWideGrasp";

    /** Mesma ordem de grandeza dos nós "grandes" já existentes na árvore
     * base (bloodShot/bloodStep/bloodBag/bloodControl/bloodParalysis = 4). */
    private static final int GRAFT_PRICE = 4;

    private BloodMasteryGraft() {
    }

    /** Chame uma vez, em {@code CommonClass.init()}. */
    public static void graft() {
        Element blood = Element.getElement("Blood");
        if (blood == null) {
            Constants.LOG.error("BloodMasteryGraft: Blood base não encontrado -- enxerto abortado.");
            return;
        }

        // bloodVeinLock -- leaf bloodControlPrecisionII, ramo exclusive de
        // bloodControlPrecisionI (ícone blood_control).
        graftOnto(blood, "bloodControlPrecisionII", BLOOD_VEIN_LOCK);
        // bloodForcedGrasp -- leaf IRMÃ bloodControlPowerI, mesmo ramo
        // exclusive de bloodControlPrecisionI (ícone blood_control).
        graftOnto(blood, "bloodControlPowerI", BLOOD_FORCED_GRASP);
        // bloodTwinShot -- leaf bloodShotEfficiencyII, ramo bloodShot
        // (ícone blood_shot).
        graftOnto(blood, "bloodShotEfficiencyII", BLOOD_TWIN_SHOT);
        // bloodMarkedVein -- leaf IRMÃ bloodShotPrecisionI, mesmo ramo
        // bloodShot (ícone blood_shot).
        graftOnto(blood, "bloodShotPrecisionI", BLOOD_MARKED_VEIN);
        // bloodRush -- leaf bloodStepRangeII, ramo bloodStep (ícone blood_step).
        graftOnto(blood, "bloodStepRangeII", BLOOD_RUSH);
        // bloodAdrenalNumbness -- leaf bloodOverchargeStrengthI, sub-ramo
        // bloodOvercharge DENTRO de bloodStep (ícone blood_overcharge).
        graftOnto(blood, "bloodOverchargeStrengthI", BLOOD_ADRENAL_NUMBNESS);
        // bloodSilentGrip -- leaf bloodParalysisEfficiencyII, ramo
        // bloodBag/bloodParalysis (ícone blood_paralysis).
        graftOnto(blood, "bloodParalysisEfficiencyII", BLOOD_SILENT_GRIP);
        // bloodWideGrasp -- leaf IRMÃ bloodParalysisRangeI, mesmo ramo
        // bloodBag/bloodParalysis (ícone blood_paralysis).
        graftOnto(blood, "bloodParalysisRangeI", BLOOD_WIDE_GRASP);

        // Slots de bind 0-3 já pertencem aos 4 ramos-raiz do Blood base
        // (AbilityBlood1..4) -- usamos os últimos 8 livres (slots 4-11,
        // ver KeyAbility1..12 do mod base, MAX_KEYBINDS=12 esgotado).
        blood.addAbility(new BloodVeinLockAbility(), 4);
        blood.addAbility(new BloodForcedGraspAbility(), 5);
        blood.addAbility(new BloodTwinShotAbility(), 6);
        blood.addAbility(new BloodMarkedVeinAbility(), 7);
        blood.addAbility(new BloodRushAbility(), 8);
        blood.addAbility(new BloodAdrenalNumbnessAbility(), 9);
        blood.addAbility(new BloodSilentGripAbility(), 10);
        blood.addAbility(new BloodWideGraspAbility(), 11);
        blood.registerUpgradeKeybind(BLOOD_VEIN_LOCK, 4);
        blood.registerUpgradeKeybind(BLOOD_FORCED_GRASP, 5);
        blood.registerUpgradeKeybind(BLOOD_TWIN_SHOT, 6);
        blood.registerUpgradeKeybind(BLOOD_MARKED_VEIN, 7);
        blood.registerUpgradeKeybind(BLOOD_RUSH, 8);
        blood.registerUpgradeKeybind(BLOOD_ADRENAL_NUMBNESS, 9);
        blood.registerUpgradeKeybind(BLOOD_SILENT_GRIP, 10);
        blood.registerUpgradeKeybind(BLOOD_WIDE_GRASP, 11);
    }

    private static void graftOnto(Element blood, String parentUpgradeName, String newUpgradeName) {
        Upgrade parent = blood.root.getUpgradeByNameRecursive(parentUpgradeName);
        if (parent == null) {
            Constants.LOG.error(
                    "BloodMasteryGraft: nó pai \"{}\" não encontrado na árvore de Blood -- \"{}\" não foi enxertado.",
                    parentUpgradeName, newUpgradeName);
            return;
        }

        // Idempotente -- se graft() rodar mais de uma vez na mesma JVM (não
        // deveria, mas é barato garantir), não duplica o filho.
        for (Upgrade existing : parent.children) {
            if (existing.name.equals(newUpgradeName)) {
                return;
            }
        }

        Upgrade grafted = new Upgrade(newUpgradeName, GRAFT_PRICE);
        Upgrade[] newChildren = Arrays.copyOf(parent.children, parent.children.length + 1);
        newChildren[newChildren.length - 1] = grafted;
        parent.children = newChildren;
        grafted.setParent(parent);
    }
}