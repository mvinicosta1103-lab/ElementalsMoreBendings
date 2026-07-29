package com.elementals.morebendings.bending.airsubbendings.sound;

import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * SoundExpansionMod — segue exatamente o mesmo padrão do ObsidianWakeMod:
 * espera todos os mods carregarem (FMLLoadCompleteEvent), localiza a árvore
 * de Air/Sound já existente no elementals-neoforge.jar, e "planta" os novos
 * nós de habilidade dentro dela.
 *
 * IMPORTANTE: os nomes exatos dos métodos (findElement, getSubbendingRoot,
 * addChildNode, registerAbility, registerUpgrade) são placeholders baseados
 * na descrição do addon Obsidian Wake — troque pelos nomes reais assim que
 * comparar com as classes Bender/Element/Ability/Upgrade do seu projeto.
 *
 * Novas abilities registradas (todas dentro do subbending "sound"):
 *   - sonicShockwave   (dano em área + knockback)
 *   - resonancePulse   (quebra vidro/gelo + nausea)
 *   - silenceField     (toggle, zona furtiva/debuff)
 *   - sonicStep        (mobilidade/dash)
 *   - discordantScream (controle, atordoamento em alvo único)
 *   - echoSense        (toggle passivo, percepção/utilidade)
 *
 * Lembrete do addon original: o Air, assim como qualquer elemento, só tem 4
 * binds (bind1–bind4) no total, compartilhados entre TODOS os subbendings
 * de Air (atmosphere, common, flying, gas, mist, sound). Adicionar estas 6
 * abilities não cria novas teclas — o jogador vai precisar reatribuir os
 * slots na árvore de skills pra usar qualquer uma delas, do mesmo jeito que
 * já acontece com o Obsidian Wake no Lava.
 */
@EventBusSubscriber(modid = "elementalsmorebendings")
public class SoundExpansionMod {

    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        SoundTreeHook.extendSoundTree();
    }
}