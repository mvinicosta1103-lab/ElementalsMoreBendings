package com.example.elementalmorebendings.common;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;

/**
 * MasterySupport
 * <p>
 * Utilitário público (compartilhado por Mud, Lava, Plant e qualquer
 * subbending futura deste addon) que responde a uma pergunta só:
 * "esse Bender já desbloqueou TODOS os upgrades da árvore inteira de tal
 * elemento?". Se sim, consideramos a subbending "masterizada" e as
 * habilidades desse elemento deixam de gastar Chi (dispensando o
 * "cansaço").
 * <p>
 * Por que isso é genérico em vez de uma lista de nomes escrita à mão:
 * cada {@code Element} do Elementals expõe sua árvore de upgrades via
 * {@code element.root}, e cada {@code Upgrade} expõe seus próprios
 * filhos via {@code Upgrade.children}. Percorrendo essa árvore
 * recursivamente e checando {@code plrData.canUseUpgrade(node.name)}
 * em cada nó, a checagem automaticamente enxerga:
 * <ul>
 *   <li>os upgrades originais do jar base (Elementals / Elementals
 *       Subbending), sejam quais forem seus nomes;</li>
 *   <li>os upgrades que este addon anexa em tempo de execução via
 *       {@code attachToBranch} (ElementalMoreBendingsMod), porque eles
 *       passam a ser filhos de nós já existentes na mesma árvore;</li>
 *   <li>qualquer upgrade novo que vier a ser adicionado no futuro, por
 *       este addon ou por outro, sem precisar tocar nesta classe.</li>
 * </ul>
 * Ou seja: não existe uma lista de "todas as habilidades" pra manter
 * atualizada — a própria árvore de upgrades É a lista.
 */
public final class MasterySupport {
    private MasterySupport() {}

    /**
     * @param bender      o jogador/bender sendo checado.
     * @param elementName nome do Element (ex.: "Mud", "Lava", "Plant").
     * @return true se o bender possui o elemento e já desbloqueou (comprou)
     *         absolutamente todos os nós da árvore de upgrades desse
     *         elemento — isto é, "masterizou" a subbending inteira.
     */
    public static boolean isElementMastered(Bender bender, String elementName) {
        if (bender == null || bender.plrData == null || elementName == null) {
            return false;
        }
        Element element = Element.getElement(elementName);
        if (element == null || !elementName.equals(element.getName()) || !bender.hasElement(element)) {
            return false;
        }
        return isNodeAndDescendantsUnlocked(bender, element.root);
    }

    private static boolean isNodeAndDescendantsUnlocked(Bender bender, Upgrade node) {
        if (node == null) {
            return true;
        }
        if (node.name != null && !bender.plrData.canUseUpgrade(node.name)) {
            return false;
        }
        Upgrade[] children = node.children;
        if (children != null) {
            for (Upgrade child : children) {
                if (!isNodeAndDescendantsUnlocked(bender, child)) {
                    return false;
                }
            }
        }
        return true;
    }
}