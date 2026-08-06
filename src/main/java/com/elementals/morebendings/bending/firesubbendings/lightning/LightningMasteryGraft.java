package com.elementals.morebendings.bending.firesubbendings.lightning;

import com.elementals.morebendings.Constants;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/**
 * Enxerta 7 novas habilidades direto na árvore de skills REAL do Lightning
 * Bending base ({@code dev.saperate.elementals.elements.lightning.LightningElement}),
 * em vez de mantê-las como um sub-bending/Element separado deste addon --
 * exatamente o mesmo esquema de {@link
 * com.elementals.morebendings.bending.earthsubbendings.metal.MetalMasteryGraft}
 * (ver aquela classe pra explicação completa do "porquê funciona sem tocar
 * no mod base").
 * <br><br>
 * Árvore REAL de Lightning (4 ramos-raiz, cada um com preço 4 -- mesma
 * "grandeza" usada por {@code MetalMasteryGraft#GRAFT_PRICE}):
 * <pre>
 * Lightning (0)
 * ├── lightningRedirection (4)
 * │    └── lightningRedirectionEfficiencyI (2)
 * │         ├── lightningRedirectionEfficiencyII (2)          [leaf -> lightningSense]
 * │         └── lightningBolt (4)
 * │              └── lightningBoltEfficiencyI (2)
 * │                   └── lightningBoltEfficiencyII (2)       [leaf -> lightningChainWhip]
 * ├── lightningVoltArc (4)
 * │    └── lightningVoltArcStrengthI (2)
 * │         ├── lightningEMP (4)
 * │         │    └── lightningEMPSizeI (2)                    [leaf -> thunderStep]
 * │         ├── lightningVoltArcStrengthII (2)                [leaf -> staticOvercharge]
 * │         └── lightningStaticAura (4)
 * │              └── lightningStaticAuraStrengthI (2)
 * │                   └── lightningStaticAuraStrengthII (2)   [leaf -> electroParalysis]
 * ├── lightningOvercharge (4)
 * │    └── lightningOverchargeStrengthI (2)
 * │         └── lightningOverchargeStrengthII (2)             [leaf -> lightningRod]
 * └── lightningStorm (4)
 *      └── lightningStormDurationI (2)                        [leaf -> judgmentStrike]
 * </pre>
 * Cada graft pendura numa folha DIFERENTE já existente -- nenhum nó passa
 * de 4 filhos (limite do {@code UpgradeTreeScreen#render()} do mod base,
 * ver o mesmo comentário em {@code LavaElement}/{@code MetalMasteryGraft}):
 * a raiz "Lightning" já tem 4 filhos (não mexemos nela) e
 * "lightningVoltArcStrengthI" já tinha 3 (ganha só +1, fica em 4). Como
 * {@code Upgrade#canBuy} exige o pai já comprado, isso recria sozinho a
 * regra "precisa ter dominado aquele ramo inteiro" sem nenhum
 * {@code canAcquire()} customizado.
 * <br><br>
 * Timing e semântica de {@code addAbility}/slots: idêntico ao documentado
 * em {@code MetalMasteryGraft} -- a base já ocupa os slots 0-3 (com
 * {@code AbilityLightning1..4}, os "toques genéricos" dos 4 ramos-raiz, via
 * {@code addAbility(ability, true)}), então usamos os slots 4-10
 * (MAX_KEYBINDS=12, sobra o 11 livre). Cada uma das 7 abilities grafadas
 * chama {@code bender.getData().canUseUpgrade(...)} logo no início do
 * {@code onCall} -- sem isso o player usaria a habilidade só por ter
 * Lightning selecionado, sem precisar comprar o upgrade.
 */
public final class LightningMasteryGraft {

    public static final String LIGHTNING_SENSE = "lightningSense";
    public static final String LIGHTNING_CHAIN_WHIP = "lightningChainWhip";
    public static final String THUNDER_STEP = "thunderStep";
    public static final String STATIC_OVERCHARGE = "staticOvercharge";
    public static final String ELECTRO_PARALYSIS = "electroParalysis";
    public static final String LIGHTNING_ROD = "lightningRod";
    public static final String JUDGMENT_STRIKE = "judgmentStrike";

    /** Mesma ordem de grandeza dos nós "grandes" já existentes na árvore
     * base (lightningRedirection/lightningVoltArc/lightningOvercharge/lightningStorm = 4). */
    private static final int GRAFT_PRICE = 4;

    private LightningMasteryGraft() {
    }

