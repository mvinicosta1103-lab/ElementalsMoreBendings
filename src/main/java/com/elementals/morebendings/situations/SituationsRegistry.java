package com.elementals.morebendings.situations;

import com.elementals.morebendings.bending.airsubbendings.atmosphere.AtmosphereElement;
import com.elementals.morebendings.bending.airsubbendings.gas.GasElement;
import com.elementals.morebendings.bending.airsubbendings.mist.MistElement;
import com.elementals.morebendings.bending.airsubbendings.sound.SoundElement;
import com.elementals.morebendings.bending.airsubbendings.temperature.TemperatureElement;
import com.elementals.morebendings.bending.airsubbendings.voiding.VoidElement;
import com.elementals.morebendings.bending.earthsubbendings.bone.BoneElement;
import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalElement;
import com.elementals.morebendings.bending.earthsubbendings.glass.GlassElement;
import com.elementals.morebendings.bending.earthsubbendings.lava.LavaElement;
import com.elementals.morebendings.bending.earthsubbendings.mud.MudElement;
import com.elementals.morebendings.bending.earthsubbendings.petrification.PetrificationElement;
import com.elementals.morebendings.bending.earthsubbendings.sand.SandElement;
import com.elementals.morebendings.bending.firesubbendings.combustion.CombustionElement;
import com.elementals.morebendings.bending.firesubbendings.plasma.PlasmaElement;
import com.elementals.morebendings.bending.watersubbendings.plant.PlantElement;
import com.elementals.morebendings.bending.watersubbendings.spirit.SpiritElement;
import com.elementals.morebendings.data.SubbendingType;
import dev.saperate.elementals.elements.air.AirElement;
import dev.saperate.elementals.elements.earth.EarthElement;
import dev.saperate.elementals.elements.fire.FireElement;
import dev.saperate.elementals.elements.water.WaterElement;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Uma situação plausível por sub-bending -- ver o javadoc de
 * {@link SituationDefinition} pro que cada campo significa e
 * {@link SituationsSystem} pra como isso é consumido.
 * <br><br>
 * IMPORTANTE (decisão de design, ver conversa que originou este sistema):
 * diferente do scroll/{@code canAcquire}, aqui a mastery completa da
 * árvore-mãe NÃO é exigida -- só ter a bending base (Earth/Air/Fire/Water)
 * e estar na situação certa. Pré-requisitos "extra" que scrolls específicos
 * tinham (Glass exigir Sand Bending, Bone exigir ter cruzado com um Blood
 * bender) também foram trocados por uma situação ambiental própria em vez
 * de continuar exigindo a outra sub-bending/evento histórico -- por
 * enquanto o scroll continua sendo o único caminho que respeita esses
 * requisitos "de verdade", esse sistema é propositalmente mais solto.
 * <br><br>
 * Raios, contagens e chances abaixo são um primeiro chute -- valem
 * playtesting e ajuste fino depois de ver com que frequência cada uma
 * dispara na prática.
 */
public final class SituationsRegistry {

    private static final double DEFAULT_CHANCE = 0.02; // 2% por checagem (ver CHECK_INTERVAL_TICKS)
    private static final double RARE_COMBO_CHANCE = 0.05; // situações que já dependem de algo raro (tempestade, etc)

    private SituationsRegistry() {
    }

