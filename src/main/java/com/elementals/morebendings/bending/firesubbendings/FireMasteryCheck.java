package com.elementals.morebendings.bending.firesubbendings;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.data.PlayerData;

import java.util.ArrayList;
import java.util.List;

/**
 * Réplica, em texto legível, da checagem de "árvore de Fire masterizada"
 * que vive em {@code FireElement#isSkillTreeComplete} (mod base, sem
 * fonte disponível -- decompilada via bytecode pra montar esta lista).
 * Mesmo propósito de {@code AirMasteryCheck}, só que pro lado Fire
 * (usado por Plasma e futuras sub-bendings de Fire).
 * <p>
 * Serve só pra diagnóstico: {@code /morebending grant <player> plasma}
 * usa isso pra explicar EXATAMENTE quais nós faltam quando o grant
 * falha, em vez de só dizer "precisa masterizar Fire inteiro".
 * <p>
 * Estrutura real da checagem (decompilada em 26/07/2026,
 * elementals-neoforge-1.21.1-5.0.0.jar): 4 requisitos obrigatórios + 2
 * grupos "OU" (basta UM nó/caminho do grupo contar como satisfeito). Se
 * o mod base for atualizado e essa lista sair de sincronia, o pior caso
 * é o diagnóstico ficar impreciso -- a checagem de verdade (que decide
 * se o grant realmente acontece) continua sendo sempre {@code
 * FireElement#isSkillTreeComplete}, não esta classe.
 */
public final class FireMasteryCheck {

    private FireMasteryCheck() {
    }

    public static List<String> missingRequirements(Bender bender) {
        PlayerData data = bender.getData();
        List<String> missing = new ArrayList<>();

        if (!(data.canUseUpgrade("blueFire")
                || data.canUseUpgrade("fireWallWideI")
                || data.canUseUpgrade("fireWallTallI")
                || (data.canUseUpgrade("fireSpikesCountI") && data.canUseUpgrade("fireSpikesRangeI")))) {
            missing.add("Fire Flare-Up: falta UM destes -- Blue Fire, Fire Wall (Wide I ou Tall I), ou Fire Spikes (Count I + Range I)");
        }

        if (!data.canUseUpgrade("fireBallSpeedII")) {
            missing.add("Fire Ball: falta Speed II");
        }

        if (!(data.canUseUpgrade("flameThrower")
                || data.canUseUpgrade("fireShield")
                || (data.canUseUpgrade("fireWhipRangeI") && data.canUseUpgrade("fireWhipDamageI")))) {
            missing.add("Fire Arc: falta UM destes caminhos completos -- Flamethrower, Fire Shield, ou (Fire Whip Range I + Damage I)");
        }

        if (!data.canUseUpgrade("fireArcMastery")) {
            missing.add("Fire Arc: falta Fire Arc Mastery");
        }
        if (!data.canUseUpgrade("fireJetSpeedII")) {
            missing.add("Fire Jet: falta Speed II");
        }
        if (!data.canUseUpgrade("fireJumpRangeII")) {
            missing.add("Fire Jump: falta Range II");
        }

        return missing;
    }
}