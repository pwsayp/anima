package com.pwsayp.locusts;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Locusts.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LocustsConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Приходят ли налёты саранчи.")
            .define("enabled", true);

    private static final ForgeConfigSpec.IntValue CHECK_INTERVAL = BUILDER
            .comment("Раз в сколько тиков мод решает, не пора ли налёту. 20 тиков = 1 секунда.")
            .defineInRange("checkInterval", 1200, 20, 100000);

    private static final ForgeConfigSpec.DoubleValue SWARM_CHANCE = BUILDER
            .comment("Шанс налёта за одну такую проверку, если условия сошлись.",
                     "По умолчанию это примерно один налёт за несколько игровых дней "
                   + "непрерывной игры у большого поля.")
            .defineInRange("swarmChance", 0.04D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.IntValue SWARM_MIN = BUILDER
            .comment("Наименьшая стая.")
            .defineInRange("swarmMin", 40, 1, 400);

    private static final ForgeConfigSpec.IntValue SWARM_MAX = BUILDER
            .comment("Наибольшая стая. Это же и потолок нагрузки: больше мод не создаст.",
                     "Размер выбирается случайно между swarmMin и swarmMax и не зависит от "
                   + "того, сколько вокруг еды: сколько прилетело, столько и прилетело. "
                   + "Не нашедшие еды особи садятся на землю и досиживают своё.")
            .defineInRange("swarmMax", 120, 1, 400);

    private static final ForgeConfigSpec.IntValue EAT_TICKS = BUILDER
            .comment("Сколько тиков одна особь тратит на один блок.")
            .defineInRange("eatTicks", 20, 1, 600);

    private static final ForgeConfigSpec.BooleanValue EAT_GRASS = BUILDER
            .comment("Ест ли саранча дикую зелень по пути, а не только посевы: траву, цветы, "
                   + "поросль, тростник, бамбук, кактус, лианы, какао, ягодные кусты, тыквы "
                   + "и арбузы.")
            .define("eatGrass", true);

    private static final ForgeConfigSpec.BooleanValue EAT_LEAVES = BUILDER
            .comment("Объедает ли саранча кроны деревьев.",
                     "Крона ест только сверху: внутрь саранча не лезет, потому что садится "
                   + "лишь туда, где видно небо. Но листва не отрастает, поэтому прореженный "
                   + "лес останется таким навсегда — если это лишнее, флаг выключается.")
            .define("eatLeaves", true);

    private static final ForgeConfigSpec.IntValue SMOKE_RADIUS = BUILDER
            .comment("На сколько блоков вокруг горящего костра саранча не садится.")
            .defineInRange("smokeRadius", 6, 0, 32);

    private static final ForgeConfigSpec.BooleanValue CHICKENS_HUNT = BUILDER
            .comment("Клюют ли куры саранчу. Курятник у поля становится защитой.")
            .define("chickensHunt", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Значения продублированы: их читают цели ИИ и тики сервера, которые могут случиться
    // раньше загрузки конфига.
    public static boolean enabled = true;
    public static int checkInterval = 1200;
    public static double swarmChance = 0.04D;
    public static int swarmMin = 40;
    public static int swarmMax = 120;
    public static int eatTicks = 20;
    public static boolean eatGrass = true;
    public static boolean eatLeaves = true;
    public static int smokeRadius = 6;
    public static boolean chickensHunt = true;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enabled = ENABLED.get();
        checkInterval = CHECK_INTERVAL.get();
        swarmChance = SWARM_CHANCE.get();
        swarmMin = SWARM_MIN.get();
        swarmMax = SWARM_MAX.get();
        eatTicks = EAT_TICKS.get();
        eatGrass = EAT_GRASS.get();
        eatLeaves = EAT_LEAVES.get();
        smokeRadius = SMOKE_RADIUS.get();
        chickensHunt = CHICKENS_HUNT.get();
    }

    private LocustsConfig() {}
}
