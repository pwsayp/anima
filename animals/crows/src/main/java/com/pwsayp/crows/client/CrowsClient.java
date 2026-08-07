package com.pwsayp.crows.client;

import com.pwsayp.crows.Crows;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// EntityRenderersEvent живёт на обычной шине (статическое поле BUS), а не на модовой.
@Mod.EventBusSubscriber(modid = Crows.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CrowsClient {
    public static final ModelLayerLocation CROW_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Crows.MODID, "crow"), "main");

    @SubscribeEvent
    static void onRegisterLayers(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CROW_LAYER, CrowModel::createBodyLayer);
    }

    @SubscribeEvent
    static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Crows.CROW.get(), CrowRenderer::new);
    }

    private CrowsClient() {}
}
