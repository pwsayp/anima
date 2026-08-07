package com.pwsayp.locusts.client;

import com.pwsayp.locusts.Locusts;
import com.pwsayp.locusts.entity.Locust;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class LocustRenderer extends MobRenderer<Locust, LocustRenderState, LocustModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Locusts.MODID, "textures/entity/locust.png");

    public LocustRenderer(final EntityRendererProvider.Context context) {
        super(context, new LocustModel(context.bakeLayer(LocustsClient.LOCUST_LAYER)), 0.2F);
    }

    @Override
    public Identifier getTextureLocation(final LocustRenderState state) {
        return TEXTURE;
    }

    @Override
    public LocustRenderState createRenderState() {
        return new LocustRenderState();
    }

    @Override
    public void extractRenderState(final Locust locust, final LocustRenderState state, final float partialTicks) {
        super.extractRenderState(locust, state, partialTicks);
        state.feeding = locust.isFeeding();
    }
}
