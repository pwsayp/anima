package com.pwsayp.locusts;

import com.pwsayp.anima.world.Greenery;
import com.pwsayp.anima.world.Sky;
import com.pwsayp.anima.world.Smoke;
import com.pwsayp.locusts.entity.Locust;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Locusts — саранча, которая приходит тучей и уходит дальше.
 *
 * <p>Это мод не про моба, а про событие. Стая не живёт рядом, не размножается и не требует
 * зачистки: раз в редкое время она появляется с одной стороны горизонта, садится на самое
 * крупное поле по пути, обжирает его за пару минут и улетает. Драться с ней бессмысленно —
 * особей полсотни, и в этом весь смысл: ответ у мода хозяйственный, а не боевой.</p>
 *
 * <p>Слышно её раньше, чем видно, и это главное: у игрока есть примерно минута, чтобы
 * успеть хоть что-то — дожать поле, раздуть костры, закрыть грядки. Дальше остаётся
 * смотреть.</p>
 *
 * <p>Мод опирается на ванильные теги и на ядро Anima — и ни на один соседний мод. Любая
 * культура, которая честно лежит в теге {@code minecraft:crops}, будет съедена: хоть
 * ванильная пшеница, хоть чужая капуста.</p>
 */
@Mod(Locusts.MODID)
public final class Locusts {
    public static final String MODID = "locusts";

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);

    /**
     * Голос саранчи — свой, а не пчелиный.
     *
     * <p>Раньше особь звучала семплами пчелы, поднятыми по высоте: получался улей, а не
     * поле. Стрёкот у прямокрылых сухой и трескучий, и подделать его чужим тоном нельзя —
     * высота тянет за собой и длину, и тембр.</p>
     */
    public static final RegistryObject<SoundEvent> LOCUST_AMBIENT = SOUNDS.register(
            "entity.locust.ambient",
            () -> SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath(MODID, "entity.locust.ambient")));

    public static final RegistryObject<SoundEvent> LOCUST_HURT = SOUNDS.register(
            "entity.locust.hurt",
            () -> SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath(MODID, "entity.locust.hurt")));

    public static final RegistryObject<SoundEvent> LOCUST_DEATH = SOUNDS.register(
            "entity.locust.death",
            () -> SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath(MODID, "entity.locust.death")));

    /**
     * Гул подходящей стаи — та самая фора, ради которой всё затевалось.
     *
     * <p>Играется один раз, когда налёт только выпущен в стороне от поля, и слышен далеко.
     * Это единственное предупреждение игроку: услышал — беги жать.</p>
     */
    public static final RegistryObject<SoundEvent> SWARM_APPROACH = SOUNDS.register(
            "event.swarm_approach",
            () -> SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath(MODID, "event.swarm_approach")));

    public static final RegistryObject<EntityType<Locust>> LOCUST = ENTITY_TYPES.register(
            "locust",
            () -> EntityType.Builder.of(Locust::new, MobCategory.AMBIENT)
                    .sized(0.35F, 0.3F)
                    .eyeHeight(0.2F)
                    .clientTrackingRange(10)
                    .build(ENTITY_TYPES.key("locust")));

    /**
     * Сбитая саранча — еда бедняка.
     *
     * <p>Единственное, что можно взять с налёта, и взято оно не просто так: разорённое поле
     * перестаёт быть чистым вычетом. Прок, впрочем, невелик — одно деление голода и почти
     * никакой сытости: сухая хитиновая мелочь, которой набивают брюхо, когда больше нечем.
     * Наесться ею нельзя, дотянуть до урожая — можно.</p>
     */
    public static final RegistryObject<Item> LOCUST_ITEM = ITEMS.register(
            "locust",
            () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(1)
                            .saturationModifier(0.05F)
                            .build())
                    .setId(ITEMS.key("locust"))));

    /**
     * Жареная саранча.
     *
     * <p>Огонь делает её вдвое стоящей, но роскошью не делает: это по-прежнему закуска, а не
     * обед. Дешевле любой другой жарёхи и настолько же скудная.</p>
     */
    public static final RegistryObject<Item> COOKED_LOCUST = ITEMS.register(
            "cooked_locust",
            () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(3)
                            .saturationModifier(0.2F)
                            .build())
                    .setId(ITEMS.key("cooked_locust"))));

    public static final RegistryObject<Item> LOCUST_SPAWN_EGG = ITEMS.register(
            "locust_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties()
                    .spawnEgg(LOCUST.get())
                    .setId(ITEMS.key("locust_spawn_egg"))));

    public Locusts(final FMLJavaModLoadingContext context) {
        BusGroup modBus = context.getModBusGroup();
        ENTITY_TYPES.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        context.registerConfig(ModConfig.Type.COMMON, LocustsConfig.SPEC);
    }

    /**
     * Что саранча ест.
     *
     * <p>Всё зелёное и мягкое, до чего дотянется. Посевы — всегда: это и есть налёт. Дикая
     * зелень и кроны деревьев — по флагам, потому что вытравленный луг и прореженный лес
     * остаются надолго, а листва не отрастает вовсе.</p>
     *
     * <p>Списка блоков тут нет: что считать зеленью, знает ядро, а что посевом — ванильный
     * тег. Своего перечня мод не заводит намеренно, иначе чужая культура была бы съедобна
     * для саранчи и несъедобна для ворон.</p>
     */
    public static boolean isFood(final BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.is(BlockTags.CROPS)) {
            return true;
        }
        if (state.is(BlockTags.LEAVES)) {
            return LocustsConfig.eatLeaves;
        }
        if (Greenery.isWild(state)) {
            return LocustsConfig.eatGrass;
        }
        return false;
    }

    /**
     * Годится ли блок под посадку стаи.
     *
     * <p>Две ванильные преграды, и обе ставятся заранее: крыша над грядкой и дым костра.
     * Обе живут в ядре — от них одинаково спасаются все летающие напасти семейства, иначе
     * игроку пришлось бы запоминать, от кого костёр помогает, а от кого нет.</p>
     *
     * <p>Третья причина отказаться от места — вода. Саранча над ней не садится: сев на воду,
     * особь захлёбывалась и тонула, и вместо налёта получалось самоубийство стаи в
     * ближайшем пруду.</p>
     */
    public static boolean canLandOn(final LevelReader level, final BlockPos pos) {
        return Sky.openAbove(level, pos)
                && !overWater(level, pos)
                && !Smoke.campfireNearby(level, pos, LocustsConfig.smokeRadius);
    }

    /**
     * Вода в самом блоке или прямо под ним.
     *
     * <p>Тростнику это не мешает: он растёт <b>рядом</b> с водой, а стоит на песке или
     * земле.</p>
     */
    public static boolean overWater(final LevelReader level, final BlockPos pos) {
        return !level.getBlockState(pos).getFluidState().isEmpty()
                || !level.getBlockState(pos.below()).getFluidState().isEmpty();
    }
}
