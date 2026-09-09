package com.elementals.morebendings.bending.avatarstate;

/**
 * Nível de progressão do Avatar State de um jogador, travado no momento de
 * cada ativação (ver {@link AvatarStateManager#ACTIVE_ENTRY_LEVEL}) com base
 * em quantas vezes ele já completou uma transformação (ver {@code
 * PlayerAvatarData#getTransformationCount}). Controla:
 * <ul>
 *     <li>se a entrada MANUAL (tecla) é permitida nesse nível ({@link #allowsManualEntry});</li>
 *     <li>se a ativação tem duração limitada, e qual ({@link #hasDurationLimit}/{@link #getDurationTicks});</li>
 *     <li>se existe cooldown pra próxima entrada manual, e qual ({@link #hasCooldown}/{@link #getCooldownTicks});</li>
 *     <li>o nome mostrado ao jogador ({@link #getDisplayName}).</li>
 * </ul>
 * A instinto de sobrevivência ({@code AvatarNearDeathGuardian}) chama {@link
 * AvatarStateManager#activate} direto, então NUNCA é bloqueado por {@link
 * #allowsManualEntry} nem por cooldown -- essas duas restrições só valem pra
 * {@link AvatarStateManager#activateManual}.
 */
public enum AvatarProgressionLevel {

    /** Primeira vez (nenhuma transformação completada ainda) -- só entra por instinto de sobrevivência, sessão curta e cooldown longo. */
    UNTRAINED("Untrained", false, 20 * 20, 5 * 60 * 20),

    /** Algumas transformações completadas -- já pode entrar pela tecla, mas ainda com duração limitada e cooldown considerável. */
    NOVICE("Novice", true, 60 * 20, 3 * 60 * 20),

    /** Bastante experiência -- duração bem mais longa, cooldown mais curto. */
    ADEPT("Adept", true, 3 * 60 * 20, 90 * 20),

    /** Domínio total -- sem limite de duração e sem cooldown pra reentrar. */
    MASTER("Master", true, -1, -1);

    /** A partir de quantas transformações completadas cada nível começa (mesma ordem dos valores do enum). */
    private static final int NOVICE_THRESHOLD = 1;
    private static final int ADEPT_THRESHOLD = 3;
    private static final int MASTER_THRESHOLD = 6;

    private final String displayName;
    private final boolean manualEntry;
    private final long durationTicks;
    private final long cooldownTicks;

    AvatarProgressionLevel(String displayName, boolean manualEntry, long durationTicks, long cooldownTicks) {
        this.displayName = displayName;
        this.manualEntry = manualEntry;
        this.durationTicks = durationTicks;
        this.cooldownTicks = cooldownTicks;
    }

    /** @return o nível de progressão correspondente a {@code transformationCount} transformações já completadas. */
    public static AvatarProgressionLevel forTransformationCount(int transformationCount) {
        if (transformationCount >= MASTER_THRESHOLD) {
            return MASTER;
        }
        if (transformationCount >= ADEPT_THRESHOLD) {
            return ADEPT;
        }
        if (transformationCount >= NOVICE_THRESHOLD) {
            return NOVICE;
        }
        return UNTRAINED;
    }

    /** @return nome mostrado ao jogador (ex.: mensagem de ativação). */
    public String getDisplayName() {
        return displayName;
    }

    /** @return se este nível permite entrada MANUAL (tecla) -- sempre {@code true} pro instinto de sobrevivência, independente disso. */
    public boolean allowsManualEntry() {
        return manualEntry;
    }

    /** @return se a ativação deste nível se desliga sozinha após um tempo. */
    public boolean hasDurationLimit() {
        return durationTicks >= 0;
    }

    /** @return duração em ticks desta ativação, ou indefinido se {@link #hasDurationLimit()} for {@code false}. */
    public long getDurationTicks() {
        return durationTicks;
    }

    /** @return se sair da ativação neste nível aplica cooldown antes da próxima entrada manual. */
    public boolean hasCooldown() {
        return cooldownTicks >= 0;
    }

    /** @return cooldown em ticks antes da próxima entrada manual, ou indefinido se {@link #hasCooldown()} for {@code false}. */
    public long getCooldownTicks() {
        return cooldownTicks;
    }
}