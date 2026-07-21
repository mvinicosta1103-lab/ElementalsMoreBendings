package com.example.elementalmorebendings.mud.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.elements.earth.EarthElement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * MudShellAbility ("mudShell")
 * Ramo 1 (Defesa) — habilidade bônus anexada dentro do ramo "mudWall": o
 * jogador ergue um abrigo/cúpula de lama endurecida ao redor de si (e de
 * quem mais estiver por perto — a cúpula se adapta ao grupo, ficando maior
 * quanto mais gente próxima ela precisa cobrir). Ela sobe camada por
 * camada, do chão pra cima, como uma construção de verdade, fica de pé por
 * um tempo e depois afunda/soma do mesmo jeito. Quem está dentro também
 * ganha Resistência a dano enquanto a cúpula existe — "mudShellHardenedI"
 * deixa a casca mais "endurecida" (Resistência II em vez de I) e
 * "mudShellDurationI" estende quanto tempo ela dura de pé.
 */
public class MudShellAbility implements Ability {

    private static final float BASE_COST = 22.0f;
    private static final int BASE_DURATION = 100; // 5s de Resistência
    private static final int HOLD_BASE_TICKS = 140; // ~7s de pé
    private static final int HOLD_BONUS_DURATION_UPGRADE = 100;
    private static final int TICKS_PER_LAYER = 2;

