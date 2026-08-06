package com.elementals.morebendings.bending.earthsubbendings.glass;

import com.elementals.morebendings.bending.earthsubbendings.sand.SandElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import net.minecraft.server.level.ServerPlayer;

/**
 * Glass Bending — sub-bending de Earth, mesmo padrão de {@link
 * com.elementals.morebendings.bending.earthsubbendings.sand.SandElement}:
 * Element de verdade, registrada no mod base, com sua própria árvore de
 * skills e sistema de bind.
 *
 * REGRA DE AQUISIÇÃO: diferente de Mud/Crystal/Sand (que exigem Earth
 * masterizado), Glass só exige que o jogador já tenha (não precisa ter
 * masterizado) Sand Bending — ver {@link #canAcquire}. Só pode ser
 * concedida via comando (/morebending grant), nunca automaticamente.
 *
 * Árvore (mesmo formato de Gas/Mist/Plasma/Combustion -- "glassShards" é o
 * único filho GRÁTIS direto da raiz sintética e TEM filhos, então precisa
 * do mesmo hack de {@link #autoUnlockRoot}; "glassSpray" e "glassArmor"
 * entraram depois como os outros 2 filhos diretos -- 3 de 4 possíveis, ver
 * o limite documentado em {@code CrystalElement}):
 *
 * glassShards (grátis, é a habilidade em si -- ver GlassShardsAbility)
 *  ├─ glassShardsDamageI ─ glassShardsDamageII  (dano do estilhaço)
 *  └─ glassShardsSpeedI                          (velocidade do projétil)
 * glassSpray (ver GlassSprayAbility -- leque de estilhaços fracos em área)
 *  └─ glassSprayWideI                             (mais estilhaços, cone mais aberto)
 * glassArmor (ver GlassArmorAbility -- couraça de um uso só que absorve o próximo golpe)
 *  └─ glassArmorShatterI                          (retalia com cacos + Lentidão ao estilhaçar)
 */
public class GlassElement extends Element {

    public static final String NAME = "Glass";

    // ---- nomes dos nós (chave de save / lang / canUseUpgrade) ----
    public static final String GLASS_SHARDS = "glassShards";
    public static final String GLASS_SHARDS_DAMAGE_I = "glassShardsDamageI";
    public static final String GLASS_SHARDS_DAMAGE_II = "glassShardsDamageII";
    public static final String GLASS_SHARDS_SPEED_I = "glassShardsSpeedI";
    public static final String GLASS_SPRAY = "glassSpray";
    public static final String GLASS_SPRAY_WIDE_I = "glassSprayWideI";
    public static final String GLASS_ARMOR = "glassArmor";
    public static final String GLASS_ARMOR_SHATTER_I = "glassArmorShatterI";

    public GlassElement() {
        super(NAME, new Upgrade[]{
                new Upgrade(GLASS_SHARDS, new Upgrade[]{
                        new Upgrade(GLASS_SHARDS_DAMAGE_I, new Upgrade[]{
                                new Upgrade(GLASS_SHARDS_DAMAGE_II, 1)
                        }, 1),
                        new Upgrade(GLASS_SHARDS_SPEED_I, 1)
                }, 0), // grátis -- ver GlassShardsAbility
                new Upgrade(GLASS_SPRAY, new Upgrade[]{
                        new Upgrade(GLASS_SPRAY_WIDE_I, 1)
                }, 2),
                new Upgrade(GLASS_ARMOR, new Upgrade[]{
                        new Upgrade(GLASS_ARMOR_SHATTER_I, 1)
                }, 2)
        });
        addAbility(new GlassShardsAbility(), 0);
        addAbility(new GlassSprayAbility(), 1);
        addAbility(new GlassArmorAbility(), 2);

        // Registro explícito dos slots de bind (mesmo motivo documentado em
        // CrystalElement/LavaElement): sem isso, os 3 ramos poderiam ter
        // getKeybindSlotForUpgrade() colidindo entre si.
        registerUpgradeKeybind(GLASS_SHARDS, 0);
        registerUpgradeKeybind(GLASS_SPRAY, 1);
        registerUpgradeKeybind(GLASS_ARMOR, 2);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new GlassElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    /**
     * @return true se o jogador já tem Sand Bending (basta ter obtido, não
     * precisa ter masterizado a árvore de Sand).
     */
    public static boolean canAcquire(Bender bender) {
        return SandElement.isSandBender(bender);
    }

    public static boolean isGlassBender(Bender bender) {
        return bender.hasElement(get());
    }

    /** Atalho pras abilities, que só têm o ServerPlayer em mãos. */
    public static boolean hasUpgrade(ServerPlayer player, String upgradeName) {
        Bender bender = Bender.getBender(player);
        return bender != null && bender.getData().canUseUpgrade(upgradeName);
    }

    /**
     * Marca o nó raiz "glassShards" (preço 0) como já comprado. Chame logo
     * depois de {@code bender.addElement(GlassElement.get(), true)} no
     * momento da concessão (ver MoreBendingCommand) -- mesmo motivo do
     * {@code GasElement#autoUnlockRoot}: "glassShards" é o único filho
     * direto da raiz sintética do Element e agora TEM filhos (os níveis de
     * dano/velocidade), então sem esse desbloqueio manual a árvore inteira
     * fica travada mesmo com level de sobra.
     */
    public static void autoUnlockRoot(Bender bender) {
        Upgrade shardsNode = get().root.children[0]; // glassShards
        bender.getData().upgrades.put(shardsNode, true);
    }

    /**
     * "Masterizado" = os dois níveis de dano e o nível de velocidade
     * comprados, além da própria habilidade raiz.
     */
    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this)
                && bender.getData().canUseUpgrade(GLASS_SHARDS)
                && bender.getData().canUseUpgrade(GLASS_SHARDS_DAMAGE_II)
                && bender.getData().canUseUpgrade(GLASS_SHARDS_SPEED_I);
    }
}