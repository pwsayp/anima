package com.pwsayp.crows.entity;

import com.pwsayp.anima.world.Crops;
import com.pwsayp.crows.CrowsConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * Найти поблизости почти созревшую грядку, сесть на неё и склевать.
 *
 * <p>Урожай не пропадает и не дропается — посев просто откатывается на несколько стадий
 * роста. Между посадкой на грядку и уроном проходит {@link CrowsConfig#peckTicks} тиков:
 * это то окно, в которое игрок ещё успевает подбежать и спугнуть птицу.</p>
 */
public class EatCropGoal extends MoveToBlockGoal {
    private final Crow crow;
    private int peckTicks;

    public EatCropGoal(final Crow crow) {
        super(crow, 1.0, CrowsConfig.searchRange, 4);
        this.crow = crow;
    }

    @Override
    public boolean canUse() {
        return CrowsConfig.eatCrops && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return CrowsConfig.eatCrops && super.canContinueToUse();
    }

    @Override
    protected boolean isValidTarget(final LevelReader level, final BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.CROPS)) {
            return false;
        }

        if (CrowsConfig.wheatOnly && !state.is(Blocks.WHEAT)) {
            return false;
        }

        // Ворону интересует налившееся зерно, а не всходы: по умолчанию это только
        // полностью спелый колос. Порог подрезаем максимумом самой культуры — у пшеницы
        // спелость это семь, у свёклы три, и «клевать с седьмой стадии» отменяло бы свёклу
        // вовсе.
        //
        // Стадию спрашиваем у ядра, а не у класса блока: чужая культура не обязана
        // наследоваться от ванильного CropBlock, но свойство роста у неё то же самое.
        int ripe = Crops.maxStage(state);
        return ripe >= 0 && Crops.stage(state) >= Math.min(CrowsConfig.minCropAge, ripe);
    }

    /**
     * Целимся в саму грядку, а не в блок над ней (так по умолчанию делает MoveToBlockGoal).
     *
     * <p>Ворона садится на грядку, то есть стоит примерно на её нижней грани. До центра
     * блока сверху при этом остаётся ~1.56 блока — с порогом «дошла» такая цель не
     * засчитывалась бы никогда, и птица просто стояла бы на посеве.</p>
     */
    @Override
    protected BlockPos getMoveToTarget() {
        return this.blockPos;
    }

    @Override
    public double acceptedDistance() {
        return 1.1;
    }

    @Override
    public void start() {
        super.start();
        this.peckTicks = 0;
    }

    @Override
    public void stop() {
        super.stop();
        this.peckTicks = 0;
        this.crow.setPecking(false);
        this.crow.getNavigation().stop();
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.isReachedTarget()) {
            if (this.peckTicks > 0) {
                this.peckTicks = 0;
                this.crow.setPecking(false);
            }
            return;
        }

        this.crow.setPecking(true);
        this.crow.getLookControl().setLookAt(
                this.blockPos.getX() + 0.5, this.blockPos.getY() + 0.5, this.blockPos.getZ() + 0.5);

        this.peckTicks++;
        if (this.peckTicks % 12 == 0) {
            // Сухой шорох по колосьям, а не чавканье. Раньше здесь стоял человеческий звук
            // еды, да ещё и приспущенный по высоте вместе с голосом птицы, — из грядки
            // слышалось, будто в ней кто-то большой обедает.
            this.crow.playSound(SoundEvents.GRASS_HIT, 0.35F, 1.5F + this.crow.getRandom().nextFloat() * 0.2F);
        }

        if (this.peckTicks >= CrowsConfig.peckTicks) {
            this.eat();
            this.peckTicks = 0;
            this.crow.setPecking(false);
            // Один укус за подлёт: обрываем цель, дальше грядка ищется заново.
            this.tryTicks = 1201;
        }
    }

    /** Откатить посев на несколько стадий роста. Ничего не ломается и не дропается. */
    private void eat() {
        if (!(this.crow.level() instanceof ServerLevel level)) {
            return;
        }

        if (CrowsConfig.respectMobGriefing && !ForgeEventFactory.getMobGriefingEvent(level, this.crow)) {
            return;
        }

        BlockState state = level.getBlockState(this.blockPos);
        if (!Crops.rollback(level, this.blockPos, CrowsConfig.stagesLost)) {
            return;
        }

        level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, this.blockPos, Block.getId(state));
        level.playSound(null, this.blockPos, SoundEvents.CROP_BREAK, this.crow.getSoundSource(), 0.7F, 1.0F);
        level.sendParticles(ParticleTypes.CRIT,
                this.blockPos.getX() + 0.5, this.blockPos.getY() + 0.4, this.blockPos.getZ() + 0.5,
                4, 0.2, 0.1, 0.2, 0.0);
    }
}
