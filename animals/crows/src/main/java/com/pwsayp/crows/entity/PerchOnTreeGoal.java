package com.pwsayp.crows.entity;

import java.util.EnumSet;
import java.util.List;

import com.pwsayp.anima.world.Scan;
import com.pwsayp.crows.CrowsConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;

/**
 * Ворона предпочитает сидеть на дереве.
 *
 * <p>Без этой цели птица вела себя как чайка на пляже: бесцельно летала над поляной и
 * садилась где придётся, чаще всего посреди открытого места. Живая ворона так не делает —
 * она держится верхушек: оттуда видно и поле, и человека, идущего к полю.</p>
 *
 * <p>Приоритет у цели выше, чем у бесцельного полёта, но ниже, чем у грядки и испуга.
 * Порядок получается тот, что нужен: увидела человека — прочь; увидела спелый колос — на
 * колос; больше ничего интересного — на ближайшую крону, а не куда глаза глядят.</p>
 *
 * <p>Насест ищется двумя способами. Вблизи честно перебираются блоки, вдаль идёт выборка
 * случайных точек — и то и другое взято из ядра, потому что перебирать куб со стороной в
 * три десятка блоков на каждую птицу нельзя.</p>
 */
public class PerchOnTreeGoal extends Goal {
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

    private final Crow crow;
    private @Nullable BlockPos perch;
    private int cooldown;
    private int travel;
    private int sitting;
    private boolean settled;

    public PerchOnTreeGoal(final Crow crow) {
        this.crow = crow;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!CrowsConfig.perchOnTrees || this.cooldown-- > 0) {
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
        if (this.perch == null || !CrowsConfig.perchOnTrees) {
            return false;
        }
        // Пока летит — держим цель, но не вечно: до кроны за забором можно и не добраться.
        if (!this.settled) {
            return this.travel < TRAVEL_LIMIT;
        }
        // Слетела с ветки (спугнули, ветку сломали) — цель окончена, искать новую.
        return this.perched() && this.sitting < CrowsConfig.perchTicks;
    }

    @Override
    public void start() {
        this.travel = 0;
        this.sitting = 0;
        this.settled = false;
        if (this.perch != null) {
            this.crow.getNavigation().moveTo(
                    this.perch.getX() + 0.5, this.perch.getY() + CRUISE_HEIGHT, this.perch.getZ() + 0.5, 1.0);
        }
    }

    @Override
    public void stop() {
        this.perch = null;
        this.travel = 0;
        this.sitting = 0;
        this.settled = false;
        this.crow.getNavigation().stop();
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
                this.crow.getNavigation().stop();
                // Притяжение возвращаем на всякий случай: его выключает управление полётом,
                // и сидящей птице оно ни к чему.
                this.crow.setNoGravity(false);
            }
            this.sitting++;
            return;
        }

        this.travel++;

        double dx = this.crow.getX() - x;
        double dz = this.crow.getZ() - z;
        double horizontal = dx * dx + dz * dz;

        // Над самой веткой и выше неё — глушим навигацию и не вмешиваемся.
        //
        // Это и есть посадка. Пока птице задана цель, управление полётом держит ей
        // выключенным притяжение, и она честно висит в сантиметрах над веткой, не в силах
        // коснуться. Стоит цель убрать — притяжение возвращается само, и ворона садится.
        if (horizontal < 1.4 && this.crow.getY() > this.perch.getY() + 0.9) {
            this.crow.getNavigation().stop();
            this.crow.getLookControl().setLookAt(x, y, z);
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
        if (this.crow.getNavigation().isDone()
                && !this.crow.getNavigation().moveTo(x, cruise, z, 1.0)) {
            // Пути нет вовсе — насест бросается, ворона занимается своими делами.
            this.perch = null;
            return;
        }
        this.crow.getLookControl().setLookAt(x, cruise, z);
    }

    /** Стоит ли ворона на дереве прямо сейчас. */
    private boolean perched() {
        return this.crow.onGround() && isTree(this.crow.level(), this.crow.blockPosition().below());
    }

    private @Nullable BlockPos findPerch() {
        Level level = this.crow.level();
        BlockPos origin = this.crow.blockPosition();

        List<BlockPos> near = Scan.collect(origin, NEAR_RADIUS, 6, 12, CANDIDATES,
                pos -> isTree(level, pos) && free(level, pos));
        if (!near.isEmpty()) {
            return near.get(this.crow.getRandom().nextInt(near.size()));
        }

        int far = Math.max(NEAR_RADIUS + 1, CrowsConfig.perchRange);
        return Scan.sample(origin, far, 8, 16, FAR_SAMPLES, this.crow.getRandom(),
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
