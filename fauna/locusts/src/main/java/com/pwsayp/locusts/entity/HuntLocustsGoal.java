package com.pwsayp.locusts.entity;

import java.util.EnumSet;
import java.util.List;

import com.pwsayp.locusts.LocustsConfig;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import org.jspecify.annotations.Nullable;

/**
 * Курица склёвывает саранчу.
 *
 * <p>Цель подсаживается ванильным курам при появлении в мире (см. {@code LocustsEvents}),
 * поэтому никакого нового моба заводить не нужно: курятник рядом с деревянным домом — это
 * и есть защита от налёта. Одного клевка хватает: в саранче две единицы здоровья.</p>
 */
public class HuntLocustsGoal extends Goal {
    /** С какого расстояния курица замечает саранчу. */
    private static final double SEARCH_RANGE = 8.0;

    /** Ближе этого можно клевать. */
    private static final double PECK_RANGE_SQR = 1.5;

    /**
     * Насколько высоко курица достаёт клювом.
     *
     * <p>Без этой проверки курица выбирала целью саранчу, висящую в трёх блоках над землёй,
     * и застывала под ней столбом до тех пор, пока та не спустится. Летящую добычу курица
     * просто не замечает — как и в жизни.</p>
     */
    private static final double PECK_HEIGHT = 1.5;

    /** Сколько курица гонится за одной саранчой, прежде чем махнуть на неё крылом. */
    private static final int GIVE_UP = 100;

    /** Пауза между клевками. */
    private static final int PECK_COOLDOWN = 20;

    private final PathfinderMob chicken;
    private @Nullable Locust prey;
    private int peckCooldown;

    /** Сколько тиков курица уже гонится, так и не догнав. */
    private int chase;

    public HuntLocustsGoal(final PathfinderMob chicken) {
        this.chicken = chicken;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!LocustsConfig.chickensHunt || this.chicken.isBaby()) {
            return false;
        }

        this.prey = this.findPrey();
        return this.prey != null;
    }

    @Override
    public boolean canContinueToUse() {
        // Высоту проверяем и здесь, а не только при выборе: саранча перепрыгивает с места на
        // место, и цель, только что сидевшая на земле, через миг оказывается в воздухе.
        // Без этой проверки курица упиралась в недосягаемую добычу и стояла столбом.
        return LocustsConfig.chickensHunt
                && this.prey != null
                && this.prey.isAlive()
                && this.chicken.distanceToSqr(this.prey) < SEARCH_RANGE * SEARCH_RANGE
                && Math.abs(this.prey.getY() - this.chicken.getY()) <= PECK_HEIGHT
                && this.chase < GIVE_UP;
    }

    @Override
    public void start() {
        this.peckCooldown = 0;
        this.chase = 0;
    }

    @Override
    public void stop() {
        this.prey = null;
        this.chase = 0;
        this.chicken.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.prey == null) {
            return;
        }

        this.chicken.getLookControl().setLookAt(this.prey, 30.0F, 30.0F);

        if (this.peckCooldown > 0) {
            this.peckCooldown--;
        }

        if (this.chicken.distanceToSqr(this.prey) > PECK_RANGE_SQR) {
            // Догоняем не бесконечно: не вышло за GIVE_UP тиков — цель бросается, и курица
            // возвращается к своим делам вместо того, чтобы стоять и смотреть.
            this.chase++;
            if (this.chicken.tickCount % 10 == 0) {
                this.chicken.getNavigation().moveTo(this.prey, 1.2);
            }
            return;
        }

        this.chicken.getNavigation().stop();
        if (this.peckCooldown > 0) {
            return;
        }

        this.peckCooldown = PECK_COOLDOWN;
        this.chase = 0;
        if (this.chicken.level() instanceof ServerLevel level) {
            // Курица не боевой моб: урон задаём напрямую, а не через атрибут атаки.
            this.prey.hurtServer(level, level.damageSources().mobAttack(this.chicken), 2.0F);
            this.chicken.playSound(SoundEvents.GENERIC_EAT.value(), 0.4F, 1.4F);
        }
    }

    private @Nullable Locust findPrey() {
        List<Locust> nearby = this.chicken.level().getEntitiesOfClass(
                Locust.class, this.chicken.getBoundingBox().inflate(SEARCH_RANGE));

        Locust closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Locust candidate : nearby) {
            if (Math.abs(candidate.getY() - this.chicken.getY()) > PECK_HEIGHT) {
                continue;
            }

            double distance = this.chicken.distanceToSqr(candidate);
            if (candidate.isAlive() && distance < closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
        }

        return closest;
    }
}
