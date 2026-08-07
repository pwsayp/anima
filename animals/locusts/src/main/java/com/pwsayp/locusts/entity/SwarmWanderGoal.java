package com.pwsayp.locusts.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Между укусами саранча не стоит на месте, а мельтешит над полем.
 *
 * <p>Цель нарочно тупая: короткие перелёты в случайную точку рядом. Стая должна выглядеть
 * роем, а не строем, и вся её «умность» держится на том, что особей много и каждая занята
 * своим блоком.</p>
 */
public class SwarmWanderGoal extends Goal {
    /** Сколько раз подряд искать сухую точку, прежде чем остаться на месте. */
    private static final int TRIES = 4;

    private final Locust locust;

    public SwarmWanderGoal(final Locust locust) {
        this.locust = locust;
        this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.locust.getNavigation().isDone()) {
            return false;
        }

        // Севшая особь двигается реже летающей, но не замирает: неподвижное насекомое
        // читается как зависшее. Раньше пауза была вчетверо длиннее, и особь подолгу стояла
        // столбом.
        int rarity = this.locust.isIdle() ? 50 : 8;
        return this.locust.getRandom().nextInt(rarity) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.locust.getNavigation().isInProgress();
    }

    @Override
    public void start() {
        Vec3 target = hover();
        if (target != null) {
            this.locust.getNavigation().moveTo(target.x, target.y, target.z, 1.0);
        }
    }

    /**
     * Куда перепорхнуть.
     *
     * <p>Точка ищется несколько раз подряд, пока не попадётся сухая: генератор ванильный и
     * воду предлагает охотно, а саранче она смертельна — севшая на пруд особь захлёбывалась.
     * Не нашлось сухой за {@link #TRIES} попыток — значит вокруг вода, и лучше остаться на
     * месте, чем лезть в неё.</p>
     */
    private @Nullable Vec3 hover() {
        for (int attempt = 0; attempt < TRIES; attempt++) {
            Vec3 target = randomPos();
            if (target != null && dry(target)) {
                return target;
            }
        }
        return null;
    }

    private @Nullable Vec3 randomPos() {
        Vec3 forward = this.locust.getViewVector(0.0F);
        if (this.locust.isIdle()) {
            // Занятия нет: короткий перескок вниз, к земле. Отрицательная высота у цели —
            // это и есть «садись», всё остальное доделает притяжение.
            return AirAndWaterRandomPos.getPos(this.locust, 3, 2, -2, forward.x, forward.z, Math.PI / 2);
        }
        return AirAndWaterRandomPos.getPos(this.locust, 8, 2, -1, forward.x, forward.z, Math.PI / 2);
    }

    /** Ни воды в самой точке, ни воды под ней. */
    private boolean dry(final Vec3 target) {
        BlockPos pos = BlockPos.containing(target);
        return this.locust.level().getBlockState(pos).getFluidState().isEmpty()
                && this.locust.level().getBlockState(pos.below()).getFluidState().isEmpty();
    }

    /** Точка, к которой стая стягивается: центр поля, на которое её привели. */
    public static Vec3 above(final BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5);
    }
}
