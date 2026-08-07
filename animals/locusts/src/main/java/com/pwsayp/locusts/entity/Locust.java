package com.pwsayp.locusts.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;

/**
 * Саранча — одна особь из стаи.
 *
 * <p>Сама по себе она никто: одно сердце, никакого урона, ни к кому не пристаёт. Опасна
 * только толпа, и то не игроку, а его полю.</p>
 *
 * <p><b>Никаких сроков жизни.</b> Прилетевшая особь остаётся в мире, пока её не убьют или
 * пока рядом не перестанет быть людей — и только тогда убирается. Раньше у каждой был
 * обратный отсчёт, и стая таяла на глазах; выглядело это не как конец события, а как
 * поломка мода. Ванильный деспавн по той же причине запрещён — см.
 * {@link #removeWhenFarAway}.</p>
 *
 * <p>Кончается налёт тем, что игрок уходит, отбивается или сживает стаю со свету курами.
 * Само собой поле не очистится.</p>
 *
 * <p>Над водой саранча не летает: широкая вода для неё — стена (см. {@link EatCropGoal}).</p>
 */
public class Locust extends PathfinderMob {
    /** Ест ли прямо сейчас — клиенту нужно, чтобы прижать крылья. */
    private static final EntityDataAccessor<Boolean> DATA_FEEDING =
            SynchedEntityData.defineId(Locust.class, EntityDataSerializers.BOOLEAN);

    /**
     * Дальше этого от игрока особь убирается из мира.
     *
     * <p>Это <b>единственное</b> условие, по которому саранча пропадает сама. Никаких
     * сроков и обратных отсчётов: пока рядом есть человек, особь остаётся в мире, сколько
     * бы она там ни жила. Пропажа на глазах — хоть по таймеру, хоть по случайному
     * броску — читается как поломка мода, а не как конец события.</p>
     */
    private static final double VANISH_DISTANCE = 64.0;

    /** Еды поблизости нет: особь садится на землю и сидит. */
    private boolean idle;

    public Locust(final EntityType<? extends Locust> type, final Level level) {
        super(type, level);
        // hoversInPlace = false: без цели саранча не зависает в воздухе, а опускается на
        // землю. С true она висела столбом, и это выглядело поломкой — да и куры не могли
        // до неё дотянуться.
        this.moveControl = new FlyingMoveControl<>(this, 20, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new EatCropGoal(this));
        this.goalSelector.addGoal(2, new SwarmWanderGoal(this));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(final Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(false);
        return navigation;
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FEEDING, false);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            return;
        }

        // Подчищаем за собой: в одной из сборок отлёт отключал ИИ, и такие особи застывали
        // в воздухе намертво. Те, что успели сохраниться в мир, оживают здесь.
        if (this.isNoAi()) {
            this.setNoAi(false);
        }

        // Попала в воду — выгребает вверх и снимается с места. Утонуть она при этом не
        // может (см. canBreatheUnderwater), но барахтаться в пруду до конца срока — тоже
        // не дело: раз села не туда, пусть взлетает и ищет другое место.
        if (this.isInWater()) {
            this.setIdle(false);
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.08, 0.0));
        }

        // Единственная причина пропасть из мира — рядом не осталось людей. Ни срока, ни
        // обратного отсчёта: саранча живёт, пока её кто-то видит.
        if (this.level().getNearestPlayer(this, VANISH_DISTANCE) == null) {
            this.discard();
        }
    }

    /**
     * Ванильный деспавн саранче запрещён, и это важнее, чем кажется.
     *
     * <p>Саранча числится «окружающей» живностью, а такую игра убирает сама: дальше
     * тридцати двух блоков от игрока каждая особь каждый тик рискует просто перестать
     * существовать. Со стороны это выглядело так, будто особь зависла и вдруг исчезла у
     * тебя на глазах — при том, что до неё было рукой подать.</p>
     *
     * <p>Теперь решение принимает только мод, и правило у него одно: см.
     * {@link #VANISH_DISTANCE}.</p>
     */
    @Override
    public boolean removeWhenFarAway(final double distanceToClosestPlayer) {
        return false;
    }

    /**
     * Нашлось ли этой особи занятие.
     *
     * <p>Стая приходит какая пришла, а еды под ней может не хватить на всех. Лишние не
     * мечутся и не жрут ландшафт по кругу: они садятся на землю и сидят — и это заодно
     * самая дешёвая для сервера половина стаи.</p>
     */
    public boolean isIdle() {
        return this.idle;
    }

    public void setIdle(final boolean idle) {
        this.idle = idle;
    }

    public boolean isFeeding() {
        return this.entityData.get(DATA_FEEDING);
    }

    public void setFeeding(final boolean feeding) {
        this.entityData.set(DATA_FEEDING, feeding);
    }

    /**
     * Захлебнуться саранча не может.
     *
     * <p>Насекомое такого веса держится на плёнке воды и не тонет, а в игре тонущая на
     * ровном месте стая выглядела просто поломкой: садились в пруд у поля и умирали
     * десятками. Садиться на воду особь и так больше не станет (см.
     * {@code Locusts.canLandOn}), но если уж угодила — выгребет, а не задохнётся.</p>
     */
    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    /** Саранча не тонет и не разбивается: она летает. */
    @Override
    public boolean causeFallDamage(final double distance, final float multiplier, final DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(final double y, final boolean onGround,
                                   final net.minecraft.world.level.block.state.BlockState state,
                                   final BlockPos pos) {
    }

    @Override
    public boolean isFlapping() {
        return true;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 40;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BEE_LOOP;
    }

    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return SoundEvents.BEE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BEE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.4F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.35F;
    }

    public ServerLevel serverLevel() {
        return (ServerLevel) this.level();
    }
}
