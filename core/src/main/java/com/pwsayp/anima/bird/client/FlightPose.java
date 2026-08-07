package com.pwsayp.anima.bird.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Поза птицы в полёте: крен корпусом, ровная голова, хвост рулём, взмах крыла.
 *
 * <p>Всё, что здесь есть, одинаково у любой летающей птицы, и переписывать это в каждой
 * модели незачем. Породе остаются только числа: у кого шире размах взмаха, у кого длиннее
 * хвост.</p>
 *
 * <p>Класс клиентский и на сервере не грузится — как и всё остальное в {@code client}.</p>
 */
public final class FlightPose {
    private FlightPose() {}

    /**
     * Крен и тангаж — на весь корпус разом.
     *
     * <p>Именно разом, а не по частям: птица в вираже заваливается целиком, вместе с
     * хвостом и лапами. Голова при этом крен <b>не разделяет</b> — она держится по
     * горизонту, глядя туда, куда птица летит. После самого крена это самая заметная
     * примета живого полёта.</p>
     */
    public static void body(final ModelPart root, final ModelPart head,
                            final float bankDegrees, final float pitchDegrees) {
        root.zRot = bankDegrees * Mth.DEG_TO_RAD;
        root.xRot = pitchDegrees * Mth.DEG_TO_RAD;

        head.zRot = -bankDegrees * Mth.DEG_TO_RAD * 0.65F;
        head.xRot = -pitchDegrees * Mth.DEG_TO_RAD * 0.5F;
    }

    /**
     * Хвост рулём: в вираже разворачивается против крена, на выходе из пикирования
     * подбирается вниз тормозом.
     */
    public static void tail(final ModelPart tail, final float bankDegrees, final float pitchDegrees) {
        tail.yRot = bankDegrees * Mth.DEG_TO_RAD * 0.35F;
        tail.xRot += Math.max(0.0F, pitchDegrees) * Mth.DEG_TO_RAD * 0.4F;
    }

    /**
     * Взмах: глубокий мах от заметно выше спины до заметно ниже брюха, с гребком вперёд.
     *
     * <p>Глубина и редкость — то, чем птичий полёт отличается от трепета насекомого.
     * Возвращает подскок корпуса, который вызывающая сторона добавляет к частям: на махе
     * вниз птица приподнимается.</p>
     */
    public static float flap(final ModelPart left, final ModelPart right,
                             final float wingPhase, final float amplitude) {
        float beat = Mth.sin(wingPhase);
        left.zRot = -0.3F - beat * amplitude;
        right.zRot = 0.3F + beat * amplitude;
        left.yRot = -beat * 0.12F;
        right.yRot = beat * 0.12F;
        return Mth.cos(wingPhase) * 0.25F;
    }

    /** Планирование: крылья разведены, неподвижны и чуть отведены вперёд. */
    public static void glide(final ModelPart left, final ModelPart right) {
        left.zRot = -0.12F;
        right.zRot = 0.12F;
        left.yRot = -0.15F;
        right.yRot = 0.15F;
    }

    /** Сложенные крылья: сидящая птица не растопыривается. */
    public static void fold(final ModelPart left, final ModelPart right) {
        left.zRot = -0.02F;
        right.zRot = 0.02F;
        left.yRot = 0.0F;
        right.yRot = 0.0F;
    }
}
