package com.pwsayp.crows.entity;

import com.pwsayp.anima.bird.BirdFlight;
import com.pwsayp.anima.bird.FleeFromPlayerGoal;
import com.pwsayp.anima.bird.PerchGoal;
import com.pwsayp.crows.Crows;
import com.pwsayp.crows.CrowsConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * Ворона — маленькая пугливая птица, которая садится на почти созревшие грядки
 * и склёвывает их, отбрасывая посев на несколько стадий роста назад.
 *
 * <p>Летающая часть устроена так же, как у ванильного попугая: {@link FlyingMoveControl}
 * плюс {@link FlyingPathNavigation}, покачивание крыльев считается в {@link #aiStep()}
 * и уезжает на клиент через состояние рендера.</p>
 */
public class Crow extends PathfinderMob {
    /** Клюёт ли ворона прямо сейчас — нужно клиенту, чтобы наклонить голову. */
    private static final EntityDataAccessor<Boolean> DATA_PECKING =
            SynchedEntityData.defineId(Crow.class, EntityDataSerializers.BOOLEAN);

    /**
     * Сколько тиков осталось каркать.
     *
     * <p>Уезжает на клиент только ради одного: чтобы на крике раскрывался клюв. Молча
     * каркающая птица выглядит куклой, и никакая проработка модели этого не спасает.</p>
     */
    private static final EntityDataAccessor<Integer> DATA_CAW =
            SynchedEntityData.defineId(Crow.class, EntityDataSerializers.INT);

    /** Сколько тиков держать клюв раскрытым: примерно длина самого длинного карканья. */
    public static final int CAW_TICKS = 18;

    /**
     * Голос вороны звучит как есть.
     *
     * <p>Раньше здесь стояло 0.62: карканья своего не было, и голос собирался из семплов
     * попугая, приспущенных по высоте. Слышалось это не как птица, а как что-то большое в
     * кустах — тон тянет за собой и длину, и тембр. Теперь у мода свои файлы, и трогать их
     * высоту незачем; лёгкий разброс остаётся только чтобы стая не куковала в унисон.</p>
     */
    private static final float VOICE_PITCH = 1.0F;

    /**
     * Скорость взмаха, радиан в тик.
     *
     * <p>Ворона машет редко и глубоко — около трёх взмахов в секунду. Попугай и пчела
     * трепещут вчетверо чаще, и именно от этого полёт вороны выглядел насекомым: дело было
     * не в размахе крыла, а в частоте. Это единственное, чем порода отличается в полёте;
     * вся остальная кинематика общая и живёт в ядре.</p>
     */
    private static final float BEAT_SPEED = 0.95F;

    /** Крен, тангаж, фаза взмаха и планирование — всё считает ядро. */
    public final BirdFlight flight = new BirdFlight(this, BEAT_SPEED);

    public Crow(final EntityType<? extends Crow> type, final Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 10, false);
        this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, -1.0F);
        this.setPathfindingMalus(PathType.FIRE, -1.0F);
        this.setPathfindingMalus(PathType.COCOA, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                // Ванильные 0.5 — это скорость попугая в клетке. Ворона летает вдвое быстрее
                // человека, и без этого никакой полёт не спасёт: медленная птица выглядит
                // не плавной, а вялой.
                .add(Attributes.FLYING_SPEED, 0.95)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5));
        // Приоритет 2: увидел игрока — бросает грядку и улетает.
        this.goalSelector.addGoal(2, new FleeFromPlayerGoal(this, () -> CrowsConfig.scareDistance, 1.6));
        this.goalSelector.addGoal(3, new EatCropGoal(this));
        // Дерево важнее бесцельного полёта: не найдя грядки, ворона идёт не куда глаза
        // глядят, а на ближайшую крону.
        this.goalSelector.addGoal(4, new PerchGoal(this,
                () -> CrowsConfig.perchOnTrees,
                () -> CrowsConfig.perchRange,
                () -> CrowsConfig.perchTicks));
        this.goalSelector.addGoal(5, new SoarGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(final Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_PECKING, false);
        entityData.define(DATA_CAW, 0);
    }

    public boolean isPecking() {
        return this.entityData.get(DATA_PECKING);
    }

    public void setPecking(final boolean pecking) {
        this.entityData.set(DATA_PECKING, pecking);
    }

    public boolean isFlying() {
        return !this.onGround();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.updateWings();

        int caw = this.entityData.get(DATA_CAW);
        if (caw > 0 && !this.level().isClientSide()) {
            this.entityData.set(DATA_CAW, caw - 1);
        }
    }

    /**
     * Каркнула — значит открыла клюв.
     *
     * <p>Ванильный ambient-звук играется сам по таймеру, и единственное надёжное место
     * узнать о крике — этот вызов. Отсюда и счётчик: он же и есть длина открытого
     * клюва.</p>
     */
    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        if (!this.level().isClientSide()) {
            this.entityData.set(DATA_CAW, CAW_TICKS);
        }
    }

    /** Сколько тиков крика осталось — клиенту для раскрытия клюва. */
    public int getCawTicks() {
        return this.entityData.get(DATA_CAW);
    }

    /**
     * Взмахи крыльев по-вороньи.
     *
     * <p>Раньше здесь стояла попугайская механика: частота взмаха бралась от того, летит
     * птица или нет, и разгонялась до трепета. У врановых полёт другой — редкие глубокие
     * взмахи вперемежку с планированием, и узнаётся он именно по ритму.</p>
     *
     * <p>Планирование включается само: если птица снижается и при этом идёт вперёд, крылья
     * замирают разведёнными. Оттуда же берётся мягкое снижение — планирующая ворона падает
     * медленнее, чем камень, но быстрее порхающего попугая.</p>
     */
    private void updateWings() {
        this.flight.tick();

        // Шорох крыла — раз во взмах, на границе фазы. Отдельного семпла у нас нет,
        // поэтому берётся тихий ванильный взмах, поднятый по высоте: на этой громкости он
        // читается как шелест пера, а не как чужая птица.
        if (!this.gliding() && !this.onGround() && !this.level().isClientSide()
                && this.flight.beatCrossed()) {
            this.playSound(SoundEvents.PHANTOM_FLAP, 0.06F, 1.7F + this.random.nextFloat() * 0.2F);
        }
    }

    public boolean gliding() {
        return this.flight.gliding;
    }

    @Override
    protected boolean isFlapping() {
        // Звук взмаха мод играет сам, по фазе крыла: ванильный расчёт завязан на пройденное
        // расстояние и на планировании молчал бы невпопад.
        return false;
    }

    @Override
    protected boolean omnidirectionalAirMover() {
        return true;
    }

    @Override
    protected void checkFallDamage(final double ya, final boolean onGround, final BlockState onState, final BlockPos pos) {
        // Птица. Не разбивается.
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    @Override
    public float getVoicePitch() {
        return VOICE_PITCH + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Crows.CROW_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return Crows.CROW_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Crows.CROW_DEATH.get();
    }

    @Override
    protected void playStepSound(final BlockPos pos, final BlockState blockState) {
        this.playSound(SoundEvents.PARROT_STEP, 0.15F, VOICE_PITCH);
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.5F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
    }

    /** Ванильные условия для пассивного моба: светло и земля, на которой водятся животные. */
    public static boolean checkCrowSpawnRules(
            final EntityType<Crow> type, final LevelAccessor level,
            final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random) {
        boolean brightEnough = EntitySpawnReason.ignoresLightRequirements(spawnReason)
                || level.getRawBrightness(pos, 0) > 8;
        return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON) && brightEnough;
    }
}
