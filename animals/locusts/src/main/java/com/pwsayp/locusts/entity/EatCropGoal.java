package com.pwsayp.locusts.entity;

import com.pwsayp.anima.world.Claims;
import com.pwsayp.anima.world.Crops;
import com.pwsayp.anima.world.Greenery;
import com.pwsayp.anima.world.Scan;
import com.pwsayp.locusts.Locusts;
import com.pwsayp.locusts.LocustsConfig;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;

/**
 * Ради чего всё и затевалось: саранча объедает поле.
 *
 * <p>Посев не уничтожается, а <b>откатывается на стадию назад</b> — и так до нуля. Поэтому
 * налёт ощущается не как вандализм, а как потеря времени: грядка цела, семена целы, но
 * урожай отодвинулся на неделю. Спелое поле, застигнутое стаей, — самая дорогая потеря, и
 * это правильно: не надо было тянуть со жатвой.</p>
 *
 * <p>Трава и цветы просто исчезают: после стаи остаётся вытравленная полоса.</p>
 *
 * <p>Две ванильные преграды решают всё: под крышей саранча не садится (светит небо или нет,
 * проверяет сама игра) и в дыму костра не садится тоже.</p>
 */
public class EatCropGoal extends Goal {
    /** Ближний обзор: сюда попадает всё, до чего рукой подать. */
    private static final int NEAR_RADIUS = 6;

    /**
     * Дальний обзор: им саранча находит рощу или заросли тростника в стороне.
     *
     * <p>Перебирать такой объём целиком нельзя — это десятки тысяч блоков на особь, — поэтому
     * дальний обзор делается выборкой: {@link #FAR_SAMPLES} случайных точек. Дерево, поле или
     * куст занимают много блоков, так что случайная выборка находит их надёжно, а стоит
     * копейки.</p>
     */
    private static final int FAR_RADIUS = 24;

    private static final int FAR_SAMPLES = 250;

    /** Как часто особь заново оглядывается вблизи. */
    private static final int SEARCH_INTERVAL = 100;

    /**
     * Пауза между укусами: две секунды вместо пяти.
     *
     * <p>Раньше особь после каждого укуса уходила в общий поиск на {@link #SEARCH_INTERVAL}
     * тиков, и за весь налёт успевала укусить полтора десятка раз. Поле от этого не
     * съедалось, а откатывалось: стая улетала, оставив грядки в ростках нулевой стадии,
     * которые отрастали сами за пару дней. Налёт выходил бесплатным.</p>
     */
    private static final int NEXT_PLANT = 40;

    /** Как часто она осматривается вдаль, если рядом пусто. */
    private static final int IDLE_INTERVAL = 200;

    /**
     * С какого расстояния саранча дотягивается до еды.
     *
     * <p>Заметно больше блока: саранча не садится на блок, а зависает рядом с ним, и слишком
     * строгая мерка приводила к тому, что она замирала в полуметре от тростника и не могла
     * до него дотянуться.</p>
     */
    private static final double REACH = 2.2;

    /** Сколько тиков особь пытается долететь до цели, прежде чем плюнуть на неё. */
    private static final int STUCK_LIMIT = 60;

    /** Сколько свободных блоков хватает набрать, чтобы выбрать из них наугад. */
    private static final int CANDIDATES = 24;

    private final Locust locust;
    private @Nullable BlockPos target;
    private int biting;
    private int cooldown;

    /** Сколько тиков подряд особь не приближается к цели. */
    private int stuck;

    /** Насколько близко она была к цели в прошлый раз. */
    private double lastDistance = Double.MAX_VALUE;

    public EatCropGoal(final Locust locust) {
        this.locust = locust;
        this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown-- > 0) {
            return false;
        }

