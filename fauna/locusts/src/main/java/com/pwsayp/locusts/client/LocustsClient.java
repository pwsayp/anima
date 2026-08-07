package com.pwsayp.locusts.client;

import com.pwsayp.locusts.Locusts;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// EntityRenderersEvent живёт на обычной шине (статическое поле BUS), а не на модовой.
@Mod.EventBusSubscriber(modid = Locusts.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LocustsClient {
    public static final ModelLayerLocation LOCUST_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Locusts.MODID, "locust"), "main");

    @SubscribeEvent
    static void onRegisterLayers(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LOCUST_LAYER, LocustModel::createBodyLayer);
    }

    @SubscribeEvent
    static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Locusts.LOCUST.get(), LocustRenderer::new);
    }

    private LocustsClient() {}
}