    /** Chame uma vez, em {@code CommonClass.init()}. */
    public static void graft() {
        Element lightning = Element.getElement("Lightning");
        if (lightning == null) {
            Constants.LOG.error("LightningMasteryGraft: Lightning base não encontrado -- enxerto abortado.");
            return;
        }

        // lightningSense -- leaf lightningRedirectionEfficiencyII, ramo
        // lightningRedirection (ícone lightning_redirection).
        graftOnto(lightning, "lightningRedirectionEfficiencyII", LIGHTNING_SENSE);
        // lightningChainWhip -- leaf lightningBoltEfficiencyII, sub-cadeia
        // lightningBolt DENTRO de lightningRedirection (ícone lightning_bolt).
        graftOnto(lightning, "lightningBoltEfficiencyII", LIGHTNING_CHAIN_WHIP);
        // thunderStep -- leaf lightningEMPSizeI, sub-ramo lightningEMP
        // dentro de lightningVoltArc (ícone lightning_emp).
        graftOnto(lightning, "lightningEMPSizeI", THUNDER_STEP);
        // staticOvercharge -- leaf lightningVoltArcStrengthII, folha IRMÃ
        // de lightningEMP/lightningStaticAura, ramo lightningVoltArc
        // (ícone lightning_volt_arc).
        graftOnto(lightning, "lightningVoltArcStrengthII", STATIC_OVERCHARGE);
        // electroParalysis -- leaf lightningStaticAuraStrengthII, sub-ramo
        // lightningStaticAura dentro de lightningVoltArc (ícone lightning_static_aura).
        graftOnto(lightning, "lightningStaticAuraStrengthII", ELECTRO_PARALYSIS);
        // lightningRod -- leaf lightningOverchargeStrengthII, ramo
        // lightningOvercharge, único ramo-raiz ainda sem nada enxertado
        // até aqui (ícone lightning_overcharge).
        graftOnto(lightning, "lightningOverchargeStrengthII", LIGHTNING_ROD);
        // judgmentStrike -- leaf lightningStormDurationI, ramo
        // lightningStorm (ícone lightning_localised_storm).
        graftOnto(lightning, "lightningStormDurationI", JUDGMENT_STRIKE);

        // Slots de bind 0-3 já pertencem aos 4 ramos-raiz do Lightning base
        // (AbilityLightning1..4) -- usamos os próximos 7 livres (slots
        // 4-10, ver KeyAbility1..12 do mod base, MAX_KEYBINDS=12).
        lightning.addAbility(new LightningSenseAbility(), 4);
        lightning.addAbility(new LightningChainWhipAbility(), 5);
        lightning.addAbility(new ThunderStepAbility(), 6);
        lightning.addAbility(new StaticOverchargeAbility(), 7);
        lightning.addAbility(new ElectroParalysisAbility(), 8);
        lightning.addAbility(new LightningRodAbility(), 9);
        lightning.addAbility(new JudgementStrikeAbility(), 10);
        lightning.registerUpgradeKeybind(LIGHTNING_SENSE, 4);
        lightning.registerUpgradeKeybind(LIGHTNING_CHAIN_WHIP, 5);
        lightning.registerUpgradeKeybind(THUNDER_STEP, 6);
        lightning.registerUpgradeKeybind(STATIC_OVERCHARGE, 7);
        lightning.registerUpgradeKeybind(ELECTRO_PARALYSIS, 8);
        lightning.registerUpgradeKeybind(LIGHTNING_ROD, 9);
        lightning.registerUpgradeKeybind(JUDGMENT_STRIKE, 10);
    }

    private static void graftOnto(Element lightning, String parentUpgradeName, String newUpgradeName) {
        Upgrade parent = lightning.root.getUpgradeByNameRecursive(parentUpgradeName);
        if (parent == null) {
            Constants.LOG.error(
                    "LightningMasteryGraft: nó pai \"{}\" não encontrado na árvore de Lightning -- \"{}\" não foi enxertado.",
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

    /**
     * @return true se a entidade for um bom condutor no momento: estiver
     * com pelo menos 1 peça de armadura de metal DE VERDADE (ferro, ouro,
     * cota de malha ou netherite -- couro/diamante/tartaruga NÃO contam)
     * equipada, OU estiver molhada (água/chuva/bolha). Usado por {@link
     * LightningSenseAbility} e {@link LightningChainWhipAbility} pra dar
     * bônus/alcance extra contra alvos "condutivos".
     */
    public static boolean isConductive(LivingEntity entity) {
        if (entity.isInWaterRainOrBubble()) {
            return true;
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.getItem() instanceof ArmorItem armor && isMetalMaterial(armor)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMetalMaterial(ArmorItem armor) {
        var material = armor.getMaterial();
        return material.is(ArmorMaterials.IRON)
                || material.is(ArmorMaterials.GOLD)
                || material.is(ArmorMaterials.CHAIN)
                || material.is(ArmorMaterials.NETHERITE);
    }
}