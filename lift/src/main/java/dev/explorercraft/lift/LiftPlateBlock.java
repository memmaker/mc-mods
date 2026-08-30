package dev.explorercraft.lift;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/** A pressure plate that carries whoever stands on it upward. See {@link Lift#beginAscend}. */
public class LiftPlateBlock extends PressurePlateBlock {
    public LiftPlateBlock(BlockBehaviour.Properties properties) {
        super(BlockSetType.STONE, properties);
    }

    /**
     * {@code stepOn} is the wrong hook here even though the plate now has collision: it fires
     * only as an entity moves along the block it rests on, whereas this needs to keep noticing a
     * rider who is standing still. {@code entityInside} is what pressure plates use for their own
     * redstone check and runs every tick the entity overlaps the block, so it catches both.
     */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effectApplier, boolean insideMainCollisionBox) {
        super.entityInside(state, level, pos, entity, effectApplier, insideMainCollisionBox);
        if (level instanceof ServerLevel serverLevel && entity instanceof ServerPlayer player) {
            Lift.beginAscend(serverLevel, pos, player);
        }
    }

    /**
     * A travelling plate spends its whole trip in mid-air, so — unlike every other pressure
     * plate — it must not pop off when there is nothing underneath it.
     */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }
}
