package com.pwsayp.anima.bird;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Как птица держится в воздухе: крен, тангаж, взмах и планирование.
 *
 * <p>Это чистая кинематика, и она у всех птиц одна. Как считается заваливание на крыло из
 * скорости поворота — вопрос не породы, а физики; ворона, стриж и гриф кренятся по одному
 * закону. Отличаются только числа: частота взмаха у воробья вчетверо выше вороньей.
 * Поэтому здесь механика, а порода задаёт единственный параметр — частоту.</p>
 *
 * <p>Держится состоянием сущности, а не наследованием: птица заводит поле и зовёт
 * {@link #tick()} из своего {@code aiStep}. Тикать надо <b>на обеих сторонах</b> — на
 * клиенте от этого зависит анимация.</p>
 *
 * <p>Что даёт крен, стоит сказать прямо: без него птица меняет курс, оставаясь
 * горизонтальной, и летит как вертолёт. Никакая скорость и никакая модель этого не
 * исправят — вираж узнаётся именно по завалу на крыло.</p>
 */
public final class BirdFlight {
    /** Насколько круто крен отзывается на поворот и до какого предела доходит. */
    private static final float BANK_PER_DEGREE = 3.0F;
    private static final float BANK_LIMIT = 42.0F;

    /** То же для тангажа, но от вертикальной скорости. */
    private static final float PITCH_PER_SPEED = 110.0F;
    private static final float PITCH_DOWN_LIMIT = -35.0F;
    private static final float PITCH_UP_LIMIT = 40.0F;

    /** Сглаживание: без него крен дёргается вслед за рывками навигации. */
    private static final float SMOOTHING = 0.18F;

    /** На планировании фаза почти стоит: крылья разведены и держат воздух. */
    private static final float GLIDE_SPEED = 0.04F;

    private final LivingEntity bird;
    private final float beatSpeed;

    /** Фаза взмаха в радианах и её значение в прошлом тике — клиенту для плавности. */
    public float wingPhase;
    public float oWingPhase;

    /** Крен в вираже и наклон в пикировании, в градусах, со значениями прошлого тика. */
    public float bank;
    public float oBank;
    public float pitch;
    public float oPitch;

    /** Снижается ходом, не тратя взмахов. */
    public boolean gliding;

    /**
     * @param beatSpeed скорость взмаха в радианах за тик. Ворона машет редко и глубоко —
     *                  около 0.95, то есть три взмаха в секунду; мелкая птица заметно чаще.
     *                  Именно частота, а не размах, отличает птицу от насекомого.
     */
    public BirdFlight(final LivingEntity bird, final float beatSpeed) {
        this.bird = bird;
        this.beatSpeed = beatSpeed;
    }

    public void tick() {
        this.oWingPhase = this.wingPhase;
        this.oBank = this.bank;
        this.oPitch = this.pitch;

        Vec3 movement = this.bird.getDeltaMovement();
        boolean airborne = !this.bird.onGround() && !this.bird.isPassenger();

        // Планирование включается само: снижается и при этом идёт вперёд — значит скользит.
        this.gliding = airborne && movement.y < -0.02 && movement.horizontalDistanceSqr() > 0.0025;

        float turn = airborne ? Mth.degreesDifference(this.bird.yRotO, this.bird.getYRot()) : 0.0F;
        this.bank = Mth.lerp(SMOOTHING, this.bank,
                Mth.clamp(turn * BANK_PER_DEGREE, -BANK_LIMIT, BANK_LIMIT));

        float vertical = airborne ? (float) movement.y : 0.0F;
        this.pitch = Mth.lerp(SMOOTHING * 0.85F, this.pitch,
                Mth.clamp(-vertical * PITCH_PER_SPEED, PITCH_DOWN_LIMIT, PITCH_UP_LIMIT));

        // Набор высоты стоит взмахов, снижение — нет.
        float climbing = movement.y > 0.03 ? 1.25F : 1.0F;
        this.wingPhase += !airborne ? 0.0F : (this.gliding ? GLIDE_SPEED : this.beatSpeed * climbing);

        // Мягкое снижение: планирующая птица падает медленнее камня, но быстрее
        // порхающего насекомого.
        if (airborne && movement.y < 0.0) {
            this.bird.setDeltaMovement(movement.multiply(1.0, this.gliding ? 0.85 : 0.75, 1.0));
        }
    }

    /** Прошла ли фаза взмаха через границу — по этому играется шорох крыла. */
    public boolean beatCrossed() {
        return (int) (this.oWingPhase / Mth.TWO_PI) != (int) (this.wingPhase / Mth.TWO_PI);
    }
}