        this.target = findFood();
        if (this.target != null && !Claims.claim(this.locust.serverLevel(), this.target, this.locust.getId())) {
            this.target = null;
        }
        this.cooldown = this.target != null ? SEARCH_INTERVAL : IDLE_INTERVAL;
        this.locust.setIdle(this.target == null);
        this.stuck = 0;
        this.lastDistance = Double.MAX_VALUE;
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null
                && Locusts.isFood(this.locust.level().getBlockState(this.target));
    }

    @Override
    public void stop() {
        if (this.target != null && this.locust.level() instanceof ServerLevel level) {
            Claims.release(level, this.target, this.locust.getId());
        }
        this.target = null;
        this.biting = 0;
        this.stuck = 0;
        this.lastDistance = Double.MAX_VALUE;
        this.locust.setFeeding(false);
        this.locust.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.target == null || !(this.locust.level() instanceof ServerLevel level)) {
            return;
        }

        this.locust.getLookControl().setLookAt(
                this.target.getX() + 0.5, this.target.getY() + 0.5, this.target.getZ() + 0.5);

        double distance = this.locust.position().distanceToSqr(
                this.target.getX() + 0.5, this.target.getY() + 0.5, this.target.getZ() + 0.5);

        if (distance > REACH * REACH) {
            this.locust.setFeeding(false);

            // До цели можно и не долететь: тростник за забором, крона за стеной. Если за
            // STUCK_LIMIT тиков особь не стала ближе — цель бросается и ищется другая.
            // Без этого саранча зависала рядом и «смотрела» на недосягаемый блок.
            if (distance < this.lastDistance - 0.05) {
                this.stuck = 0;
                this.lastDistance = distance;
            } else if (++this.stuck > STUCK_LIMIT) {
                Claims.release(level, this.target, this.locust.getId());
                this.target = null;
                this.cooldown = 20;
                return;
            }

            if (this.locust.getNavigation().isDone()) {
                this.locust.getNavigation().moveTo(
                        this.target.getX() + 0.5, this.target.getY() + 0.6, this.target.getZ() + 0.5, 1.0);
            }
            return;
        }

        this.locust.setFeeding(true);
        if (++this.biting < LocustsConfig.eatTicks) {
            return;
        }

        this.biting = 0;
        bite(level, this.target);

        // Откусила — и дальше: держаться за одно растение особь не обязана, доест его
        // соседка. Важно другое — насколько быстро она берётся за следующее.
        Claims.release(level, this.target, this.locust.getId());
        this.target = null;
        this.cooldown = NEXT_PLANT;
    }

    /**
     * Один укус.
     *
     * <p>Посев сперва теряет стадии роста одну за другой, а с нулевой съедается совсем —
     * вместе с ростком. Только что посаженные всходы для саранчи такая же еда, как спелый
     * колос: после стаи грядка должна остаться пустой, иначе поле отрастёт само и налёт
     * выйдет бесплатным.</p>
     */
    private void bite(final ServerLevel level, final BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!Locusts.isFood(state)) {
            return;
        }

        level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.NEUTRAL, 0.4F, 1.8F);
        level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));

        // Урожай откатывается на стадию назад, а не пропадает: потеряно время, а не грядка.
        // Как именно откатывать, знает ядро — правило общее для всех едоков семейства.
        if (Crops.rollback(level, pos)) {
            return;
        }

        // Откатывать нечего: трава, цветы и сами ростки нулевой стадии исчезают совсем.
        // Ничего не роняем: съеденное съедено, подбирать после стаи нечего.
        level.removeBlock(pos, false);
    }

    /**
     * Что съесть.
     *
     * <p>Сначала смотрим вокруг себя, и если рядом пусто — выборкой вдаль. Второе и есть то,
     * чем стая перебирается с объеденного поля на рощу или в заросли тростника: без дальнего
     * обзора половина стаи просто садилась на голую землю посреди зелёного луга.</p>
     */
    private @Nullable BlockPos findFood() {
        BlockPos near = scanNear();
        return near != null ? near : scanFar();
    }

    /**
     * Ближний обзор.
     *
     * <p>Цель берётся <b>случайная из свободных</b>, а не ближайшая. Это важнее, чем кажется:
     * когда каждая особь выбирала ближайший блок, вся стая сходилась на один и тот же куст,
     * ел его кто-то один, а остальные толклись рядом без дела. Случайный выбор из незанятых
     * растекает стаю по полю.</p>
     */
    private @Nullable BlockPos scanNear() {
        List<BlockPos> found = Scan.collect(
                this.locust.blockPosition(), NEAR_RADIUS, 5, 6, CANDIDATES, this::available);
        if (found.isEmpty()) {
            return null;
        }
        return top(found.get(this.locust.getRandom().nextInt(found.size())));
    }

    /** Дальний обзор выборкой: дёшево и достаточно, чтобы заметить рощу или поле. */
    private @Nullable BlockPos scanFar() {
        BlockPos found = Scan.sample(this.locust.blockPosition(), FAR_RADIUS, 8, 16, FAR_SAMPLES,
                this.locust.getRandom(), this::available);
        return found != null ? top(found) : null;
    }

    /** Съедобно и ещё не занято соседкой. */
    private boolean available(final BlockPos pos) {
        return Locusts.isFood(this.locust.level().getBlockState(pos))
                && Locusts.canLandOn(this.locust.level(), pos)
                && Claims.free(this.locust.serverLevel(), pos, this.locust.getId());
    }

    /**
     * Верхушка растения, если оно стоит столбиком.
     *
     * <p>Тростник, бамбук и кактус едятся сверху вниз: так саранча обгрызает верхний блок, а
     * не подрубает стебель у корня, роняя всё остальное на землю предметами.</p>
     */
    private BlockPos top(final BlockPos pos) {
        BlockState state = this.locust.level().getBlockState(pos);
        if (!Greenery.isStalk(state)) {
            return pos;
        }

        BlockPos above = pos;
        while (this.locust.level().getBlockState(above.above()).is(state.getBlock())) {
            above = above.above();
        }
        return above;
    }
}
