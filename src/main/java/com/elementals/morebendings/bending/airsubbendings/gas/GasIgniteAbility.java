package com.elementals.morebendings.bending.airsubbendings.gas;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ExplosionDamageCalculator;

/**
 * Ramo de especialização "gasIgnite" (ver {@link GasSkillTree}) — a opção de
 * maior risco/recompensa: ignita a própria nuvem de gás, causando uma
 * explosão instantânea centrada no caster. Não quebra blocos de propósito
 * (gás não é dinamite) — só dano de área, incluindo no próprio caster se ele
 * não sair de perto a tempo.
 *
 * É um nó terminal (sem upgrades de melhoria ainda) e o mais caro dos três
 * (3 pontos), justamente por não precisar de investimento extra pra ser forte.
 *
 * NOTA: a assinatura exata de {@code Level#explode} varia entre versões do
 * NeoForge/Minecraft 1.21.x (parâmetro de "block interaction" mudou de
 * enum pra outra coisa em algumas builds) — conferir contra a versão real
 * usada no projeto ao compilar; deixei um fallback comentado.
 */
public class GasIgniteAbility {

    private static final float EXPLOSION_POWER = 2.5f;

    public static void register() {
        // Acionada a partir de GasCloudAbility.execute(), igual as outras especializações.
    }

    public static void applyOnCloud(ServerPlayer caster, ServerLevel level, double radius) {
        if (!GasElement.hasUpgrade(caster, GasSkillTree.GAS_IGNITE)) {
            return;
        }
        // Explosão que causa dano a entidades mas não altera o terreno
        // (keepBlocks = true / interaction = NONE, dependendo da API exata).
        level.explode(caster, null, (ExplosionDamageCalculator) null,
                caster.getX(), caster.getY() + 0.5, caster.getZ(),
                EXPLOSION_POWER, false, Level.ExplosionInteraction.TRIGGER);

        // Fallback, caso a assinatura acima não bata com a versão do NeoForge:
        // level.explode(caster, caster.getX(), caster.getY() + 0.5, caster.getZ(),
        //         EXPLOSION_POWER, Level.ExplosionInteraction.TRIGGER);
    }

    private GasIgniteAbility() {
    }
}