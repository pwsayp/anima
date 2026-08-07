package com.pwsayp.locusts;

import com.pwsayp.locusts.entity.Locust;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Сам налёт: когда стая приходит, откуда и куда.
 *
 * <p>Это не спавн, а событие. Мод изредка смотрит, есть ли у игрока настоящее поле, и если
 * есть — раз в сколько-то игровых дней приводит на него стаю. Никаких кладок, гнёзд и
 * источников, которые нужно искать и зачищать: пришла, съела, улетела.</p>
 *
 * <p>Стая появляется <b>в стороне</b> от поля и на высоте — чтобы её было видно и слышно
 * заранее. Эта минута до посадки и есть всё, что мод даёт игроку: успеешь дожать, раздуть
 * костры, накрыть грядки — молодец, не успеешь — смотри.</p>
 *
 * <p>Условия намеренно ванильные и проверяемые на глаз: день, сухая погода, тёплый биом и
 * поле хотя бы в {@link LocustsConfig#minField} посевов. В тайге и под дождём саранчи не
 * бывает.</p>
 */
@Mod.EventBusSubscriber(modid = Locusts.MODID)
public final class Swarm {
    /** В каком радиусе от игрока мод ищет поле. */
    private static final int FIELD_RADIUS = 32;

    /** С какого расстояния стая заходит на поле. */
    private static final int APPROACH = 28;

    /** На какой высоте над полем появляется туча. */
    private static final int APPROACH_HEIGHT = 10;

    /** Ниже этой температуры биома саранче холодно. */
    private static final float MIN_TEMPERATURE = 0.5F;

    private Swarm() {}

    @SubscribeEvent
    static void onServerTick(final TickEvent.ServerTickEvent.Post event) {
        if (!LocustsConfig.enabled) {
            return;
        }

        // Протухшие заявки на блоки метёт ядро, моду за этим следить не надо.
        final MinecraftServer server = event.server();
        if (server.getTickCount() % LocustsConfig.checkInterval != 0) {
            return;
        }

        for (final ServerLevel level : server.getAllLevels()) {
            for (final ServerPlayer player : level.players()) {
                if (player.isSpectator()) {
                    continue;
                }
                tryRaid(level, player);
            }
        }
    }

    private static void tryRaid(final ServerLevel level, final ServerPlayer player) {
        final RandomSource random = level.getRandom();
        if (random.nextDouble() >= LocustsConfig.swarmChance) {
            return;
        }

        BlockPos where = player.blockPosition();
        if (level.isRaining() || !level.isBrightOutside()) {
            return;
        }
        if (level.getBiome(where).value().getBaseTemperature() < MIN_TEMPERATURE) {
            return;
        }

        // Одна стая за раз: пока предыдущая не улетела, новую звать неоткуда. Без этого
        // налёты накладываются и вместо тучи получается бесконечный поток.
        if (!level.getEntitiesOfClass(Locust.class,
                player.getBoundingBox().inflate(FIELD_RADIUS * 2)).isEmpty()) {
            return;
        }

        List<BlockPos> targets = findTargets(level, where);
        if (targets.isEmpty()) {
            return;
        }

        release(level, center(targets), swarmSize(random), random);
    }

    /**
     * Куда лететь.
     *
     * <p>Годится любая еда, а не только грядки: саванна с травой, тростником и деревьями —
     * такое же приглашение, как поле пшеницы. Раньше налёт требовал именно посевов, и это
     * выглядело нелепо — стоишь посреди зелени, ничего не происходит, ставишь одну грядку,
     * и тут же прилетает туча.</p>
     *
     * <p>Но если посевы есть, стая идёт на них: поле для неё вкуснее луга, и садиться она
     * должна в его середину.</p>
     */
    private static List<BlockPos> findTargets(final ServerLevel level, final BlockPos center) {
        List<BlockPos> crops = new ArrayList<>();
        List<BlockPos> wild = new ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-FIELD_RADIUS, -8, -FIELD_RADIUS),
                center.offset(FIELD_RADIUS, 8, FIELD_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (!Locusts.isFood(state) || !Locusts.canLandOn(level, pos)) {
                continue;
            }
            if (state.is(BlockTags.CROPS)) {
                crops.add(pos.immutable());
            } else {
                wild.add(pos.immutable());
            }
        }

        return crops.isEmpty() ? wild : crops;
    }

    /** Центр найденного: стая садится в середину, а не с краю. */
    private static BlockPos center(final List<BlockPos> crops) {
        long x = 0;
        long y = 0;
        long z = 0;
        for (BlockPos pos : crops) {
            x += pos.getX();
            y += pos.getY();
            z += pos.getZ();
        }
        return new BlockPos((int) (x / crops.size()), (int) (y / crops.size()), (int) (z / crops.size()));
    }

    /**
     * Сколько особей приведёт налёт.
     *
     * <p>Случайно, от {@link LocustsConfig#swarmMin} до {@link LocustsConfig#swarmMax}, и
     * <b>без оглядки на то, сколько вокруг еды</b>. Стая — это стихия: сколько её принесло,
     * столько и принесло. Тем, кому не хватило грядок, достанется трава, а не хватит и
     * травы — сядут на землю и досидят своё.</p>
     */
    private static int swarmSize(final RandomSource random) {
        int min = Math.min(LocustsConfig.swarmMin, LocustsConfig.swarmMax);
        int max = Math.max(LocustsConfig.swarmMin, LocustsConfig.swarmMax);
        return min + random.nextInt(max - min + 1);
    }

    /** Выпустить стаю в стороне от поля и направить её туда. */
    private static void release(final ServerLevel level, final BlockPos field, final int size,
                                final RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        Vec3 from = new Vec3(
                field.getX() + Math.cos(angle) * APPROACH,
                field.getY() + APPROACH_HEIGHT,
                field.getZ() + Math.sin(angle) * APPROACH);

        for (int i = 0; i < size; i++) {
            Locust locust = Locusts.LOCUST.get().create(level, EntitySpawnReason.EVENT);
            if (locust == null) {
                continue;
            }

            // Туча, а не ком: разброс широкий, иначе особи вылетают друг у друга внутри и
            // толкаются всю дорогу до поля.
            locust.snapTo(
                    from.x + random.nextGaussian() * 9.0,
                    from.y + random.nextGaussian() * 3.0,
                    from.z + random.nextGaussian() * 9.0,
                    random.nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(locust);
            locust.getNavigation().moveTo(field.getX() + 0.5, field.getY() + 2.0, field.getZ() + 0.5, 1.0);
        }

        // Слышно раньше, чем видно: это и есть та фора, ради которой всё затевалось.
        level.playSound(null, BlockPos.containing(from), Locusts.SWARM_APPROACH.get(),
                SoundSource.HOSTILE, 7.0F, 1.0F);
    }
}
