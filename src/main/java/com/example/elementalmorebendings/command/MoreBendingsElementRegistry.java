package com.example.elementalmorebendings.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MoreBendingsElementRegistry
 * <p>
 * Lista central dos elementos que o ElementalMoreBendings adiciona (Mud,
 * e os que vierem depois). Cada Element novo se registra aqui na hora que
 * é instanciado (ex: dentro de registerMudElement()), e o comando
 * /morebendings grant lê essa lista pra montar o autocomplete.
 * <p>
 * Assim, adicionar um subbending novo no futuro não exige tocar no
 * comando: basta chamar register("NomeDoElemento") no ponto onde o
 * elemento é criado.
 */
public final class MoreBendingsElementRegistry {

    private static final List<String> REGISTERED = new ArrayList<>();

    private MoreBendingsElementRegistry() {
    }

    public static void register(String elementName) {
        if (!REGISTERED.contains(elementName)) {
            REGISTERED.add(elementName);
        }
    }

    public static List<String> getRegisteredNames() {
        return Collections.unmodifiableList(REGISTERED);
    }
}