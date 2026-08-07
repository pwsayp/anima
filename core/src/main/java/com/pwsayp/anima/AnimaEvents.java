package com.pwsayp.anima;

import com.pwsayp.anima.world.Claims;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Хозяйственные тики ядра.
 *
 * <p>Пока тут одно дело: раз в секунду выметать протухшие заявки на блоки. Делает это ядро,
 * а не моды, — иначе каждый мод заводил бы свой обработчик тика ради одной и той же уборки,
 * и мести карту они начали бы по очереди.</p>
 */
@Mod.EventBusSubscriber(modid = Anima.MODID)
public final class AnimaEvents {
    private AnimaEvents() {}

    @SubscribeEvent
    static void onServerTick(final TickEvent.ServerTickEvent.Post event) {
        if (event.server().getTickCount() % 20 == 0) {
            Claims.sweep(event.server());
        }
    }
}
