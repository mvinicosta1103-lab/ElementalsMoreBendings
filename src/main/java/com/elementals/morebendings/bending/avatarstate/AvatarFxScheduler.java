package com.elementals.morebendings.bending.avatarstate.fx;

import java.util.ArrayList;
import java.util.List;

/**
 * Agendador de efeitos visuais em múltiplos ticks pras abilities do Avatar
 * State (pilares subindo, ondas viajando, meteoros caindo, etc.) -- a API
 * base do Elementals não tem nada assim, e todas as abilities do mod são
 * instantâneas (disparam tudo dentro de {@code onCall} e nunca setam
 * {@code currAbility}), então qualquer animação "coreografada" (que dure
 * mais que um tick) precisa desse agendador em vez de rodar tudo de uma
 * vez só.
 * <p>
 * Tickado uma vez por tick do servidor por {@code
 * AvatarStateManager#onServerTick} (ver a chamada {@code
 * AvatarFxScheduler.tick()} logo no início do método). Sem threads, sem
 * estado por jogador -- é só uma fila global de {@code Runnable}s com um
 * atraso em ticks. Cada {@code Runnable} agendado já carrega dentro de si
 * toda referência que precisa (nível, posição, etc. via closure) e deve
 * checar sozinho se ainda faz sentido rodar (ex: {@code level.isLoaded}) se
 * isso importar -- pras animações curtas usadas aqui (no máximo ~1s) isso
 * nunca chega a ser um problema real.
 * <p>
 * Uma exception dentro de uma task de FX é só logada e ignorada -- nunca
 * pode derrubar o tick do servidor inteiro por causa de um efeito visual
 * quebrado.
 */
public final class AvatarFxScheduler {

    private record Scheduled(int ticksLeft, Runnable task) {
        private Scheduled tick() {
            return new Scheduled(ticksLeft - 1, task);
        }
    }

    private static final List<Scheduled> QUEUE = new ArrayList<>();
    // Fila auxiliar pra permitir agendar de dentro de uma task (ex: um passo da
    // animação agenda o próximo) sem mexer em QUEUE enquanto ela está sendo
    // percorrida em tick().
    private static final List<Scheduled> PENDING = new ArrayList<>();

    private AvatarFxScheduler() {
    }

    /**
     * Agenda {@code task} pra rodar daqui a {@code delayTicks} ticks do
     * servidor (0 = já no próximo tick). Chame quantas vezes precisar pra
     * montar uma sequência (ex: um pilar por tick, uma ring de partículas a
     * cada 2 ticks...).
     */
    public static void schedule(int delayTicks, Runnable task) {
        PENDING.add(new Scheduled(Math.max(0, delayTicks), task));
    }

    /** Chamado uma vez por tick do servidor, a partir de {@code AvatarStateManager#onServerTick}. */
    public static void tick() {
        if (!PENDING.isEmpty()) {
            QUEUE.addAll(PENDING);
            PENDING.clear();
        }
        if (QUEUE.isEmpty()) {
            return;
        }

        List<Scheduled> due = null;
        for (int i = QUEUE.size() - 1; i >= 0; i--) {
            Scheduled s = QUEUE.get(i);
            if (s.ticksLeft() <= 0) {
                if (due == null) {
                    due = new ArrayList<>();
                }
                due.add(s);
                QUEUE.remove(i);
            } else {
                QUEUE.set(i, s.tick());
            }
        }
        if (due == null) {
            return;
        }
        // due foi montada de trás pra frente (percorremos QUEUE ao contrário) --
        // roda na ordem em que foram agendadas.
        for (int i = due.size() - 1; i >= 0; i--) {
            try {
                due.get(i).task().run();
            } catch (Exception ignored) {
                // efeito visual quebrado não pode travar o tick do servidor.
            }
        }
    }
}