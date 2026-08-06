package com.elementals.morebendings.bending.earthsubbendings.metal;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;

/**
 * Metal Mastery — sub-bending ADICIONAL de quem já tem a Metal Bending
 * ORIGINAL do mod base ({@code dev.saperate.elementals.elements.metal.MetalElement})
 * e já masterizou a árvore de skills DELA. Mesmo padrão de {@link
 * com.elementals.morebendings.bending.earthsubbendings.bone.BoneElement}
 * (também gated atrás de um pré-requisito que não é simplesmente "ter Earth
 * mastered") -- aqui o pré-requisito é ter e masterizar o Metal base, e não
 * o Earth (ver mensagem em {@code MoreBendingCommand#eligibilityMessage}:
 * "precisa ter Metal Bending e ter masterizado a árvore de Metal inteira").
 *
 * Antes desta correção esta classe era um stub vazio (sem construtor,
 * sem register()/get()/canAcquire(), sem isWearingMetal()) -- por isso
 * nada relacionado a Metal compilava: {@code CommonClass.init()} chama
 * {@code MetalElement.register()}, {@code ElementalsMoreBendingsMod}
 * chama {@code MetalElement.canAcquire(bender)}, e {@link
 * MetalSenseAbility} chama {@code MetalElement.isWearingMetal(entity)}.
 *
 * Só 3 habilidades -- cabem direto nos 4 filhos da raiz sem precisar
 * aninhar nada (ver o comentário grande em {@code LavaElement} sobre o
 * limite de 4 do {@code UpgradeTreeScreen#render()} do mod base).
 */
public class MetalElement extends Element {

    public static final String NAME = "MetalMastery";

    public static final String METAL_SENSE = "metalSense";
    public static final String METAL_SLAM = "metalSlam";
    public static final String CHESTPLATE_DEVELOP = "chestplateDevelop";

    public MetalElement() {
        super(NAME, new Upgrade[]{
                new Upgrade(METAL_SENSE, 0),
                new Upgrade(METAL_SLAM, 0),
                new Upgrade(CHESTPLATE_DEVELOP, 0)
        });

        addAbility(new MetalSenseAbility(), 0);
        addAbility(new MetalSlamAbility(), 1);
        addAbility(new ChestplateDevelopAbility(), 2);

        // Registro explícito dos slots de bind (mesmo motivo documentado em
        // LavaElement/CrystalElement): sem isso, getKeybindSlotForUpgrade()
        // pode cair pro índice errado quando upgrades futuros forem
        // aninhados dentro de um destes 3 ramos.
        registerUpgradeKeybind(METAL_SENSE, 0);
        registerUpgradeKeybind(METAL_SLAM, 1);
        registerUpgradeKeybind(CHESTPLATE_DEVELOP, 2);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new MetalElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    /**
     * @return true se o bender já tiver a Metal Bending ORIGINAL do mod
     * base e já tiver masterizado a árvore de skills dela inteira (mesmo
     * padrão de LavaElement/CrystalElement#canAcquire, só que o elemento
     * pré-requisito aqui é o Metal base, não o Earth).
     */
    public static boolean canAcquire(Bender bender) {
        Element baseMetal = dev.saperate.elementals.elements.metal.MetalElement.get();
        return baseMetal != null && bender.hasElement(baseMetal) && baseMetal.isSkillTreeComplete(bender);
    }

    public static boolean isMetalMasteryBender(Bender bender) {
        return bender.hasElement(get());
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this)
                && bender.getData().canUseUpgrade(METAL_SENSE)
                && bender.getData().canUseUpgrade(METAL_SLAM)
                && bender.getData().canUseUpgrade(CHESTPLATE_DEVELOP);
    }

    /**
     * @return true se a entidade estiver com pelo menos 1 peça de armadura
     * de metal DE VERDADE (ferro, ouro, cota de malha ou netherite --
     * couro/diamante/tartaruga NÃO contam) equipada em qualquer slot.
     * Usado por {@link MetalSenseAbility} e {@link MetalSlamAbility} pra
     * só afetar quem estiver "de metal".
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