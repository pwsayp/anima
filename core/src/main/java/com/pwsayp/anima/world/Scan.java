package com.pwsayp.anima.world;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

import org.jspecify.annotations.Nullable;

/**
 * Как зверь оглядывается вокруг.
 *
 * <p>Тут две разные вещи, и путать их дорого. <b>Вблизи</b> блоки перебираются честно:
 * объём маленький, а пропустить куст под носом нельзя. <b>Вдаль</b> честный перебор
 * невозможен — радиус в двадцать четыре блока это десятки тысяч чтений на одну особь, а
 * особей в стае полторы сотни. Поэтому дальний обзор делается выборкой случайных точек:
 * дерево, поле или заросли занимают много блоков, так что выборка находит их надёжно, а
 * стоит копейки.</p>
 *
 * <p>Класс намеренно ничего не знает про мир: он работает с координатами и условием.
 * Читает блоки вызывающая сторона — ей виднее, что считать подходящим.</p>
 */
public final class Scan {
    private Scan() {}

    /**
     * Честный перебор вокруг точки.
     *
     * <p>Набирает не больше {@code limit} подходящих мест и на этом останавливается: цель
     * потом всё равно берётся случайная, а перебирать поле до последнего колоса незачем.</p>
     */
    public static List<BlockPos> collect(final BlockPos origin, final int radius, final int down,
                                         final int up, final int limit, final Predicate<BlockPos> fits) {
        List<BlockPos> found = new ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-radius, -down, -radius), origin.offset(radius, up, radius))) {
            if (fits.test(pos)) {
                found.add(pos.immutable());
                if (found.size() >= limit) {
                    break;
                }
            }
        }
        return found;
    }

    /**
     * Дальний обзор выборкой: дёшево и достаточно, чтобы заметить рощу или поле.
     *
     * <p>Возвращает первую подошедшую точку из {@code samples} случайных — не ближайшую.
     * Это не небрежность, а нужное свойство: когда каждая особь шла к ближайшему, вся стая
     * сходилась в одну точку.</p>
     */
    public static @Nullable BlockPos sample(final BlockPos origin, final int radius, final int down,
                                            final int up, final int samples, final RandomSource random,
                                            final Predicate<BlockPos> fits) {
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        int height = down + up + 1;

        for (int i = 0; i < samples; i++) {
            probe.set(
                    origin.getX() + random.nextInt(radius * 2 + 1) - radius,
                    origin.getY() + random.nextInt(height) - down,
                    origin.getZ() + random.nextInt(radius * 2 + 1) - radius);
            if (fits.test(probe)) {
                return probe.immutable();
            }
        }
        return null;
    }
}