    private static final double GROUP_SEARCH_RADIUS = 8.0;
    private static final int MIN_RADIUS = 3;
    private static final int MAX_RADIUS = 7;
    private static final int RADIUS_PADDING = 2;
    private static final double SHELL_THICKNESS = 0.6;
    private static final double DOOR_HALF_ANGLE_COS = 0.82; // ~35° de largura de porta

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer caster)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!AbilitySupport.spendUnlocked(bender, "mudShell", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        ServerLevel world = caster.serverLevel();

        // 1) Descobre quem está por perto pra saber o tamanho do abrigo.
        List<ServerPlayer> nearby = world.getEntitiesOfClass(ServerPlayer.class,
                caster.getBoundingBox().inflate(GROUP_SEARCH_RADIUS));
        if (nearby.isEmpty() || !nearby.contains(caster)) {
            nearby.add(caster);
        }

        double cx = 0.0;
        double cz = 0.0;
        double minY = Double.MAX_VALUE;
        for (ServerPlayer p : nearby) {
            cx += p.getX();
            cz += p.getZ();
            minY = Math.min(minY, p.getY());
        }
        cx /= nearby.size();
        cz /= nearby.size();
        int baseY = (int) Math.floor(minY);

        double maxDist = 0.0;
        for (ServerPlayer p : nearby) {
            double dx = p.getX() - cx;
            double dz = p.getZ() - cz;
            maxDist = Math.max(maxDist, Math.sqrt(dx * dx + dz * dz));
        }
        int radius = (int) Math.ceil(maxDist) + RADIUS_PADDING;
        radius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius));

        // 2) Posições ocupadas por jogadores (não coloca bloco em cima de ninguém).
        Set<BlockPos> occupied = new HashSet<>();
        for (ServerPlayer p : nearby) {
            occupied.add(p.blockPosition());
            occupied.add(p.blockPosition().above());
        }

        // 3) Direção da "porta" — na direção pra onde o conjurador está olhando.
        Vec3 doorDir = caster.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (doorDir.lengthSqr() < 0.01) {
            doorDir = new Vec3(0.0, 0.0, 1.0);
        }
        doorDir = doorDir.normalize();

        // 4) Monta a casca da cúpula (hemisfério oco), camada por camada (y crescente).
        TreeMap<Integer, List<BlockPos>> layerMap = new TreeMap<>();
        Map<BlockPos, BlockState> originalStates = new LinkedHashMap<>();
        planDome(world, caster, bender, cx, cz, baseY, radius, doorDir, occupied, layerMap, originalStates);

        if (originalStates.isEmpty()) {
            bender.setCurrAbility(null);
            return;
        }

        List<List<BlockPos>> layers = new ArrayList<>(layerMap.values());

        Object previous = bender.getBackgroundAbilityData(this);
        if (previous instanceof MudStructureAnimator previousAnimator) {
            previousAnimator.cancelAndRestoreAll();
        }

        int amplifier = bender.plrData.canUseUpgrade("mudShellHardenedI") ? 1 : 0;
        int resistanceDuration = bender.plrData.canUseUpgrade("mudShellDurationI")
                ? BASE_DURATION + HOLD_BONUS_DURATION_UPGRADE : BASE_DURATION;
        int holdDuration = bender.plrData.canUseUpgrade("mudShellDurationI")
                ? HOLD_BASE_TICKS + HOLD_BONUS_DURATION_UPGRADE : HOLD_BASE_TICKS;

        // Todo mundo abrigado ganha a Resistência, não só quem conjurou.
        for (ServerPlayer p : nearby) {
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, resistanceDuration, amplifier));
        }

        MudStructureAnimator animator = new MudStructureAnimator(world, layers, originalStates,
                Blocks.MUD.defaultBlockState(), TICKS_PER_LAYER, holdDuration);
        bender.addBackgroundAbility(this, animator);

        bender.setCurrAbility(null);
    }

    /**
     * Calcula as posições da casca de uma cúpula (hemisfério oco) centrada em
     * (cx, baseY, cz), agrupadas por altura (y) pra animação subir camada por
     * camada. Deixa uma "porta" aberta na direção que o conjurador estava
     * olhando, e nunca coloca bloco em cima de um jogador.
     */
    private static void planDome(ServerLevel world, ServerPlayer caster, Bender bender,
                                 double cx, double cz, int baseY, int radius, Vec3 doorDir,
                                 Set<BlockPos> occupied, TreeMap<Integer, List<BlockPos>> layerMap,
                                 Map<BlockPos, BlockState> originalStates) {
        if (!AbilitySupport.canDamageBlocks(world)) {
            return;
        }

        int minX = (int) Math.floor(cx - radius);
        int maxX = (int) Math.ceil(cx + radius);
        int minZ = (int) Math.floor(cz - radius);
        int maxZ = (int) Math.ceil(cz + radius);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double dx = (x + 0.5) - cx;
                double dz = (z + 0.5) - cz;
                double horizDist = Math.sqrt(dx * dx + dz * dz);
                if (horizDist > radius + SHELL_THICKNESS) {
                    continue;
                }

                // Só deixa a "porta" nos dois blocos de baixo (altura de passagem).
                boolean inDoorDirection = false;
                if (horizDist > 0.001) {
                    double dot = (dx / horizDist) * doorDir.x + (dz / horizDist) * doorDir.z;
                    inDoorDirection = dot >= DOOR_HALF_ANGLE_COS;
                }

                for (int dy = 0; dy <= radius; dy++) {
                    double vy = dy;
                    double dist3 = Math.sqrt(dx * dx + vy * vy + dz * dz);
                    if (Math.abs(dist3 - radius) > SHELL_THICKNESS) {
                        continue; // só a casca, não a cúpula inteira preenchida
                    }

                    if (inDoorDirection && dy <= 1) {
                        continue; // vão da porta
                    }

                    BlockPos pos = new BlockPos(x, baseY + dy, z);
                    if (occupied.contains(pos)) {
                        continue;
                    }

                    BlockState current = world.getBlockState(pos);
                    if (!current.isAir() && !current.getFluidState().isEmpty()) {
                        continue;
                    }
                    if (!EarthElement.isBlockBendable(current, bender) && !current.isAir()) {
                        continue;
                    }
                    if (!caster.mayInteract((Level) world, pos)) {
                        continue;
                    }

                    originalStates.put(pos, current);
                    layerMap.computeIfAbsent(dy, k -> new ArrayList<>()).add(pos);
                }
            }
        }
    }

    public void onBackgroundTick(Bender bender, Object data) {
        if (!(data instanceof MudStructureAnimator animator)) {
            bender.removeAbilityFromBackground(this);
            return;
        }
        if (animator.tick()) {
            bender.removeAbilityFromBackground(this);
        }
    }

    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}