package com.pwsayp.crows.client;

import com.pwsayp.crows.Crows;
import com.pwsayp.crows.CrowsConfig;
import com.pwsayp.crows.entity.Crow;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Раскраску — чёрная ворона или серая — выбирает конфиг, и выбирает один раз, при создании
 * рендерера. Смена на лету не поддерживается намеренно: текстура берётся при загрузке
 * ресурсов, и проверять флаг каждый кадр было бы дороже, чем перезапустить игру.
 */
public class CrowRenderer extends MobRenderer<Crow, CrowRenderState, CrowModel> {
    private final Identifier texture;

    public CrowRenderer(final EntityRendererProvider.Context context) {
        super(context, new CrowModel(context.bakeLayer(CrowsClient.CROW_LAYER)), 0.25F);
        this.texture = Identifier.fromNamespaceAndPath(Crows.MODID,
                CrowsConfig.hooded ? "textures/entity/crow_hooded.png" : "textures/entity/crow.png");
    }

    @Override
    public Identifier getTextureLocation(final CrowRenderState state) {
        return this.texture;
    }

    @Override
    public CrowRenderState createRenderState() {
        return new CrowRenderState();
    }

    @Override
    public void extractRenderState(final Crow crow, final CrowRenderState state, final float partialTicks) {
        super.extractRenderState(crow, state, partialTicks);
        state.wingPhase = Mth.lerp(partialTicks, crow.flight.oWingPhase, crow.flight.wingPhase);
        state.gliding = crow.gliding();
        state.flying = crow.isFlying();
        state.pecking = crow.isPecking();
        state.phase = crow.getId() % 23;
        state.bank = Mth.lerp(partialTicks, crow.flight.oBank, crow.flight.bank);
        state.pitch = Mth.lerp(partialTicks, crow.flight.oPitch, crow.flight.pitch);
        // Счётчик идёт вниз, а прогресс должен расти: так модель закрывает клюв к концу крика.
        state.cawProgress = crow.getCawTicks() <= 0
                ? 0.0F
                : 1.0F - (crow.getCawTicks() - partialTicks) / Crow.CAW_TICKS;
    }
}
