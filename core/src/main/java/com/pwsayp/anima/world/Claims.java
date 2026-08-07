package com.pwsayp.anima.world;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Кто какой блок уже занял.
 *
 * <p>Без этого любая толпа бесполезна, и это выясняется одинаково в каждом моде: каждая
 * особь ищет <b>ближайшую</b> добычу, все находят одну и ту же, толпятся вокруг, и
 * достаётся она кому-то одному. Со стороны выглядит как «зверья много, а толку ноль» — так
 * оно и есть.</p>
 *
 * <p>Здесь блок можно занять, и занятый чужой блок в выбор не попадает. Толпа растекается по
 * полю вместо того, чтобы сходиться в точку.</p>
 *
 * <p>Заявка живёт в памяти сервера и протухает сама: если зверь погиб или ушёл, его блок
 * через {@link #EXPIRY} тиков снова свободен. Ничего не сохраняется — после перезапуска
 * никакой толпы всё равно нет. Мести карту вручную не надо, ядро делает это само раз в
 * секунду.</p>
 */
public final class Claims {
    /** Через сколько тиков брошенная заявка перестаёт считаться. */
    private static final int EXPIRY = 200;

    private static final Map<Key, Claim> CLAIMS = new HashMap<>();

    private record Key(ResourceKey<Level> dimension, BlockPos pos) {}

    private static final class Claim {
        int owner;
        long touched;
    }

    private Claims() {}

    /** Занять блок под себя. Вернёт {@code false}, если он уже за кем-то. */
    public static boolean claim(final ServerLevel level, final BlockPos pos, final int owner) {
        Key key = new Key(level.dimension(), pos.immutable());
        Claim claim = CLAIMS.get(key);

        if (claim != null && claim.owner != owner && level.getGameTime() - claim.touched < EXPIRY) {
            return false;
        }

        Claim mine = claim != null ? claim : new Claim();
        mine.owner = owner;
        mine.touched = level.getGameTime();
        CLAIMS.put(key, mine);
        return true;
    }

    /** Свободен ли блок для этого зверя. */
    public static boolean free(final ServerLevel level, final BlockPos pos, final int owner) {
        Claim claim = CLAIMS.get(new Key(level.dimension(), pos.immutable()));
        return claim == null || claim.owner == owner || level.getGameTime() - claim.touched >= EXPIRY;
    }

    /** Отпустить блок: доел, бросил или ушёл. */
    public static void release(final ServerLevel level, final BlockPos pos, final int owner) {
        Key key = new Key(level.dimension(), pos.immutable());
        Claim claim = CLAIMS.get(key);
        if (claim != null && claim.owner == owner) {
            CLAIMS.remove(key);
        }
    }

    /** Выметаем протухшее — иначе карта росла бы весь налёт. Зовётся ядром раз в секунду. */
    public static void sweep(final MinecraftServer server) {
        if (CLAIMS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<Key, Claim>> iterator = CLAIMS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Key, Claim> entry = iterator.next();
            ServerLevel level = server.getLevel(entry.getKey().dimension());
            if (level == null || level.getGameTime() - entry.getValue().touched > EXPIRY) {
                iterator.remove();
            }
        }
    }
}
