package com.elementals.morebendings.situations;

import com.elementals.morebendings.data.SubbendingType;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Descreve uma sub-bending que pode ser aprendida "no susto" pelo
 * {@link SituationsSystem}, sem precisar do scroll (ver
 * {@code AbstractSubbendingScrollItem} pro caminho equivalente via item).
 *
 * @param type              a sub-bending concedida, só pra mensagens/logs
 * @param parentElement     bending base exigida (Earth/Air/Fire/Water) --
 *                          ainda precisa ser esse tipo de dobrador; só a
 *                          mastery completa da árvore-mãe que é dispensada
 * @param subbendingElement a sub-bending em si
 * @param situation         a condição do ambiente que precisa estar
 *                          verdadeira no momento da checagem
 * @param chancePerCheck    chance (0.0-1.0) de aprender quando a condição
 *                          bate, rolada a cada checagem periódica
 * @param discoveryMessage  mensagem mostrada ao jogador quando aprende
 * @param onGranted         passo extra pós-addElement (ex: autoUnlockRoot
 *                          pras sub-bendings cujo nó raiz tem preço 0 --
 *                          mesmo motivo do {@code onGranted} do scroll)
 */
public record SituationDefinition(
        SubbendingType type,
        Supplier<Element> parentElement,
        Supplier<Element> subbendingElement,
        Situation situation,
        double chancePerCheck,
        String discoveryMessage,
        Consumer<Bender> onGranted
) {

    /** Conveniência pras sub-bendings que não precisam de nenhum passo pós-addElement. */
    public SituationDefinition(SubbendingType type, Supplier<Element> parentElement,
                                Supplier<Element> subbendingElement, Situation situation,
                                double chancePerCheck, String discoveryMessage) {
        this(type, parentElement, subbendingElement, situation, chancePerCheck, discoveryMessage, bender -> {
        });
    }
}
