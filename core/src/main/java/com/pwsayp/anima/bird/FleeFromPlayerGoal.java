package com.pwsayp.anima.bird;

import java.util.EnumSet;

import java.util.function.DoubleSupplier;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Птица замечает подошедшего игрока, срывается с места и улетает прочь.
 *
 * <p>Ванильный {@code AvoidEntityGoal} здесь не подходит: он проверяет цель через
 * {@code TargetingConditions.forCombat()}, а тот требует {@code canAttack()} — то есть
 * игрок в креативе (он неуязвим) для него вообще не существует. Плюс он ищет позицию
 * для отхода по земле, а нам нужно, чтобы птица уходила по воздуху и вверх.</p>
 */
public class FleeFromPlayerGoal extends Goal {
    private static final double CHASE_MARGIN = 1.5;

    private final PathfinderMob bird;
    private final DoubleSupplier distance;
    private final double speedModifier;
    private @Nullable Player scaredOf;

    public FleeFromPlayerGoal(final PathfinderMob bird, final DoubleSupplier distance,
                              final double speedModifier) {
        this.bird = bird;
        this.distance = distance;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.distance.getAsDouble() <= 0.0) {
            return false;
        }

        this.scaredOf = this.findNearbyPlayer(this.distance.getAsDouble());
        if (this.scaredOf == null) {
            return false;
        }

        Vec3 escape = this.findEscapePosition(this.scaredOf);
        if (escape == null) {
            return false;
        }

        return this.bird.getNavigation().moveTo(escape.x, escape.y, escape.z, this.speedModifier);
    }

    @Override
    public boolean canContinueToUse() {
        // Держим цель, пока летим прочь либо пока игрок продолжает идти следом.
        return !this.bird.getNavigation().isDone()
                || this.findNearbyPlayer(this.distance.getAsDouble() * CHASE_MARGIN) != null;
    }

    @Override
    public void start() {
        // Резкий взлёт: птица не «уходит шагом», а вспархивает. Своё занятие она бросает
        // сама: цель испуга держит флаг MOVE, и промысловая цель этим же и прерывается.
        if (this.bird.onGround()) {
            this.bird.setDeltaMovement(this.bird.getDeltaMovement().add(0.0, 0.32, 0.0));
        }
    }

    @Override
    public void stop() {
        this.scaredOf = null;
        this.bird.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.scaredOf == null) {
            return;
        }

        // Подошли совсем вплотную — прибавляем ходу и ищем новую точку отхода.
        if (this.bird.distanceToSqr(this.scaredOf) < 16.0 && this.bird.getNavigation().isDone()) {
            Vec3 escape = this.findEscapePosition(this.scaredOf);
            if (escape != null) {
                this.bird.getNavigation().moveTo(escape.x, escape.y, escape.z, this.speedModifier);
            }
        }
    }

    private @Nullable Player findNearbyPlayer(final double radius) {
        double bestDistanceSqr = radius * radius;
        Player nearest = null;

        // Перебираем сами, а не через TargetingConditions: те завязаны на canAttack(),
        // и для летающего пассивного моба это лишние ворота, из-за которых цель
        // молча не срабатывала. Креатив и зрителей отсекаем явно — как в ванили,
        // где пугливые мобы не замечают игрока в этих режимах.
        for (Player player : this.bird.level().players()) {
            if (player.isSpectator() || player.isCreative() || !player.isAlive()) {
                continue;
            }

            double distanceSqr = player.distanceToSqr(this.bird);
            if (distanceSqr < bestDistanceSqr) {
                bestDistanceSqr = distanceSqr;
                nearest = player;
            }
        }

        return nearest;
    }

    /** Точка в воздухе в стороне от игрока — та же математика, что у полёта-блуждания. */
    private @Nullable Vec3 findEscapePosition(final Player from) {
        Vec3 away = this.bird.position().subtract(from.position());
        if (away.horizontalDistanceSqr() < 1.0E-4) {
            away = this.bird.getViewVector(0.0F);
        }
        away = away.normalize();

        Vec3 hovering = HoverRandomPos.getPos(this.bird, 12, 7, away.x, away.z, (float) (Math.PI / 2), 5, 2);
        return hovering != null
                ? hovering
                : AirAndWaterRandomPos.getPos(this.bird, 12, 5, -1, away.x, away.z, (float) (Math.PI / 2));
    }
}
