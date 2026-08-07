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
     * Откатить посев на одну стадию назад.
     *
     * <p>Вернёт {@code false}, если откатывать нечего — блок не посев или уже на нуле. Тогда
     * решает вызывающая сторона: одному зверю положено сожрать ростки совсем, другому —
     * оставить их в покое.</p>
     */
    public static boolean rollback(final ServerLevel level, final BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        IntegerProperty age = age(state);
        if (age == null || state.getValue(age) <= 0) {
            return false;
        }

        level.setBlock(pos, state.setValue(age, state.getValue(age) - 1), Block.UPDATE_ALL);
        return true;
    }
}
