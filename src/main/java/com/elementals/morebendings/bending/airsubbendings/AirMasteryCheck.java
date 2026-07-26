package com.elementals.morebendings.bending.airsubbendings;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.data.PlayerData;

import java.util.ArrayList;
import java.util.List;

/**
 * Réplica, em texto legível, da checagem de "árvore de Air masterizada"
 * que vive em {@code AirElement#isSkillTreeComplete} (mod base, sem fonte
 * disponível -- decompilada via bytecode/javap pra montar esta lista).
 *
 * Serve só pra diagnóstico: {@code /morebending grant <player> gas/atmosphere}
 * usa isso pra explicar EXATAMENTE quais nós faltam quando o grant falha,
 * em vez de só dizer "precisa masterizar Air inteiro" (que não ajuda em
 * nada quando o jogador já acha que comprou tudo).
 *
 * Estrutura real da checagem (decompilada em 26/07/2026, elementals-neoforge
 * -1.21.1-5.0.0.jar): 5 requisitos obrigatórios + 2 grupos "OU" (basta UM
 * nó do grupo contar como satisfeito). Se o mod base for atualizado e essa
 * lista sair de sincronia, o pior caso é o diagnóstico ficar impreciso --
 * a checagem de verdade (que decide se o grant realmente acontece) continua
 * sendo sempre {@code AirElement#isSkillTreeComplete}, não esta classe.
 */
public final class AirMasteryCheck {

    private AirMasteryCheck() {
    }

    public static List<String> missingRequirements(Bender bender) {
        PlayerData data = bender.getData();
        List<String> missing = new ArrayList<>();

        if (!(data.canUseUpgrade("airTornadoSpeedII")
                || data.canUseUpgrade("airShield")
                || data.canUseUpgrade("airSuction"))) {
            missing.add("Gust: falta UM destes -- Tornado Speed II, Air Shield ou Air Suction");
        }

        if (!data.canUseUpgrade("airBallSpeedII")) {
            missing.add("Air Ball: falta Speed II");
        }

        boolean bulletsPath = data.canUseUpgrade("airBulletsMastery") && data.canUseUpgrade("airBulletsCountII");
        boolean suffocatePath = data.canUseUpgrade("airSuffocate");
        boolean bladePath = data.canUseUpgrade("airBladeRangeI") && data.canUseUpgrade("airBladeDamageI");
        if (!(bulletsPath || suffocatePath || bladePath)) {
            missing.add("Air Stream: falta UM destes caminhos completos -- "
                    + "(Bullets Mastery + Bullets Count II), Suffocate, ou (Blade Range I + Blade Damage I)");
        }

        if (!data.canUseUpgrade("airStreamMastery")) {
            missing.add("Air Stream: falta Air Stream Mastery");
        }
        if (!data.canUseUpgrade("airJumpRangeII")) {
            missing.add("Air Jump: falta Range II");
        }
        if (!data.canUseUpgrade("airScooterSpeedII")) {
            missing.add("Air Scooter: falta Speed II");
        }
        if (!data.canUseUpgrade("airSpiritProjectionRangeIV")) {
            missing.add("Spirit Projection: falta Range IV");
        }

        return missing;
    }
}