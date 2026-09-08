package com.elementals.morebendings.bending.watersubbendings.ice;

import dev.saperate.elementals.effects.ElementalsStatusEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Estado de uma única prisão de gelo ativa (um caster prende uma vítima por
 * vez) -- dirigida tick a tick por {@link IcePrisonManager#onServerTick}.
 * Mesmo esquema geral de {@code MudTrapState}, mas com dois diferenciais:
 *
 *  1. A "gaiola" é uma caixa fechada de verdade em vez de só uma coluna --
 *     paredes nos 8 blocos ao redor da vítima (não só os 4 lados
 *     cardeais -- as quinas também são seladas, senão mobs mais largos que
 *     1 bloco escapavam pela diagonal), MAIS piso e teto, formando uma
 *     cela completa de {@link Blocks#PACKED_ICE} (não derrete sozinho,
 *     diferente de {@link Blocks#ICE} comum). A altura da cela acompanha a
 *     altura real da vítima ({@link LivingEntity#getBbHeight()}) em vez de
 *     um valor fixo de 2 blocos -- mobs com 2 blocos de altura ou mais
 *     (zumbi, esqueleto, etc. já têm ~1.95; hostis maiores como Ravager,
 *     Iron Golem, Enderman passam de 2) tinham a cabeça acima do teto e
 *     conseguiam sair andando.
 *
 *  2. Duração presa ao agachar: igual {@code MudTrapState}, libera assim
 *     que o caster solta o agachar, sem mínimo garantido -- enquanto o
 *     caster mantiver o Shift pressionado, a prisão continua de pé até
 *     {@link #MAX_DURATION_TICKS} (failsafe de segurança, pra não travar o
 *     alvo indefinidamente numa queda de conexão silenciosa do caster que
 *     ainda reporte isAlive()).
 *
 * A vítima fica travada com {@code STUNNED} (efeito de verdade do mod base,
 * mesmo usado por {@code CrystalPrisonAbility}) reaplicado a cada tick com
 * duração curta -- assim o stun nunca "vence" sozinho enquanto a prisão
 * segue de pé, e cai imediatamente assim que a gente parar de reaplicar
 * (no {@link #release()}).
 */
public class IcePrisonState {

    /** Trava de segurança: se o caster nunca soltar o agachar, desfaz sozinho depois de 5min. */
    private static final int MAX_DURATION_TICKS = 20 * 60 * 5;
    /** Duração curta reaplicada a cada tick -- suficiente pra cobrir folgas de tick sem "vencer" sozinha. */
    private static final int STUN_REFRESH_TICKS = 10;
    private static final int STUN_AMPLIFIER = 2;

    private final ServerLevel level;
    private final ServerPlayer caster;
    private final LivingEntity victim;

    private final Map<BlockPos, BlockState> savedStates = new LinkedHashMap<>();
    private int ticksElapsed = 0;

    public IcePrisonState(ServerLevel level, ServerPlayer caster, LivingEntity victim) {
        this.level = level;
        this.caster = caster;
        this.victim = victim;
    }

    /** Chamada uma vez, ao criar a prisão -- ergue a cela e aplica o primeiro stun. */
    public void begin() {
        raiseCell();
        victim.addEffect(new MobEffectInstance(ElementalsStatusEffects.STUNNED.get(), STUN_REFRESH_TICKS, STUN_AMPLIFIER));
        level.playSound(null, victim.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 0.9f, 0.8f);
        level.playSound(null, victim.blockPosition(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.6f, 0.9f);
    }

    /** @return true enquanto a prisão deve continuar ativa; false quando deve ser liberada. */
    public boolean tick() {
        ticksElapsed++;

        if (!caster.isAlive() || caster.isRemoved() || !victim.isAlive() || victim.isRemoved()) {
            return false;
        }
        if (ticksElapsed > MAX_DURATION_TICKS) {
            return false;
        }
        if (!caster.isShiftKeyDown()) {
            return false;
        }

        victim.addEffect(new MobEffectInstance(ElementalsStatusEffects.STUNNED.get(), STUN_REFRESH_TICKS, STUN_AMPLIFIER));
        return true;
    }

    /**
     * Ergue a cela: paredes nos 8 blocos ao redor da vítima (cardeais +
     * quinas), com altura igual a {@code cageHeight} (calculada a partir da
     * altura real do alvo), mais piso e teto -- só nas posições
     * substituíveis (ar, grama alta etc.), nunca sobrescrevendo terreno
     * sólido de verdade.
     */
    private void raiseCell() {
        BlockPos feet = victim.blockPosition();
        // Acompanha a altura real do alvo em vez de um valor fixo de 2 --
        // é isso que fazia mobs com 2+ blocos de altura ficarem com a
        // cabeça acima do teto e saírem andando.
        int cageHeight = Math.max(2, Mth.ceil(victim.getBbHeight()));

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue; // coluna central é onde a vítima está
                }
                for (int h = 0; h < cageHeight; h++) {
                    place(feet.offset(dx, h, dz));
                }
            }
        }
        place(feet.below());            // piso
        place(feet.above(cageHeight));  // teto, agora acima da cabeça real do alvo

        if (!savedStates.isEmpty()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                    feet.getX() + 0.5, feet.getY() + 1.0, feet.getZ() + 0.5, 20, 0.35, 0.5, 0.35, 0.0);
        }
    }

    private void place(BlockPos pos) {
        BlockState existing = level.getBlockState(pos);
        if (!existing.canBeReplaced()) {
            return; // já tem algo sólido ali -- já serve de parede/piso/teto sozinho
        }
        savedStates.put(pos.immutable(), existing);
        level.setBlock(pos, Blocks.PACKED_ICE.defaultBlockState(), 3);
    }

    /** Restaura os blocos originais, solta a vítima do stun e libera a prisão. Chamada uma vez, ao final. */
    public void release() {
        for (Map.Entry<BlockPos, BlockState> entry : savedStates.entrySet()) {
            if (level.getBlockState(entry.getKey()).is(Blocks.PACKED_ICE)) {
                level.setBlock(entry.getKey(), entry.getValue(), 3);
            }
        }
        if (victim.isAlive() && !victim.isRemoved()) {
            victim.removeEffect(ElementalsStatusEffects.STUNNED.get());
            level.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                    victim.getX(), victim.getY() + 1.0, victim.getZ(), 16, 0.35, 0.5, 0.35, 0.05);
        }
        level.playSound(null, victim.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.9f, 1.0f);
    }
}