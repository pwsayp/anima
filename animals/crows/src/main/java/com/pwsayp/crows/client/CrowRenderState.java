package com.pwsayp.crows.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class CrowRenderState extends LivingEntityRenderState {
    /** Фаза взмаха в радианах: по ней модель и считает положение крыла. */
    public float wingPhase;

    /** Планирует: крылья разведены и неподвижны. */
    public boolean gliding;

    public boolean flying;
    public boolean pecking;
}
