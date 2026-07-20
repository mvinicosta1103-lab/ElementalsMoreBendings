package com.example.elementalmorebendings.plant.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * VineArcAbility ("vineArc")
 * <p>
 * Versão em arco do Vine Whip original. Em vez de mandar as partículas em
 * linha reta (o que no Vine Whip normal quase não dá pra perceber), essa
 * habilidade desenha um ARCO curvo de vinhas — o mesmo "modelo" de traço
 * usado pela Water Arc do Elementals base (uma curva, não uma linha),
 * só que verde e com partículas de folha/vinha em vez de água.
 * <p>
 * Nota: a Water Arc do mod base (Elementals) é fechada/closed-source
 * (distribuída só como .jar, sem repositório público), então não dá pra
 * reaproveitar a implementação dela diretamente — essa habilidade recria o
 * MESMO CONCEITO visual (traço em arco, mais vistoso que uma linha reta)
 * do zero, usando só partículas vanilla.
 */
public class VineArcAbility implements Ability {

    private static final float BASE_COST = 14.0f;

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!AbilitySupport.spendUnlocked(bender, "vineArc", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        double range = bender.plrData.canUseUpgrade("vineArcRangeI") ? 13.0 : 9.0;
        float damage = bender.plrData.canUseUpgrade("vineArcPowerI") ? 6.0f : 4.0f;

        ServerLevel world = player.serverLevel();
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(range));

        // vetor lateral (perpendicular ao olhar, no plano horizontal) usado
        // pra dar a "barriga" do arco — alterna de lado a cada uso, pra não
        // ficar sempre curvando pro mesmo jeito.
        Vec3 sideways = new Vec3(-look.z, 0, look.x).normalize();
        if ((deltaT & 1) == 1) sideways = sideways.scale(-1);

        double bulge = 1.6 + range * 0.12;
        AbilitySupport.spawnArc(world, start, end, sideways, bulge, 48);

        // núcleo brilhante do arco (partícula "spore" verde, mais rala, dá
        // profundidade extra ao traço sem poluir a curva principal)
        for (int i = 0; i <= 48; i += 3) {
            double t = i / 48.0;
            double b = 4.0 * bulge * t * (1.0 - t);
            Vec3 p = start.add(end.subtract(start).scale(t)).add(sideways.scale(b));
            world.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
        }

        List<LivingEntity> targets = AbilitySupport.entitiesInFront(player, range, 1.4);
        for (LivingEntity target : targets) {
            target.hurt(world.damageSources().playerAttack(player), damage);
            AbilitySupport.pushAway(start, target, 0.35, 0.25);
            world.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    target.getX(), target.getY(0.6), target.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        }

        bender.setCurrAbility(null);
    }

    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}
