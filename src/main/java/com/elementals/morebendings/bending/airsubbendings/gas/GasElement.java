public class GasElement extends Element {
    public static final String NAME = "Gas";

    public GasElement() {
        super(NAME, new Upgrade[]{
                new Upgrade(GasSkillTree.GAS_CLOUD, new Upgrade[]{ ... }, 0) // árvore inteira aninhada aqui
        });
        addAbility(new GasCloudAbility(), 0);
    }

    public static void register() { ... } // igual Atmosphere/Mud
    public static Element get() { ... }

    public static boolean canAcquire(Bender bender) {
        Element air = AirElement.get();
        return bender.hasElement(air) && air.isSkillTreeComplete(bender);
    }

    public static boolean isGasBender(Bender bender) {
        return bender.hasElement(get());
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this) && bender.getData().canUseUpgrade(GasSkillTree.GAS_IGNITE); // ou outro nó terminal representativo
    }
}