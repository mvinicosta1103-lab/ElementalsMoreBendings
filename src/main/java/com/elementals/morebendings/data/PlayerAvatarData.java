package com.elementals.morebendings.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Estado de "Avatar State" por jogador — igual ao Avatar de verdade na
 * série, quando ativo o jogador tem acesso a TODAS as bendings de uma vez
 * (os 4 elementos-base + todas as sub-bendings deste addon), sem precisar
 * cumprir os pré-requisitos normais (masterizar a árvore do elemento-base,
 * já ter cruzado com um Blood bender, etc. — ver {@code
 * com.elementals.morebendings.commands.MoreBendingCommand#grantAvatarState}).
 * <p>
 * A parte delicada é DESLIGAR o Avatar State sem bagunçar o jogador: se ele
 * já tinha Fire antes (por exemplo) e o Avatar também "concedeu" Fire (não
 * concedeu, porque já tinha), desligar não pode tirar o Fire dele. Por
 * isso esta classe não guarda só um {@code boolean} — ela guarda
 * EXATAMENTE quais elementos e quais sub-bendings "flag-only" (as que não
 * têm {@code Element} de verdade, ex: Flying) foram concedidos POR CAUSA
 * do Avatar State, pra desligar poder desfazer só isso e nada mais.
 */
public class PlayerAvatarData {

    public static final Codec<PlayerAvatarData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("avatarState", false).forGetter(data -> data.avatarState),
            // Element#getName() de cada elemento (base ou sub-bending) concedido só por causa do Avatar State.
            Codec.STRING.listOf().optionalFieldOf("grantedElements", List.of())
                    .forGetter(data -> List.copyOf(data.grantedElements)),
            // Sub-bendings flag-only (sem Element real, ex: Flying) concedidas só por causa do Avatar State.
            Codec.STRING.listOf().optionalFieldOf("grantedFlagSubbendings", List.of())
                    .forGetter(data -> data.grantedFlagSubbendings.stream()
                            .map(SubbendingType::getId).toList()),
            // "Foto" do que o jogador tinha em Bender#plrData.elements (Element#getName()
            // de cada um, na ordem original) ANTES do Avatar State bloquear tudo -- ver
            // AvatarStateManager#activate/deactivate. Sem isso não dá pra devolver as
            // bendings de antes quando o jogador sai do Avatar State.
            Codec.STRING.listOf().optionalFieldOf("savedElements", List.of())
                    .forGetter(data -> List.copyOf(data.savedElements)),
            // activeElementIndex de antes de entrar no Avatar State, pra devolver o
            // jogador pro mesmo elemento ativo que ele estava usando (em vez de sempre
            // cair no primeiro da lista).
            Codec.INT.optionalFieldOf("savedActiveElementIndex", 0)
                    .forGetter(data -> data.savedActiveElementIndex)
    ).apply(instance, PlayerAvatarData::fromSaved));

    private boolean avatarState = false;
    private final Set<String> grantedElements = new HashSet<>();
    private final Set<SubbendingType> grantedFlagSubbendings = new HashSet<>();
    private final List<String> savedElements = new java.util.ArrayList<>();
    private int savedActiveElementIndex = 0;

    private static PlayerAvatarData fromSaved(boolean avatarState, List<String> grantedElements,
                                              List<String> grantedFlagIds, List<String> savedElements,
                                              int savedActiveElementIndex) {
        PlayerAvatarData data = new PlayerAvatarData();
        data.avatarState = avatarState;
        data.grantedElements.addAll(grantedElements);
        for (String id : grantedFlagIds) {
            SubbendingType.byId(id).ifPresent(data.grantedFlagSubbendings::add);
        }
        data.savedElements.addAll(savedElements);
        data.savedActiveElementIndex = savedActiveElementIndex;
        return data;
    }

    public boolean isAvatarState() {
        return avatarState;
    }

    public void setAvatarState(boolean value) {
        this.avatarState = value;
    }

    /** Registra que {@code elementName} (Element#getName()) foi concedido por causa do Avatar State. */
    public void markGrantedElement(String elementName) {
        grantedElements.add(elementName);
    }

    /** @return nomes (Element#getName()) de tudo que o Avatar State concedeu e que ainda não foi desfeito. */
    public Set<String> getGrantedElements() {
        return Set.copyOf(grantedElements);
    }

    public void clearGrantedElements() {
        grantedElements.clear();
    }

    /** Registra que a sub-bending flag-only {@code type} foi concedida por causa do Avatar State. */
    public void markGrantedFlag(SubbendingType type) {
        grantedFlagSubbendings.add(type);
    }

    public Set<SubbendingType> getGrantedFlagSubbendings() {
        return Set.copyOf(grantedFlagSubbendings);
    }

    public void clearGrantedFlagSubbendings() {
        grantedFlagSubbendings.clear();
    }

    /**
     * Guarda a lista de elementos (nomes) que o jogador tinha ANTES do Avatar
     * State bloqueá-los. Chamado uma única vez, no momento em que o Avatar
     * State liga (ver {@code AvatarStateManager#activate}).
     */
    public void setSavedElements(List<String> elementNames) {
        savedElements.clear();
        savedElements.addAll(elementNames);
    }

    /** @return cópia da lista salva (nunca null; vazia se não há nada salvo). */
    public List<String> getSavedElements() {
        return List.copyOf(savedElements);
    }

    public void clearSavedElements() {
        savedElements.clear();
        savedActiveElementIndex = 0;
    }

    public void setSavedActiveElementIndex(int index) {
        this.savedActiveElementIndex = index;
    }

    public int getSavedActiveElementIndex() {
        return savedActiveElementIndex;
    }
}