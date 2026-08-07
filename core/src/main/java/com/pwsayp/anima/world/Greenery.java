package com.pwsayp.anima.world;

import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Дикая зелень: трава, цветы, поросль и всё, что растёт само.
 *
 * <p>Отделено от посевов намеренно. Грядка — это чужой труд, и трогать её мод должен
 * осознанно; луг же ничей, и вытоптать его — обычное дело для зверя. Почти каждому моду про
 * живность нужна ровно эта граница, поэтому список общий: иначе у одного мода тростник
 * съедобный, у другого нет, и мир перестаёт быть предсказуемым.</p>
 *
 * <p>Держится на ванильных тегах и нескольких блоках по именам — никакой оглядки на другие
 * моды: что честно лежит в теге, то и зелень.</p>
 */
public final class Greenery {
    private Greenery() {}

    /** Дикая зелень — то, что выросло само. */
    public static boolean isWild(final BlockState state) {
        return state.is(BlockTags.REPLACEABLE_BY_TREES)
                // FLOWERS шире, чем SMALL_FLOWERS: сюда попадают и подсолнух с кустом розы.
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockItemTags.SAPLINGS.block())
                || state.is(Blocks.SUGAR_CANE)
                || state.is(Blocks.BAMBOO)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.VINE)
                || state.is(Blocks.COCOA)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.MELON)
                || state.is(Blocks.PUMPKIN);
    }

    /**
     * Растёт ли эта зелень столбиком — тростник, бамбук, кактус.
     *
     * <p>Такие стебли объедаются сверху вниз: зверь обгрызает верхушку, а не подрубает
     * стебель у корня, роняя всё остальное на землю предметами.</p>
     */
    public static boolean isStalk(final BlockState state) {
        return state.is(Blocks.SUGAR_CANE) || state.is(Blocks.BAMBOO) || state.is(Blocks.CACTUS);
    }
}
