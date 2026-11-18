package com.copperironalloy.fridge;

import com.copperironalloy.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class FridgeBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public enum Part {
        SINGLE, LOWER, UPPER
    }

    private final FridgeTier tier;

    public FridgeBlock(Properties properties, FridgeTier tier) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
    }

    public FridgeTier tier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        if (tier == FridgeTier.WHITE_STEEL) {
            BlockPos pos = ctx.getClickedPos();
            BlockState below = ctx.getLevel().getBlockState(pos.below());
            BlockState above = ctx.getLevel().getBlockState(pos.above());
            if (below.getBlock() instanceof FridgeBlock other && other.tier == FridgeTier.WHITE_STEEL) {
                facing = below.getValue(FACING);
            } else if (above.getBlock() instanceof FridgeBlock other && other.tier == FridgeTier.WHITE_STEEL) {
                facing = above.getValue(FACING);
            }
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return tier == FridgeTier.WHITE_STEEL ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }

    /**
     * One white-steel block is a top-opening freezer and needs the space above.
     * Two stacked blocks are one front-opening fridge and need the space in front of both halves.
     * Brittle white steel and the earlier fridges are ordinary cabinets.
     */
    public static boolean canOpen(BlockGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof FridgeBlock fridge) || fridge.tier != FridgeTier.WHITE_STEEL) {
            return true;
        }
        Part part = part(level, pos, state);
        if (part == Part.SINGLE) {
            return !occupied(level, pos.above());
        }
        Direction facing = state.getValue(FACING);
        BlockPos other = part == Part.LOWER ? pos.above() : pos.below();
        return !occupied(level, pos.relative(facing)) && !occupied(level, other.relative(facing));
    }

    /** Pairs a vertical run from the bottom. A third block stays a separate freezer. */
    public static Part part(BlockGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof FridgeBlock fridge) || fridge.tier != FridgeTier.WHITE_STEEL) {
            return Part.SINGLE;
        }
        Direction facing = state.getValue(FACING);
        int below = 0;
        BlockPos cursor = pos.below();
        while (below < 16 && isWhiteSteel(level, cursor, facing)) {
            below++;
            cursor = cursor.below();
        }
        if ((below & 1) == 1) {
            return Part.UPPER;
        }
        if (isWhiteSteel(level, pos.above(), facing)) {
            return Part.LOWER;
        }
        return Part.SINGLE;
    }

    private static boolean isWhiteSteel(BlockGetter level, BlockPos pos, Direction facing) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof FridgeBlock fridge
                && fridge.tier == FridgeTier.WHITE_STEEL
                && state.getValue(FACING) == facing;
    }

    private static boolean occupied(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!canOpen(level, pos, state)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FridgeBlockEntity fridge) {
                NetworkHooks.openScreen(serverPlayer, fridge, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FridgeBlockEntity fridge) {
                fridge.dropContents();
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FridgeBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return createTickerHelper(type, ModBlockEntities.FRIDGE.get(), FridgeBlockEntity::clientTick);
        }
        return createTickerHelper(type, ModBlockEntities.FRIDGE.get(), FridgeBlockEntity::serverTick);
    }
}
