package com.pwsayp.anima.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Дым горящего костра — старое средство от любой летающей напасти.
 *
 * <p>Ценность такой преграды в том, что она ставится заранее и работает без игрока: увидел
 * тучу на горизонте — поздно, а костры вокруг поля стоят с прошлой недели. Поэтому проверка
 * живёт в ядре: любой мод про летающую нечисть должен отвечать на дым одинаково, иначе
 * игроку придётся запоминать, от кого костёр спасает, а от кого нет.</p>
 *
 * <p>Область намеренно шире по горизонтали, чем по высоте: дым стелется, а не стоит
 * столбом.</p>
 */
public final class Smoke {
    private Smoke() {}

    /** Есть ли рядом горящий костёр — обычный или из душ. */
    public static boolean campfireNearby(final LevelReader level, final BlockPos pos, final int radius) {
        if (radius <= 0) {
            return false;
        }

        for (BlockPos near : BlockPos.betweenClosed(
                pos.offset(-radius, -2, -radius), pos.offset(radius, 4, radius))) {
            BlockState state = level.getBlockState(near);
            if ((state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE))
                    && state.getValue(BlockStateProperties.LIT)) {
                return true;
            }
        }
        return false;
    }
}
