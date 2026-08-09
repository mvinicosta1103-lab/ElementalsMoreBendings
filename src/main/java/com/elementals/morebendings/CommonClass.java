package com.elementals.morebendings;

import com.elementals.morebendings.bending.earthsubbendings.bone.BoneElement;
import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalElement;
import com.elementals.morebendings.bending.earthsubbendings.glass.GlassElement;
import com.elementals.morebendings.bending.earthsubbendings.lava.LavaElement;
import com.elementals.morebendings.bending.earthsubbendings.metal.MetalMasteryGraft;
import com.elementals.morebendings.bending.earthsubbendings.mud.MudElement;
import com.elementals.morebendings.bending.earthsubbendings.petrification.PetrificationElement;
import com.elementals.morebendings.bending.earthsubbendings.sand.SandElement;
import com.elementals.morebendings.bending.airsubbendings.atmosphere.AtmosphereElement;
import com.elementals.morebendings.bending.airsubbendings.gas.GasElement;
import com.elementals.morebendings.bending.airsubbendings.mist.MistElement;
import com.elementals.morebendings.bending.airsubbendings.temperature.TemperatureElement;
import com.elementals.morebendings.bending.airsubbendings.voiding.VoidElement;
import com.elementals.morebendings.bending.firesubbendings.combustion.CombustionElement;
import com.elementals.morebendings.bending.firesubbendings.lightning.LightningMasteryGraft;
import com.elementals.morebendings.bending.firesubbendings.plasma.PlasmaElement;
import com.elementals.morebendings.bending.watersubbendings.blood.BloodMasteryGraft;
import com.elementals.morebendings.bending.watersubbendings.plant.PlantElement;
import com.elementals.morebendings.bending.watersubbendings.spirit.SpiritElement;
import com.elementals.morebendings.bending.airsubbendings.sound.SoundElement;
import com.elementals.morebendings.bending.avatarstate.AvatarElement;
import com.elementals.morebendings.bending.avatarstate.moves.EnergyElement;
import com.elementals.morebendings.registry.ModAbilities;


public class CommonClass {

    public static void init() {
        Constants.LOG.info("Inicializando sub-bendings comuns (Plant, Spirit, Mud, Crystal, Sand, Glass, Petrification, Lava, Bone, Atmosphere, Gas, Mist, Sound, Temperature, Void, Plasma) na 1.21.1...");

        // WATER SUBBENDINGS
        PlantElement.register();
        SpiritElement.register();
        // Blood Mastery não é mais um Element separado -- suas 8 habilidades
        // são enxertadas direto na árvore de skills do Blood base (mesmo
        // esquema de MetalMasteryGraft/LightningMasteryGraft).
        BloodMasteryGraft.graft();

        // EARTH SUBBENDINGS
        MudElement.register();
        CrystalElement.register();
        SandElement.register();
        GlassElement.register();
        PetrificationElement.register();
        LavaElement.register();
        BoneElement.register();
        // Metal Mastery não é mais um Element separado -- suas 3 habilidades
        // são enxertadas direto na árvore de skills do Metal base.
        MetalMasteryGraft.graft();

        // AIR SUBBENDINGS
        AtmosphereElement.register();
        GasElement.register();
        MistElement.register();
        SoundElement.register();
        TemperatureElement.register();
        VoidElement.register();

        // FIRE SUBBENDINGS
        PlasmaElement.register();
        CombustionElement.register();
        // Lightning Subbending não é mais um Element separado -- suas 7
        // habilidades são enxertadas direto na árvore de skills do
        // Lightning base (mesmo esquema de MetalMasteryGraft).
        LightningMasteryGraft.graft();

        // AVATAR STATE -- Element/Energy só existem enquanto o Avatar State
        // está ativo (ver AvatarStateManager#activate/deactivate). Registro
        // aqui igual qualquer outro Element -- quem controla QUANDO o
        // jogador de fato tem acesso a eles é o AvatarStateManager, não o
        // sistema normal de aquisição.
        AvatarElement.register();
        EnergyElement.register();

        // Flying ainda usa o sistema antigo (PlayerSubbendingData) --
        // ainda não convertida pro mesmo padrão de Element real.
        ModAbilities.registerAbilities();
    }
}