package com.pwsayp.anima.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import org.jspecify.annotations.Nullable;

/**
 * Посевы: как узнать стадию роста и как отобрать её обратно.
 *
 * <p>Общий ответ на вопрос «что делает зверь с грядкой». Правильный ответ — <b>откатывает
 * на стадию назад</b>, а не уничтожает: грядка цела, семена целы, потеряно время. Налёт
 * тогда ощущается не как вандализм, а как отодвинутый на неделю урожай, и спелое поле,
 * застигнутое зверьём, — самая дорогая потеря. Так и должно быть: не надо было тянуть со
 * жатвой.</p>
 *
 * <p>Правило общее для всего семейства: вороны, саранча и прочие едоки должны обходиться с
 * чужим трудом одинаково, иначе игрок не сможет предсказать цену налёта.</p>
 */
public final class Crops {
    private Crops() {}

    /**
     * Стадия роста у посева, если она есть.
     *
     * <p>Свойство берётся по имени, а не у класса блока: {@code getAgeProperty} у ванильного
     * {@code CropBlock} закрыт, да и модовые культуры не обязаны от него наследоваться.
     * Ванильные восемь стадий (пшеница, морковь, картошка) и четыре (свёкла, адский нарост)
     * покрывают и всё, что пишут по их образцу.</p>
     */
    public static @Nullable IntegerProperty age(final BlockState state) {
        if (state.hasProperty(BlockStateProperties.AGE_7)) {
            return BlockStateProperties.AGE_7;
        }
        if (state.hasProperty(BlockStateProperties.AGE_3)) {
            return BlockStateProperties.AGE_3;
        }
        if (state.hasProperty(BlockStateProperties.AGE_2)) {
            return BlockStateProperties.AGE_2;
        }
        return null;
    }

    /**
     * Насколько посев вырос: текущая стадия или {@code -1}, если это вообще не посев.
     *
     * <p>Нужна всем, кто выбирает, что клевать: у ворон интерес просыпается к почти
     * налившемуся колосу, а не к ростку.</p>
     */
    public static int stage(final BlockState state) {
        IntegerProperty age = age(state);
        return age == null ? -1 : state.getValue(age);
    }

    /**
     * Сколько всего стадий у этого посева, или {@code -1}, если это не посев.
     *
     * <p>Нужна, чтобы «только спелое» значило одно и то же для любой культуры: у пшеницы
     * спелость это семь, у свёклы три. Правило вида «клевать с седьмой стадии» без этой
     * поправки просто отменяло бы свёклу вовсе.</p>
     */
    public static int maxStage(final BlockState state) {
        IntegerProperty age = age(state);
        if (age == null) {
            return -1;
        }

        int max = -1;
        for (int value : age.getPossibleValues()) {
            max = Math.max(max, value);
        }
        return max;
    }

    /** Дозрел ли посев до конца. */
    public static boolean isRipe(final BlockState state) {
        int max = maxStage(state);
        return max >= 0 && stage(state) >= max;
    }

    /**
     * Откатить посев на одну стадию назад.
     *
     * <p>Вернёт {@code false}, если откатывать нечего — блок не посев или уже на нуле. Тогда
     * решает вызывающая сторона: одному зверю положено сожрать ростки совсем, другому —
     * оставить их в покое.</p>
     */
    public static boolean rollback(final ServerLevel level, final BlockPos pos) {
        return rollback(level, pos, 1);
    }

    /**
     * Откатить посев сразу на несколько стадий.
     *
     * <p>Разница между зверьём тут не в жадности, а в повадке: саранча грызёт понемногу и
     * часто, ворона отхватывает разом и улетает. Ниже нуля откат не уходит — сам росток
     * остаётся в земле.</p>
     */
    public static boolean rollback(final ServerLevel level, final BlockPos pos, final int stages) {
        BlockState state = level.getBlockState(pos);
        IntegerProperty age = age(state);
        if (age == null || stages <= 0) {
            return false;
        }

        int now = state.getValue(age);
        int back = Math.max(0, now - stages);
        if (back == now) {
            return false;
        }

        level.setBlock(pos, state.setValue(age, back), Block.UPDATE_ALL);
        return true;
    }
}
