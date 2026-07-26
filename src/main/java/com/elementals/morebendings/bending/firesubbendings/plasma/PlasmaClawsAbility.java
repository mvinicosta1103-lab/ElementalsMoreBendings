package com.elementals.morebendings.bending.firesubbendings.plasma;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.data.PlayerData;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * "plasmaClaws" — nó raiz da árvore de {@link PlasmaElement} e a habilidade
 * em si (mesmo esquema de {@code PetrifyingTouchAbility}: golpe instantâneo
 * de curto alcance via raycast, sem canalização, disparado pelo cast nativo
 * do mod base -- por isso funciona de graça com a troca de elementos (Cycle
 * Elements) do mod base: basta o jogador ciclar até Plasma estar selecionado
 * e apertar a tecla de cast, sem precisar de nenhum keybind extra deste
 * addon, igual a Pressure Point/Petrifying Touch).
 *
 * Conceito: o bender super-aquece o ar em volta das duas mãos até virar
 * plasma, formando garras de energia azul. O golpe em si é sempre uma
 * "garra" (as mãos nuas já cortam), mas se o jogador estiver com uma
 * ferramenta/arma (qualquer {@link TieredItem} -- espada, machado, picareta,
 * pá ou enxada) na mão principal, o plasma se EMBUTE nela: a ferramenta vira
 * um condutor da energia, ganhando um bônus de dano proporcional ao material
 * dela (via {@link Tier#getAttackDamageBonus()}) e uma queimadura mínima
 * garantida, além de partículas/som diferentes -- o golpe "puro" com as mãos
 * não tem esse extra.
 *
 * Dano escala com três fontes independentes, todas somadas na mesma
 * chamada de {@link LivingEntity#hurt}:
 *  1) especialização escolhida (plasmaSpecialization é exclusive=true, só
 *     uma das três pode estar comprada por vez):
 *     - plasmaSear (+ Damage I/II): puro dano cumulativo, sem queimar por
 *       conta própria.
 *     - plasmaFlare (+ Duration I): acende o alvo em fogo normal.
 *     - plasmaSuperheat: capstone -- sempre acende com uma queimada forte
 *       e ainda espalha uma rajada de calor pros vizinhos do alvo.
 *  2) bônus de embutimento (ferramenta na mão, ver acima) -- independente
 *     da especialização.
 *  3) sinergia com Blue Fire: se o jogador tem o nó "blueFire" da árvore
 *     BASE de Fire comprado (um dos caminhos possíveis pra masterizar Fire,
 *     ver {@code FireMasteryCheck}), o golpe ganha dano extra e queima com
 *     partículas de Soul Fire em vez de fogo normal -- "soma com o blue
 *     fire" literal: um bônus a mais empilhado em cima de qualquer
 *     especialização/embutimento que o jogador já tenha.
 *
 * Toda duração de queimada de fontes diferentes usa o MAIOR valor entre
 * elas (não soma ticks) pra não virar um infinito absurdo quando o jogador
 * tem Flare + embutimento + Blue Fire ao mesmo tempo -- só o dano soma, a
 * queimada usa a fonte mais forte.
 *
 * plasmaReachI/II aumentam o alcance do raycast; plasmaHeatI/II reduzem o
 * cooldown -- crescimento igual ao esquema gasCloudSizeI/II e
 * gasVentI/II de {@code GasCloudAbility}.
 */
public class PlasmaClawsAbility implements Ability {

    private static final double BASE_REACH = 3.0;
    private static final double REACH_BONUS_PER_LEVEL = 0.75; // reachI / reachII

    private static final int BASE_COOLDOWN_TICKS = 24; // 1.2s
    private static final int COOLDOWN_REDUCTION_PER_LEVEL = 6; // heatI / heatII
    private static final int MIN_COOLDOWN_TICKS = 12; // 0.6s

    private static final float CAST_CHI_COST = 8.0f;

    private static final float BASE_DAMAGE = 4.0f;

    // -- plasmaSear (dano puro, cumulativo) --
    private static final float SEAR_BASE_BONUS = 1.5f;
    private static final float SEAR_DAMAGE_I_BONUS = 1.5f;
    private static final float SEAR_DAMAGE_II_BONUS = 2.0f;

