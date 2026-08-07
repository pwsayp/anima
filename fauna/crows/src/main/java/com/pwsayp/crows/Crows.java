package com.pwsayp.crows;

import com.pwsayp.crows.entity.Crow;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Crows — вороны, которые слетаются на поля и склёвывают почти созревшую пшеницу.
 *
 * <p>Мод добавляет ровно одного моба и яйцо призыва к нему: ни блоков, ни рецептов,
 * ни GUI. Ущерб — не потеря урожая, а потеря времени: посев откатывается на несколько
 * стадий роста.</p>
 */
@Mod(Crows.MODID)
public final class Crows {
    public static final String MODID = "crows";

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);

    /**
     * Карканье — настоящее, а не попугай с приспущенным тоном.
     *
     * <p>Раньше голос вороны собирался из семплов попугая, сыгранных на 0.62 от исходной
     * высоты. Слышалось это не как птица, а как что-то большое и недоброе в кустах: тон
     * тянет за собой длину и тембр, и ускоренный попугай остаётся попугаем. Теперь свои
     * файлы, и высота голоса вернулась к обычной единице.</p>
     */
    public static final RegistryObject<SoundEvent> CROW_AMBIENT = SOUNDS.register(
            "entity.crow.ambient",
            () -> SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath(MODID, "entity.crow.ambient")));

    public static final RegistryObject<SoundEvent> CROW_HURT = SOUNDS.register(
            "entity.crow.hurt",
            () -> SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath(MODID, "entity.crow.hurt")));

    public static final RegistryObject<SoundEvent> CROW_DEATH = SOUNDS.register(
            "entity.crow.death",
            () -> SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath(MODID, "entity.crow.death")));

    public static final RegistryObject<EntityType<Crow>> CROW = ENTITY_TYPES.register(
            "crow",
            () -> EntityType.Builder.of(Crow::new, MobCategory.CREATURE)
                    .sized(0.5F, 0.6F)
                    .eyeHeight(0.5F)
                    .clientTrackingRange(8)
                    .build(ENTITY_TYPES.key("crow")));

    public static final RegistryObject<Item> CROW_SPAWN_EGG = ITEMS.register(
            "crow_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties()
                    .spawnEgg(CROW.get())
                    .setId(ITEMS.key("crow_spawn_egg"))));

    public Crows(final FMLJavaModLoadingContext context) {
        BusGroup modBus = context.getModBusGroup();
        ENTITY_TYPES.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        context.registerConfig(ModConfig.Type.COMMON, CrowsConfig.SPEC);
    }
}
