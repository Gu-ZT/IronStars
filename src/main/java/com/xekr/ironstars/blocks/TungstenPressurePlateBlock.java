package com.xekr.ironstars.blocks;

import com.xekr.ironstars.blocks.entity.TungstenPressurePlateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class TungstenPressurePlateBlock extends PressurePlateBlock implements EntityBlock {
    public static final BooleanProperty PRESSED = BooleanProperty.create("pressed");

    public TungstenPressurePlateBlock(Sensitivity pSensitivity, Properties pProperties) {
        super(pSensitivity, pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false).setValue(PRESSED, false));
    }

    @NotNull
    @Override
    public  VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return pState.getValue(PRESSED) ? PRESSED_AABB : AABB;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(PRESSED);
    }

    @Override
    public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, Random pRand) {
        if (pState.getValue(PRESSED)) {
            this.checkPressed(null, pLevel, pPos, pState, true);
        }
    }

    @Override
    public void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
        if (!pLevel.isClientSide && !pState.getValue(PRESSED)) {
            checkPressed(pEntity, pLevel, pPos, pState, false);
        }
    }

    protected void checkPressed(@Nullable Entity pEntity, Level pLevel, BlockPos pPos, BlockState pState, boolean currentPressed) {
        boolean pressed = this.getSignalStrength(pLevel, pPos) > 0;

        if (currentPressed != pressed) {
            BlockState blockstate = pState.setValue(PRESSED, pressed);
            pLevel.setBlock(pPos, blockstate, 2);
            this.updateNeighbours(pLevel, pPos);
            pLevel.setBlocksDirty(pPos, pState, blockstate);
        }

        if (!pressed && currentPressed) {
            this.playOffSound(pLevel, pPos);
            pLevel.gameEvent(pEntity, GameEvent.BLOCK_UNPRESS, pPos);
        } else if (pressed && !currentPressed) {
            this.playOnSound(pLevel, pPos);
            pLevel.gameEvent(pEntity, GameEvent.BLOCK_PRESS, pPos);
        }

        if (pressed) {
            pLevel.scheduleTick(new BlockPos(pPos), this, this.getPressedTime());
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new TungstenPressurePlateBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide ? null : (world, pos, state, blockEntity) -> {
            if (blockEntity instanceof TungstenPressurePlateBlockEntity) {
                ((TungstenPressurePlateBlockEntity)blockEntity).tick();
            }
        };
    }

    @Override
    protected int getSignalStrength(Level pLevel, BlockPos pPos) {
        net.minecraft.world.phys.AABB aabb = TOUCH_AABB.move(pPos);
        List<? extends Entity> list = pLevel.getEntitiesOfClass(Monster.class, aabb);
        if (!list.isEmpty()) {
            for(Entity entity : list) {
                if (!entity.isIgnoringBlockTriggers()) {
                    return 15;
                }
            }
        }
        return 0;
    }
}
