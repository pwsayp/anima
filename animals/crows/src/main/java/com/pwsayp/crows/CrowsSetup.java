package com.pwsayp.crows;

import com.pwsayp.crows.entity.Crow;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Всё, что нужно зарегистрировать на модовой шине после появления EntityType:
 * атрибуты, правила спавна и место яйца призыва в творческой вкладке.
 *
 * <p>Сам спавн в мире задаётся датапаком — см.
 * {@code data/crows/forge/biome_modifier/crows_in_fields.json}.</p>
 *
 * <p>Шина обычная, не модовая: в EventBus 7 модовыми считаются только события
 * с {@code getBus(BusGroup)} (например {@code ModConfigEvent}), а у всех событий
 * ниже статическое поле {@code BUS} — то есть общая шина.</p>
 */
@Mod.EventBusSubscriber(modid = Crows.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CrowsSetup {
    @SubscribeEvent
    static void onCreateAttributes(final EntityAttributeCreationEvent event) {
        event.put(Crows.CROW.get(), Crow.createAttributes().build());
    }

    @SubscribeEvent
    static void onRegisterSpawnPlacements(final SpawnPlacementRegisterEvent event) {
        event.register(
                Crows.CROW.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Crow::checkCrowSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(Crows.CROW_SPAWN_EGG);
        }
    }

    private CrowsSetup() {}
}
