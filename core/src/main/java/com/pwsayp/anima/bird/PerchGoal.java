package com.pwsayp.anima.bird;

import java.util.EnumSet;
import java.util.List;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

import com.pwsayp.anima.world.Scan;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;

/**
 * Птица предпочитает сидеть на дереве.
 *
 * <p>Без этой цели птица ведёт себя как чайка на пляже: бесцельно летает над поляной и
 * садится где придётся, чаще всего посреди открытого места. Живая птица так не делает —
 * она держится верхушек: оттуда видно и кормовое место, и человека, идущего к нему.</p>
 *
 * <p>Ставить её надо выше бесцельного полёта, но ниже испуга и промысла: увидела
 * человека — прочь; нашла еду — к еде; больше ничего интересного — на ближайшую крону, а
 * не куда глаза глядят.</p>
 *
 * <p>Насест ищется двумя способами. Вблизи честно перебираются блоки, вдаль идёт выборка
 * случайных точек — и то и другое взято из ядра, потому что перебирать куб со стороной в
 * три десятка блоков на каждую птицу нельзя.</p>
 */
public class PerchGoal extends Goal {
    /** Раз в сколько тиков птица задумывается о насесте. */
    private static final int THINK_INTERVAL = 60;

    /** Ближний обзор — честный перебор. */
    private static final int NEAR_RADIUS = 10;

    /** Дальний — выборкой. */
    private static final int FAR_SAMPLES = 200;

    /** Сколько кандидатов набирать вблизи, чтобы выбрать из них наугад. */
    private static final int CANDIDATES = 12;

    /**
     * На сколько блоков выше ветки идёт заход.
     *
     * <p>Ровно об это спотыкались прошлые версии цели: птица шла к ветке напрямую, а пути
     * сквозь крону нет — листва твёрдая. Ворона утыкалась в дерево снизу или сбоку, теряла
     * маршрут, брала новый и наматывала круги, так ни разу и не сев.</p>
     */
    private static final double CRUISE_HEIGHT = 4.0;

    /** Сколько ждать, прежде чем бросить недостижимый насест. */
    private static final int TRAVEL_LIMIT = 300;

    private final PathfinderMob bird;
    private final BooleanSupplier enabled;
    private final IntSupplier range;
    private final IntSupplier sitTicks;
    private @Nullable BlockPos perch;
    private int cooldown;
    private int travel;
    private int sitting;
    private boolean settled;

    public PerchGoal(final PathfinderMob bird, final BooleanSupplier enabled,
                     final IntSupplier range, final IntSupplier sitTicks) {
        this.bird = bird;
        this.enabled = enabled;
        this.range = range;
        this.sitTicks = sitTicks;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.enabled.getAsBoolean() || this.cooldown-- > 0) {
            return false;
        }
        this.cooldown = THINK_INTERVAL;

        // Уже сидит на дереве — незачем срываться и лететь на соседнее.
        if (this.perched()) {
            return false;
        }

        this.perch = findPerch();
        return this.perch != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.perch == null || !this.enabled.getAsBoolean()) {
            return false;
        }
        // Пока летит — держим цель, но не вечно: до кроны за забором можно и не добраться.
        if (!this.settled) {
            return this.travel < TRAVEL_LIMIT;
        }
        // Слетела с ветки (спугнули, ветку сломали) — цель окончена, искать новую.
        return this.perched() && this.sitting < this.sitTicks.getAsInt();
    }

    @Override
    public void start() {
        this.travel = 0;
        this.sitting = 0;
        this.settled = false;
        if (this.perch != null) {
            this.bird.getNavigation().moveTo(
                    this.perch.getX() + 0.5, this.perch.getY() + CRUISE_HEIGHT, this.perch.getZ() + 0.5, 1.0);
        }
    }

    @Override
    public void stop() {
        this.perch = null;
        this.travel = 0;
        this.sitting = 0;
        this.settled = false;
        this.bird.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.perch == null) {
            return;
        }

        double x = this.perch.getX() + 0.5;
        double y = this.perch.getY() + 1.0;
        double z = this.perch.getZ() + 0.5;
        // Высота захода: выше кроны, чтобы садиться сверху вниз.
        double cruise = this.perch.getY() + CRUISE_HEIGHT;

        // Села — значит села: под лапами ветка, дальше птица просто сидит и глядит по
        // сторонам. Проверяем именно опору, а не расстояние до цели: сесть можно и на
        // соседнюю ветку, и это ничем не хуже.
        if (this.perched()) {
            if (!this.settled) {
                this.settled = true;
                this.bird.getNavigation().stop();
                // Притяжение возвращаем на всякий случай: его выключает управление полётом,
                // и сидящей птице оно ни к чему.
                this.bird.setNoGravity(false);
            }
            this.sitting++;
            return;
        }

        this.travel++;

        double dx = this.bird.getX() - x;
        double dz = this.bird.getZ() - z;
        double horizontal = dx * dx + dz * dz;

        // Над самой веткой и выше неё — глушим навигацию и не вмешиваемся.
        //
        // Это и есть посадка. Пока птице задана цель, управление полётом держит ей
        // выключенным притяжение, и она честно висит в сантиметрах над веткой, не в силах
        // коснуться. Стоит цель убрать — притяжение возвращается само, и ворона садится.
        if (horizontal < 1.4 && this.bird.getY() > this.perch.getY() + 0.9) {
            this.bird.getNavigation().stop();
            this.bird.getLookControl().setLookAt(x, y, z);
            return;
        }

        // Вся дорога — обычной навигацией, и целью служит точка **над кроной**, а не сама
        // ветка. В этом всё дело: к ветке пути нет, листва твёрдая, и птица, посланная
        // прямо на неё, утыкается в дерево и наматывает круги. А до открытого неба над
        // деревом путь есть всегда, откуда бы ворона ни летела.
        //
        // Рулить полётом вручную вместо навигации нельзя: с земли она так не взлетает
        // (управление считает скорость по шагу, а не по полёту) — птица подпрыгивала на
        // полтора блока и падала обратно.
        if (this.bird.getNavigation().isDone()
                && !this.bird.getNavigation().moveTo(x, cruise, z, 1.0)) {
            // Пути нет вовсе — насест бросается, ворона занимается своими делами.
            this.perch = null;
            return;
        }
        this.bird.getLookControl().setLookAt(x, cruise, z);
    }

    /** Стоит ли ворона на дереве прямо сейчас. */
    private boolean perched() {
        return this.bird.onGround() && isTree(this.bird.level(), this.bird.blockPosition().below());
    }

    private @Nullable BlockPos findPerch() {
        Level level = this.bird.level();
        BlockPos origin = this.bird.blockPosition();

        List<BlockPos> near = Scan.collect(origin, NEAR_RADIUS, 6, 12, CANDIDATES,
                pos -> isTree(level, pos) && free(level, pos));
        if (!near.isEmpty()) {
            return near.get(this.bird.getRandom().nextInt(near.size()));
        }

        int far = Math.max(NEAR_RADIUS + 1, this.range.getAsInt());
        return Scan.sample(origin, far, 8, 16, FAR_SAMPLES, this.bird.getRandom(),
                pos -> isTree(level, pos) && free(level, pos));
    }

    /** Сесть можно и на бревно, и на листву: ворона держится верхушек, а не стволов. */
    private static boolean isTree(final Level level, final BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES);
    }

    /** Над насестом должно быть пусто — иначе птица «сядет» внутрь кроны. */
    private static boolean free(final Level level, final BlockPos pos) {
        return level.getBlockState(pos.above()).isAir()
                && level.getBlockState(pos.above(2)).isAir();
    }
}
