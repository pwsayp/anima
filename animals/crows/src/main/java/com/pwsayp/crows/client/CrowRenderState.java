package com.pwsayp.crows.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class CrowRenderState extends LivingEntityRenderState {
    /** Фаза взмаха в радианах: по ней модель и считает положение крыла. */
    public float wingPhase;

    /** Планирует: крылья разведены и неподвижны. */
    public boolean gliding;

    /** Насколько крик прошёл: 0 — молчит, к единице клюв закрывается. */
    public float cawProgress;

    /**
     * Постоянный сдвиг фазы этой особи.
     *
     * <p>Нужен, чтобы стая не дёргала головой разом. Берётся из номера сущности и потому
     * не меняется: <b>брать его из {@code hashCode()} самого состояния нельзя</b> — объект
     * состояния пересоздаётся, и голова начинает трястись каждый кадр.</p>
     */
    public int phase;

    /** Крен в повороте и наклон в пикировании, в градусах. */
    public float bank;
    public float pitch;

    public boolean flying;
    public boolean pecking;
}