    // -- plasmaFlare (queimada) --
    private static final int FLARE_BASE_FIRE_TICKS = 60;      // 3s
    private static final int FLARE_DURATION_I_BONUS_TICKS = 40; // +2s

    // -- plasmaSuperheat (capstone) --
    private static final float SUPERHEAT_BONUS_DAMAGE = 3.0f;
    private static final int SUPERHEAT_FIRE_TICKS = 140; // 7s
    private static final double SUPERHEAT_SPLASH_RADIUS = 1.75;
    private static final float SUPERHEAT_SPLASH_DAMAGE_FACTOR = 0.5f;
    private static final int SUPERHEAT_SPLASH_FIRE_TICKS = 40; // 2s

    // -- embutimento (ferramenta na mão) --
    private static final float TOOL_EMBED_DAMAGE_FACTOR = 0.75f;
    private static final int TOOL_EMBED_MIN_FIRE_TICKS = 20; // 1s garantido

    // -- sinergia Blue Fire (nó base do FireElement, fora deste addon) --
    private static final String BLUE_FIRE_UPGRADE = "blueFire";
    private static final float BLUE_FIRE_BONUS_DAMAGE = 2.0f;
    private static final int BLUE_FIRE_FIRE_TICKS = 100; // 5s

    private static final double KNOCKBACK_STRENGTH = 0.35;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        long now = level.getGameTime();
        int cooldown = getCooldownTicks(caster);
        if (now - PlasmaCooldown.getLastUse(caster) < cooldown) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }
        PlasmaCooldown.setLastUse(caster, now);

        double reach = getReach(caster);
        ItemStack mainHand = caster.getMainHandItem();
        boolean embedded = mainHand.getItem() instanceof TieredItem;

        EntityHitResult hit = SapsUtils.raycastEntity(player, reach,
                entity -> entity instanceof LivingEntity && entity != player);

        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            strike(caster, level, target, embedded, mainHand);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5f, 1.6f);
        }

        spawnHandParticles(level, caster, embedded);

        bender.setCurrAbility(null); // instantânea -- não canaliza
    }

    private void strike(ServerPlayer caster, ServerLevel level, LivingEntity target,
                        boolean embedded, ItemStack mainHand) {
        PlayerData data = PlayerData.get(caster);

        float damage = BASE_DAMAGE;
        int fireTicks = 0;
        boolean useBlueParticles = false;

        // ---- especialização (exclusive: só um destes ramos existe comprado) ----
        if (data.canUseUpgrade(PlasmaElement.PLASMA_SEAR)) {
            damage += SEAR_BASE_BONUS;
            if (data.canUseUpgrade(PlasmaElement.PLASMA_SEAR_DAMAGE_I)) damage += SEAR_DAMAGE_I_BONUS;
            if (data.canUseUpgrade(PlasmaElement.PLASMA_SEAR_DAMAGE_II)) damage += SEAR_DAMAGE_II_BONUS;
        } else if (data.canUseUpgrade(PlasmaElement.PLASMA_FLARE)) {
            int flareTicks = FLARE_BASE_FIRE_TICKS;
            if (data.canUseUpgrade(PlasmaElement.PLASMA_FLARE_DURATION_I)) flareTicks += FLARE_DURATION_I_BONUS_TICKS;
            fireTicks = Math.max(fireTicks, flareTicks);
        } else if (data.canUseUpgrade(PlasmaElement.PLASMA_SUPERHEAT)) {
            damage += SUPERHEAT_BONUS_DAMAGE;
            fireTicks = Math.max(fireTicks, SUPERHEAT_FIRE_TICKS);
        }

        // ---- embutimento: ferramenta/arma na mão conduz o plasma ----
        if (embedded) {
            Tier tier = ((TieredItem) mainHand.getItem()).getTier();
            damage += tier.getAttackDamageBonus() * TOOL_EMBED_DAMAGE_FACTOR;
            fireTicks = Math.max(fireTicks, TOOL_EMBED_MIN_FIRE_TICKS);
        }

        // ---- sinergia com Blue Fire (nó base da árvore de Fire) ----
        boolean blueFire = data.canUseUpgrade(BLUE_FIRE_UPGRADE);
        if (blueFire) {
            damage += BLUE_FIRE_BONUS_DAMAGE;
            fireTicks = Math.max(fireTicks, BLUE_FIRE_FIRE_TICKS);
            useBlueParticles = true;
        }

        applyHit(level, caster, target, damage, fireTicks, useBlueParticles, embedded);

        // Superheat também espalha uma rajada menor pra quem estiver perto do alvo.
        if (data.canUseUpgrade(PlasmaElement.PLASMA_SUPERHEAT)) {
            double x = target.getX(), y = target.getY() + target.getBbHeight() * 0.5, z = target.getZ();
            for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(SUPERHEAT_SPLASH_RADIUS),
                    e -> e != caster && e != target && e.isAlive())) {
                float splashDamage = damage * SUPERHEAT_SPLASH_DAMAGE_FACTOR;
                applyHit(level, caster, nearby, splashDamage, SUPERHEAT_SPLASH_FIRE_TICKS, useBlueParticles, false);
            }
            level.sendParticles(useBlueParticles ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
                    x, y, z, 20, SUPERHEAT_SPLASH_RADIUS * 0.5, 0.3, SUPERHEAT_SPLASH_RADIUS * 0.5, 0.02);
        }
    }

    private void applyHit(ServerLevel level, ServerPlayer caster, LivingEntity target, float damage,
                          int fireTicks, boolean blueFire, boolean embedded) {
        target.hurt(level.damageSources().playerAttack(caster), damage);

        if (fireTicks > 0) {
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), fireTicks));
        }

        Vec3 push = target.position().subtract(caster.position()).normalize().scale(KNOCKBACK_STRENGTH);
        target.push(push.x, 0.15, push.z);

        double x = target.getX(), y = target.getY() + target.getBbHeight() * 0.5, z = target.getZ();
        level.sendParticles(blueFire ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.ELECTRIC_SPARK,
                x, y, z, 14, 0.35, 0.35, 0.35, 0.05);
        if (embedded) {
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 6, 0.25, 0.25, 0.25, 0.02);
        }

        level.playSound(null, x, y, z, embedded ? SoundEvents.ANVIL_USE : SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 0.9f, blueFire ? 1.4f : 1.0f);
        if (fireTicks > 0) {
            level.playSound(null, x, y, z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5f, 1.6f);
        }
    }

    /** Partículas cosméticas nas duas mãos, mesmo sem acertar nada -- é o que
     * vende a ideia de "garras" permanentes de plasma, não só um efeito de
     * impacto. */
    private void spawnHandParticles(ServerLevel level, ServerPlayer caster, boolean embedded) {
        Vec3 look = caster.getLookAngle();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 side = look.cross(up).normalize().scale(0.4);
        Vec3 origin = caster.position().add(0, caster.getEyeHeight() * 0.6, 0).add(look.scale(0.6));

        Vec3 leftHand = origin.subtract(side);
        Vec3 rightHand = origin.add(side);

        for (Vec3 hand : new Vec3[]{leftHand, rightHand}) {
            level.sendParticles(embedded ? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK,
                    hand.x, hand.y, hand.z, 4, 0.08, 0.08, 0.08, 0.01);
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }

    public static double getReach(ServerPlayer player) {
        double reach = BASE_REACH;
        if (PlasmaElement.hasUpgrade(player, PlasmaElement.PLASMA_REACH_I)) reach += REACH_BONUS_PER_LEVEL;
        if (PlasmaElement.hasUpgrade(player, PlasmaElement.PLASMA_REACH_II)) reach += REACH_BONUS_PER_LEVEL;
        return reach;
    }

    public static int getCooldownTicks(ServerPlayer player) {
        int cooldown = BASE_COOLDOWN_TICKS;
        if (PlasmaElement.hasUpgrade(player, PlasmaElement.PLASMA_HEAT_I)) cooldown -= COOLDOWN_REDUCTION_PER_LEVEL;
        if (PlasmaElement.hasUpgrade(player, PlasmaElement.PLASMA_HEAT_II)) cooldown -= COOLDOWN_REDUCTION_PER_LEVEL;
        return Math.max(cooldown, MIN_COOLDOWN_TICKS);
    }
}