    public static final List<SituationDefinition> ALL = List.of(

            // ---- Earth ----
            new SituationDefinition(SubbendingType.LAVA, EarthElement::get, LavaElement::get,
                    player -> SituationChecks.isUnderground(player)
                            && SituationChecks.countNearbyLava(player, 6) >= 6,
                    DEFAULT_CHANCE,
                    "The heat radiating off the surrounding lava seeps into your senses -- you've learned Lava Bending!"),

            new SituationDefinition(SubbendingType.MUD, EarthElement::get, MudElement::get,
                    player -> SituationChecks.countNearbyBlocks(player, 6, SituationChecks.MUD_BLOCKS) >= 10,
                    DEFAULT_CHANCE,
                    "Sinking your hands into the thick mud, something clicks -- you've learned Mud Bending!"),

            new SituationDefinition(SubbendingType.CRYSTAL, EarthElement::get, CrystalElement::get,
                    player -> SituationChecks.countNearbyBlocks(player, 6, SituationChecks.AMETHYST_BLOCKS) >= 3,
                    DEFAULT_CHANCE,
                    "The resonance of the amethyst around you hums in tune with your bending -- you've learned Crystal Bending!"),

            new SituationDefinition(SubbendingType.SAND, EarthElement::get, SandElement::get,
                    player -> SituationChecks.canSeeSky(player)
                            && SituationChecks.countNearbyBlocks(player, 6, SituationChecks.SAND_BLOCKS) >= 60,
                    DEFAULT_CHANCE,
                    "The dunes shift under your feet as if inviting you -- you've learned Sand Bending!"),

            new SituationDefinition(SubbendingType.GLASS, EarthElement::get, GlassElement::get,
                    player -> player.level().isThundering()
                            && SituationChecks.countNearbyBlocks(player, 6, SituationChecks.SAND_BLOCKS) >= 30,
                    RARE_COMBO_CHANCE,
                    "Lightning fuses the sand around you into glittering glass -- you've learned Glass Bending!"),

            new SituationDefinition(SubbendingType.PETRIFICATION, EarthElement::get, PetrificationElement::get,
                    player -> SituationChecks.isUnderground(player)
                            && SituationChecks.countNearbyBlocks(player, 6, SituationChecks.DRIPSTONE_BLOCKS) >= 5,
                    DEFAULT_CHANCE,
                    "Standing among ancient dripstone, you feel stone creeping into your bones -- you've learned Petrification Bending!"),

            new SituationDefinition(SubbendingType.BONE, EarthElement::get, BoneElement::get,
                    player -> SituationChecks.countNearbyBlocks(player, 6, Blocks.BONE_BLOCK) >= 4,
                    DEFAULT_CHANCE,
                    "Surrounded by old bones, you sense a grim new control over them -- you've learned Bone Bending!",
                    BoneElement::autoUnlockRoot),

            // ---- Air ----
            new SituationDefinition(SubbendingType.ATMOSPHERE, AirElement::get, AtmosphereElement::get,
                    player -> SituationChecks.canSeeSky(player) && player.getY() >= 200,
                    DEFAULT_CHANCE,
                    "At the top of the world, the thin air bends easily to your will -- you've learned Atmosphere Bending!",
                    AtmosphereElement::autoUnlockRoots),

            new SituationDefinition(SubbendingType.GAS, AirElement::get, GasElement::get,
                    player -> SituationChecks.isUnderground(player)
                            && SituationChecks.countNearbyBlocks(player, 6, SituationChecks.MUSHROOM_BLOCKS) >= 6,
                    DEFAULT_CHANCE,
                    "Spores drift thick in the cave air around you -- you've learned Gas Bending!",
                    GasElement::autoUnlockRoot),

            new SituationDefinition(SubbendingType.MIST, AirElement::get, MistElement::get,
                    player -> SituationChecks.isPrecipitatingOn(player)
                            && SituationChecks.countNearbyWater(player, 6) >= 6,
                    RARE_COMBO_CHANCE,
                    "Rain meets water and wraps you in a soft fog -- you've learned Mist Bending!",
                    MistElement::autoUnlockRoot),

            new SituationDefinition(SubbendingType.SOUND, AirElement::get, SoundElement::get,
                    player -> SituationChecks.isUnderground(player) && player.getY() <= -20,
                    DEFAULT_CHANCE,
                    "Deep underground, every echo answers back louder than it should -- you've learned Sound Bending!"),

            new SituationDefinition(SubbendingType.TEMPERATURE, AirElement::get, TemperatureElement::get,
                    player -> SituationChecks.countNearbyBlocks(player, 6, SituationChecks.HOT_BLOCKS) >= 1
                            && SituationChecks.countNearbyBlocks(player, 6, SituationChecks.COLD_BLOCKS) >= 1,
                    RARE_COMBO_CHANCE,
                    "Standing between searing heat and biting cold, you learn to command both -- you've learned Temperature Bending!"),

            new SituationDefinition(SubbendingType.VOID, AirElement::get, VoidElement::get,
                    player -> SituationChecks.canSeeSky(player) && player.getY() >= 150
                            && SituationChecks.hasEmptyDropBelow(player, 20),
                    RARE_COMBO_CHANCE,
                    "Perched over an empty drop with nothing but sky around you, you touch the void -- you've learned Void Bending!"),

            // ---- Fire ----
            new SituationDefinition(SubbendingType.PLASMA, FireElement::get, PlasmaElement::get,
                    player -> SituationChecks.canSeeSky(player) && player.level().isThundering()
                            && SituationChecks.countNearbyBlocks(player, 6, SituationChecks.HOT_BLOCKS) >= 1,
                    RARE_COMBO_CHANCE,
                    "Lightning arcs near the flames around you, and something ignites within -- you've learned Plasma Bending!",
                    PlasmaElement::autoUnlockRoot),

            new SituationDefinition(SubbendingType.COMBUSTION, FireElement::get, CombustionElement::get,
                    player -> player.level().dimension().equals(Level.NETHER)
                            && (SituationChecks.countNearbyLava(player, 6) >= 4
                            || SituationChecks.countNearbyBlocks(player, 6, SituationChecks.HOT_BLOCKS) >= 2),
                    DEFAULT_CHANCE,
                    "The Nether's violent heat teaches you to make fire explode on command -- you've learned Combustion Bending!",
                    CombustionElement::autoUnlockRoot),

            // ---- Water ----
            new SituationDefinition(SubbendingType.PLANT, WaterElement::get, PlantElement::get,
                    player -> SituationChecks.canSeeSky(player)
                            && SituationChecks.countNearbyBlocks(player, 8, SituationChecks.LEAF_BLOCKS) >= 40,
                    DEFAULT_CHANCE,
                    "Deep in the forest, the plants seem to lean toward you -- you've learned Plant Bending!"),

            new SituationDefinition(SubbendingType.SPIRIT, WaterElement::get, SpiritElement::get,
                    player -> SituationChecks.countNearbyBlocks(player, 6, SituationChecks.SOUL_BLOCKS) >= 5,
                    DEFAULT_CHANCE,
                    "Something otherworldly stirs among the soul-touched ground -- you've learned Spirit Bending!")
    );
}
