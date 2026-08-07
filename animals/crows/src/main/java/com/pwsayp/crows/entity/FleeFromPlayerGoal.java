package com.pwsayp.crows.entity;

import java.util.EnumSet;

import com.pwsayp.crows.CrowsConfig;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Ворона замечает подошедшего игрока, срывается с места и улетает прочь.
 *
 * <p>Ванильный {@code AvoidEntityGoal} здесь не подходит: он проверяет цель через
 * {@code TargetingConditions.forCombat()}, а тот требует {@code canAttack()} — то есть
 * игрок в креативе (он неуязвим) для него вообще не существует. Плюс он ищет позицию
 * для отхода по земле, а нам нужно, чтобы птица уходила по воздуху и вверх.</p>
 */
public class FleeFromPlayerGoal extends Goal {
    private static final double CHASE_MARGIN = 1.5;

    private final Crow crow;
    private final double speedModifier;
    private @Nullable Player scaredOf;

    public FleeFromPlayerGoal(final Crow crow, final double speedModifier) {
        this.crow = crow;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (CrowsConfig.scareDistance <= 0.0) {
            return false;
        }

        this.scaredOf = this.findNearbyPlayer(CrowsConfig.scareDistance);
        if (this.scaredOf == null) {
            return false;
        }

        Vec3 escape = this.findEscapePosition(this.scaredOf);
        if (escape == null) {
            return false;
        }

        return this.crow.getNavigation().moveTo(escape.x, escape.y, escape.z, this.speedModifier);
    }

    @Override
    public boolean canContinueToUse() {
        // Держим цель, пока летим прочь либо пока игрок продолжает идти следом.
        return !this.crow.getNavigation().isDone()
                || this.findNearbyPlayer(CrowsConfig.scareDistance * CHASE_MARGIN) != null;
    }

    @Override
    public void start() {
        this.crow.setPecking(false);
        // Резкий взлёт: птица не «уходит шагом», а вспархивает.
        if (this.crow.onGround()) {
            this.crow.setDeltaMovement(this.crow.getDeltaMovement().add(0.0, 0.32, 0.0));
        }
    }

    @Override
    public void stop() {
        this.scaredOf = null;
        this.crow.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.scaredOf == null) {
            return;
        }

        // Подошли совсем вплотную — прибавляем ходу и ищем новую точку отхода.
        if (this.crow.distanceToSqr(this.scaredOf) < 16.0 && this.crow.getNavigation().isDone()) {
            Vec3 escape = this.findEscapePosition(this.scaredOf);
            if (escape != null) {
                this.crow.getNavigation().moveTo(escape.x, escape.y, escape.z, this.speedModifier);
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
        for (Player player : this.crow.level().players()) {
            if (player.isSpectator() || player.isCreative() || !player.isAlive()) {
                continue;
            }

            double distanceSqr = player.distanceToSqr(this.crow);
            if (distanceSqr < bestDistanceSqr) {
                bestDistanceSqr = distanceSqr;
                nearest = player;
            }
        }

        return nearest;
    }

    /** Точка в воздухе в стороне от игрока — та же математика, что у полёта-блуждания. */
    private @Nullable Vec3 findEscapePosition(final Player from) {
        Vec3 away = this.crow.position().subtract(from.position());
        if (away.horizontalDistanceSqr() < 1.0E-4) {
            away = this.crow.getViewVector(0.0F);
        }
        away = away.normalize();

        Vec3 hovering = HoverRandomPos.getPos(this.crow, 12, 7, away.x, away.z, (float) (Math.PI / 2), 5, 2);
        return hovering != null
                ? hovering
                : AirAndWaterRandomPos.getPos(this.crow, 12, 5, -1, away.x, away.z, (float) (Math.PI / 2));
    }
}
