package com.pwsayp.crows.entity;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Полёт вороны: долгие перелёты вместо порханий.
 *
 * <p>Ванильное «случайное блуждание по воздуху» выдаёт птице цель в восьми блоках. Долетев,
 * она останавливается, думает, берёт новую — и так по кругу. Отсюда и ощущение вялого
 * попугая: дело не в скорости, а в том, что птица никуда не летит. Длинного движения не
 * получается, потому что его никто и не задавал.</p>
 *
 * <p>Здесь цель другая: <b>далеко и с высотой</b>. Точка берётся за два-три десятка блоков,
 * примерно по ходу движения — резких разворотов на месте не выходит, и птица идёт длинной
 * дугой. Высота выбирается по жребию, и именно она делает полёт живым:</p>
 *
 * <ul>
 *   <li><b>Набор</b> — цель заметно выше земли: ворона лезет вверх, часто маша крыльями.</li>
 *   <li><b>Пикирование</b> — цель у самой земли: снижение выходит быстрым, с прижатыми
 *       крыльями, потому что вниз птица идёт ходом, а не гребёт.</li>
 *   <li><b>Ровный ход</b> — на своей высоте, вперёд.</li>
 * </ul>
 *
 * <p>Чередование набора и снижения — это и есть врановая манера: они не держат высоту, а
 * идут пологой волной.</p>
 */
public class SoarGoal extends Goal {
    /** Как далеко берётся точка: длинный перелёт вместо восьми блоков ванильной цели. */
    private static final int RANGE = 26;

    /** Насколько цель уводится вбок от текущего курса, в радианах. */
    private static final double TURN = Math.PI / 2.5;

    /** Сколько раз пробовать точку, прежде чем отступиться. */
    private static final int TRIES = 6;

    /** Пауза между перелётами: птица не срывается в новый маршрут в ту же секунду. */
    private static final int REST = 30;

    private final Crow crow;
    private int rest;

    public SoarGoal(final Crow crow) {
        this.crow = crow;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.rest-- > 0 || !this.crow.getNavigation().isDone()) {
            return false;
        }

        Vec3 target = this.pick();
        if (target == null) {
            this.rest = REST;
            return false;
        }

        // Скорость своя у каждого перелёта: ровная у всех стая выглядит строем.
        double speed = 1.0 + this.crow.getRandom().nextDouble() * 0.35;
        boolean started = this.crow.getNavigation().moveTo(target.x, target.y, target.z, speed);
        if (!started) {
            this.rest = REST;
        }
        return started;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.crow.getNavigation().isDone();
    }

    @Override
    public void stop() {
        this.rest = REST / 2 + this.crow.getRandom().nextInt(REST);
    }

    /**
     * Куда лететь.
     *
     * <p>Направление — по ходу движения с разбросом: так получается дуга, а не ломаная.
     * Высота — по жребию из трёх повадок; отсчитывается она от земли под самой точкой, а не
     * от птицы, иначе над обрывом ворона уходит в стратосферу.</p>
     */
    private @Nullable Vec3 pick() {
        Level level = this.crow.level();
        RandomSource random = this.crow.getRandom();

        double heading = Math.atan2(this.crow.getLookAngle().z, this.crow.getLookAngle().x);

        for (int i = 0; i < TRIES; i++) {
            double angle = heading + (random.nextDouble() - 0.5) * 2.0 * TURN;
            double distance = RANGE * (0.55 + random.nextDouble() * 0.45);
            double x = this.crow.getX() + Math.cos(angle) * distance;
            double z = this.crow.getZ() + Math.sin(angle) * distance;

            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Mth.floor(x), Mth.floor(z));
            double y = ground + this.height(random);

            // Точка должна быть в воздухе, иначе навигация оборвёт маршрут у самого конца.
            BlockPos pos = BlockPos.containing(x, y, z);
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
                continue;
            }
            return new Vec3(x, y, z);
        }
        return null;
    }

    /** Высота над землёй: набор, ровный ход или пикирование почти к траве. */
    private double height(final RandomSource random) {
        int roll = random.nextInt(10);
        if (roll < 3) {
            return 12.0 + random.nextDouble() * 8.0;
        }
        if (roll < 7) {
            return 6.0 + random.nextDouble() * 5.0;
        }
        return 2.0 + random.nextDouble() * 2.0;
    }
}
