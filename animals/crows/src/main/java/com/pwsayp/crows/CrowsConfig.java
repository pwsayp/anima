package com.pwsayp.crows;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Crows.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CrowsConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue EAT_CROPS = BUILDER
            .comment("Разрешить воронам клевать посевы. Если false — вороны остаются просто птицами.")
            .define("eatCrops", true);

    private static final ForgeConfigSpec.BooleanValue WHEAT_ONLY = BUILDER
            .comment("Клевать только пшеницу. Если false — любые посевы (морковь, картофель, свёкла и модовые CropBlock).")
            .define("wheatOnly", true);

    private static final ForgeConfigSpec.IntValue MIN_CROP_AGE = BUILDER
            .comment("Минимальная стадия роста, с которой посев становится интересен вороне. "
                   + "У пшеницы всего 8 стадий (0..7), так что 7 — это \"только спелое\": "
                   + "недозрелое поле ворона не трогает, ей нужно налившееся зерно. "
                   + "У культур с меньшим числом стадий значение подрезается по их максимуму.")
            .defineInRange("minCropAge", 7, 0, 7);

    private static final ForgeConfigSpec.IntValue STAGES_LOST = BUILDER
            .comment("На сколько стадий роста откатывается посев после того, как ворона его склевала. "
                   + "Одна стадия — это склёванный колос, а не вытоптанная грядка: "
                   + "потеря заметная, но не разорительная.")
            .defineInRange("stagesLost", 1, 1, 7);

    private static final ForgeConfigSpec.IntValue PECK_TICKS = BUILDER
            .comment("Сколько тиков ворона клюёт грядку, прежде чем урон засчитается. 20 тиков = 1 секунда. "
                   + "Это окно, в которое игрок ещё может успеть её спугнуть.")
            .defineInRange("peckTicks", 60, 5, 600);

    private static final ForgeConfigSpec.IntValue SEARCH_RANGE = BUILDER
            .comment("Радиус поиска грядки вокруг вороны, в блоках.")
            .defineInRange("searchRange", 12, 2, 32);

    private static final ForgeConfigSpec.DoubleValue SCARE_DISTANCE = BUILDER
            .comment("С какого расстояния ворона замечает игрока и улетает.")
            .defineInRange("scareDistance", 8.0D, 0.0D, 32.0D);

    private static final ForgeConfigSpec.BooleanValue HOODED = BUILDER
            .comment("Серая ворона вместо чёрной: серые спина и брюхо, чёрные голова, крылья и хвост.",
                     "Та самая птица, которую видно из окна в средней полосе.")
            .define("hooded", false);

    private static final ForgeConfigSpec.BooleanValue PERCH_ON_TREES = BUILDER
            .comment("Тянет ли ворон на деревья. Не найдя грядки, птица садится на ближайшую "
                   + "крону, а не болтается над поляной.")
            .define("perchOnTrees", true);

    private static final ForgeConfigSpec.IntValue PERCH_RANGE = BUILDER
            .comment("В каком радиусе ворона высматривает дерево, в блоках.")
            .defineInRange("perchRange", 24, 4, 64);

    private static final ForgeConfigSpec.IntValue PERCH_TICKS = BUILDER
            .comment("Сколько тиков ворона сидит на ветке, прежде чем снова взяться за дела. "
                   + "600 тиков = полминуты.")
            .defineInRange("perchTicks", 600, 20, 12000);

    private static final ForgeConfigSpec.BooleanValue RESPECT_MOB_GRIEFING = BUILDER
            .comment("Уважать игровое правило mobGriefing: при mobGriefing=false вороны не портят посевы.")
            .define("respectMobGriefing", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Значения по умолчанию продублированы здесь намеренно: цели ИИ читают searchRange
    // и scareDistance прямо в конструкторе, а он может отработать раньше загрузки конфига.
    public static boolean eatCrops = true;
    public static boolean wheatOnly = true;
    public static int minCropAge = 7;
    public static int stagesLost = 1;
    public static int peckTicks = 60;
    public static int searchRange = 12;
    public static double scareDistance = 8.0D;
    public static boolean hooded = false;
    public static boolean perchOnTrees = true;
    public static int perchRange = 24;
    public static int perchTicks = 600;
    public static boolean respectMobGriefing = true;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        eatCrops = EAT_CROPS.get();
        wheatOnly = WHEAT_ONLY.get();
        minCropAge = MIN_CROP_AGE.get();
        stagesLost = STAGES_LOST.get();
        peckTicks = PECK_TICKS.get();
        searchRange = SEARCH_RANGE.get();
        scareDistance = SCARE_DISTANCE.get();
        hooded = HOODED.get();
        perchOnTrees = PERCH_ON_TREES.get();
        perchRange = PERCH_RANGE.get();
        perchTicks = PERCH_TICKS.get();
        respectMobGriefing = RESPECT_MOB_GRIEFING.get();
    }

    private CrowsConfig() {}
}
