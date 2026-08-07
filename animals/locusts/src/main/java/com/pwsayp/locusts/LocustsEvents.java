package com.pwsayp.locusts;

import com.pwsayp.locusts.entity.HuntLocustsGoal;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Всё, что мод делает с чужими сущностями: атрибуты саранчи, место яйца призыва во вкладке
 * и подсаженная курам цель «склевать саранчу».
 *
 * <p>Куры — единственный живой ответ на налёт, и он ванильный до конца: никакого нового
 * моба, только цель, которую получает обычная курица. Курятник у поля впервые становится
 * не декорацией, а страховкой.</p>
 *
 * <p>В мире саранча сама не спавнится: она приходит только налётом (см. {@link Swarm}),
 * поэтому правило спавна намеренно запрещающее.</p>
 */
@Mod.EventBusSubscriber(modid = Locusts.MODID)
public final class LocustsEvents {

    @SubscribeEvent
    static void onCreateAttributes(final EntityAttributeCreationEvent event) {
        event.put(Locusts.LOCUST.get(), com.pwsayp.locusts.entity.Locust.createAttributes().build());
    }

    @SubscribeEvent
    static void onRegisterSpawnPlacements(final SpawnPlacementRegisterEvent event) {
        event.register(
                Locusts.LOCUST.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> false,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(Locusts.LOCUST_SPAWN_EGG);
        }
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(Locusts.LOCUST_ITEM);
            event.accept(Locusts.COOKED_LOCUST);
        }
    }

    @SubscribeEvent
    static void onEntityJoin(final EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Chicken chicken) {
            chicken.goalSelector.addGoal(3, new HuntLocustsGoal(chicken));
        }
    }

    private LocustsEvents() {}
}
