package com.pwsayp.crows.client;

import com.pwsayp.crows.Crows;
import com.pwsayp.crows.entity.Crow;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class CrowRenderer extends MobRenderer<Crow, CrowRenderState, CrowModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Crows.MODID, "textures/entity/crow.png");

    public CrowRenderer(final EntityRendererProvider.Context context) {
        super(context, new CrowModel(context.bakeLayer(CrowsClient.CROW_LAYER)), 0.25F);
    }

    @Override
    public Identifier getTextureLocation(final CrowRenderState state) {
        return TEXTURE;
    }

    @Override
    public CrowRenderState createRenderState() {
        return new CrowRenderState();
    }

    @Override
    public void extractRenderState(final Crow crow, final CrowRenderState state, final float partialTicks) {
        super.extractRenderState(crow, state, partialTicks);
        state.wingPhase = Mth.lerp(partialTicks, crow.oWingPhase, crow.wingPhase);
        state.gliding = crow.gliding;
        state.flying = crow.isFlying();
        state.pecking = crow.isPecking();
    }
}
