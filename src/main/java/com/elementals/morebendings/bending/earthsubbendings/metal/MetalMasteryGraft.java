package com.elementals.morebendings.bending.earthsubbendings.metal;

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
 * Enxerta as 3 habilidades que antes formavam o Element "MetalMastery"
 * deste addon direto na árvore de skills REAL do Metal Bending base
 * ({@code dev.saperate.elementals.elements.metal.MetalElement}), em vez de
 * mantê-las como um sub-bending/scroll separado.
 * <br><br>
 * Por quê isso funciona sem tocar no mod base (que é uma dependência
 * compilada, não código-fonte deste projeto):
 * <ul>
 *   <li>{@code Upgrade.children} e {@code Upgrade.parent} são campos
 *   públicos e mutáveis -- dá pra anexar filhos novos em qualquer nó já
 *   existente depois que a árvore foi construída.</li>
 *   <li>{@code Element.root} também é público, e {@code Element.addAbility}
 *   /{@code registerUpgradeKeybind} são métodos públicos -- dá pra registrar
 *   habilidades e keybinds extras na MESMA instância de {@code Element} que
 *   o mod base já criou.</li>
 * </ul>
 * Cada uma das 8 habilidades foi pendurada num leaf diferente da árvore de
 * Metal -- os 4 ramos-raiz (metalBullet, metalBind, metalCable, metalArmor)
 * agora têm 2 grafts cada (um por sub-cadeia/leaf distinto dentro do mesmo
 * ramo, nunca dois grafts no mesmo leaf) -- ver escolha de {@code graft()}
 * abaixo. Como {@code Upgrade#canBuy} exige que o pai já tenha sido
 * comprado, isso por si só recria a regra "precisa ter dominado o Metal
 * inteiro" sem precisar de nenhum {@code canAcquire()} customizado: só
 * chega no nó novo quem já comprou a cadeia inteira até ali.
 * <br><br>
 * IMPORTANTE: {@code Element.addAbility(ability, slot)} vincula o slot de
 * forma incondicional (todo player com Metal ativo tem os 12 slots
 * vinculados -- os 4 originais do mod base + os 8 deste addon --
 * independente de ter comprado os upgrades) -- é o mesmo esquema que o
 * próprio mod base usa pros 4 ramos originais (as AbilityMetal1..4 fazem
 * sua própria checagem interna de upgrade). Por isso {@link
 * MetalSenseAbility}, {@link MetalSlamAbility}, {@link
 * ChestplateDevelopAbility}, {@link MetalGrappleAbility}, {@link
 * MetalShrapnelAbility}, {@link MetalRedirectAbility}, {@link
 * MetalCoilAbility} e {@link MetalPlatingAbility} chamam
 * {@code bender.getData().canUseUpgrade(...)} logo no início do
 * {@code onCall} -- sem isso o player usaria a habilidade só por ter Metal
 * selecionado, sem precisar comprar nada.
 * <br><br>
 * Timing: {@link #graft()} precisa rodar DEPOIS que o mod base já registrou
 * seu {@code MetalElement}. Isso é garantido porque o mod base chama
 * {@code Elementals.init()} (que constrói todos os Elements, incluindo
 * Metal) direto no construtor do seu mod NeoForge -- e construtores de mod
 * de TODOS os mods rodam antes de qualquer {@code FMLCommonSetupEvent} de
 * qualquer mod. Este método é chamado em {@code CommonClass.init()}, que
 * roda no {@code FMLCommonSetupEvent} deste addon -- portanto sempre depois.
 */
public final class MetalMasteryGraft {

    public static final String METAL_SENSE = "metalSense";
    public static final String METAL_SLAM = "metalSlam";
    public static final String CHESTPLATE_DEVELOP = "chestplateDevelop";
    public static final String METAL_GRAPPLE = "metalGrapple";
    public static final String METAL_SHRAPNEL = "metalShrapnel";
    public static final String METAL_REDIRECT = "metalRedirect";
    public static final String METAL_COIL = "metalCoil";
    public static final String METAL_PLATING = "metalPlating";

    /** Mesma ordem de grandeza dos nós "grandes" já existentes na árvore
     * base (metalBullet/metalBind/metalCable/metalArmor/metalLance = 4). */
    private static final int GRAFT_PRICE = 4;

    private MetalMasteryGraft() {
    }

    /** Chame uma vez, em {@code CommonClass.init()}. */
    public static void graft() {
        Element metal = Element.getElement("Metal");
        if (metal == null) {
            Constants.LOG.error("MetalMasteryGraft: Metal base não encontrado -- enxerto abortado.");
            return;
        }

        // metalSense -- fim do ramo metalCable (ícone já usa metal_cable, ver lang).
        graftOnto(metal, "metalCablePrecisionI", METAL_SENSE);
        // metalSlam -- fim do sub-ramo metalLance dentro de metalBullet (ícone usa metal_lance).
        graftOnto(metal, "metalLanceDamageII", METAL_SLAM);
        // chestplateDevelop -- fim do ramo metalArmor (ícone usa metal_armor).
        graftOnto(metal, "metalDecoyDamageII", CHESTPLATE_DEVELOP);
        // metalGrapple -- fim do ramo metalBind, único dos 4 ramos-raiz que
        // ainda não tinha nenhuma habilidade enxertada nele (ícone metal_bind).
        graftOnto(metal, "metalBindEfficiencyII", METAL_GRAPPLE);
        // metalShrapnel -- leaf metalBulletDamageI, DENTRO de metalBullet mas
        // fora do sub-ramo metalLance (já usado por metalSlam); ícone metal_bullet.
        graftOnto(metal, "metalBulletDamageI", METAL_SHRAPNEL);
        // metalRedirect -- leaf metalLanceRedirectI, sub-ramo metalLance
        // (folha IRMÃ de metalLanceDamageII, já usada por metalSlam); ícone metal_lance.
        graftOnto(metal, "metalLanceRedirectI", METAL_REDIRECT);
        // metalCoil -- leaf metalCableEfficiencyII, ramo metalCable (folha
        // IRMÃ de metalCablePrecisionI, já usada por metalSense); ícone metal_cable.
        graftOnto(metal, "metalCableEfficiencyII", METAL_COIL);
        // metalPlating -- leaf metalArmorEfficiencyI, TRONCO do ramo metalArmor
        // (cadeia diferente de metalDecoy, já usado por chestplateDevelop); ícone metal_armor.
        graftOnto(metal, "metalArmorEfficiencyI", METAL_PLATING);

        // Slots de bind 0-3 já pertencem aos 4 ramos-raiz do Metal base
        // (AbilityMetal1..4) -- usamos os próximos 8 livres (slots 4-11,
        // ver KeyAbility1..12 do mod base) -- MAX_KEYBINDS=12 esgotado.
        metal.addAbility(new MetalSenseAbility(), 4);
        metal.addAbility(new MetalSlamAbility(), 5);
        metal.addAbility(new ChestplateDevelopAbility(), 6);
        metal.addAbility(new MetalGrappleAbility(), 7);
        metal.addAbility(new MetalShrapnelAbility(), 8);
        metal.addAbility(new MetalRedirectAbility(), 9);
        metal.addAbility(new MetalCoilAbility(), 10);
        metal.addAbility(new MetalPlatingAbility(), 11);
        metal.registerUpgradeKeybind(METAL_SENSE, 4);
        metal.registerUpgradeKeybind(METAL_SLAM, 5);
        metal.registerUpgradeKeybind(CHESTPLATE_DEVELOP, 6);
        metal.registerUpgradeKeybind(METAL_GRAPPLE, 7);
        metal.registerUpgradeKeybind(METAL_SHRAPNEL, 8);
        metal.registerUpgradeKeybind(METAL_REDIRECT, 9);
        metal.registerUpgradeKeybind(METAL_COIL, 10);
        metal.registerUpgradeKeybind(METAL_PLATING, 11);
    }

    private static void graftOnto(Element metal, String parentUpgradeName, String newUpgradeName) {
        Upgrade parent = metal.root.getUpgradeByNameRecursive(parentUpgradeName);
        if (parent == null) {
            Constants.LOG.error(
                    "MetalMasteryGraft: nó pai \"{}\" não encontrado na árvore de Metal -- \"{}\" não foi enxertado.",
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
     * @return true se a entidade estiver com pelo menos 1 peça de armadura
     * de metal DE VERDADE (ferro, ouro, cota de malha ou netherite --
     * couro/diamante/tartaruga NÃO contam) equipada em qualquer slot.
     * Usado por {@link MetalSenseAbility} e {@link MetalSlamAbility}.
     */
    public static boolean isWearingMetal(LivingEntity entity) {
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