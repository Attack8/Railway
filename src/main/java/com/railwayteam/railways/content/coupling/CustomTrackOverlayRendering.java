package com.railwayteam.railways.content.coupling;

import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.railwayteam.railways.registry.CRBlockPartials;
import com.simibubi.create.content.logistics.trains.BezierConnection;
import com.simibubi.create.content.logistics.trains.ITrackBlock;
import com.simibubi.create.content.logistics.trains.management.edgePoint.EdgePointType;
import com.simibubi.create.content.logistics.trains.track.BezierTrackPointLocation;
import com.simibubi.create.content.logistics.trains.track.TrackBlock;
import com.simibubi.create.content.logistics.trains.track.TrackRenderer;
import com.simibubi.create.content.logistics.trains.track.TrackTileEntity;
import com.simibubi.create.content.schematics.SchematicWorld;
import com.simibubi.create.foundation.ponder.PonderWorld;
import com.simibubi.create.foundation.render.CachedBufferer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CustomTrackOverlayRendering {
    public static final Map<EdgePointType<?>, PartialModel> CUSTOM_OVERLAYS = new HashMap<>();

    public static void register(EdgePointType<?> edgePointType, PartialModel model) {
        CUSTOM_OVERLAYS.put(edgePointType, model);
    }

    @Nullable
    public static PartialModel getCouplerOverlayModel(boolean couple, boolean decouple) {
        if (couple && decouple) return CRBlockPartials.COUPLER_BOTH;
        if (couple) return CRBlockPartials.COUPLER_COUPLE;
        if (decouple) return CRBlockPartials.COUPLER_DECOUPLE;
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public static void renderOverlay(LevelAccessor level, BlockPos pos, Direction.AxisDirection direction,
                                     BezierTrackPointLocation bezier, PoseStack ms, MultiBufferSource buffer, int light, int overlay,
                                     EdgePointType<?> type, float scale) {
        if (CUSTOM_OVERLAYS.containsKey(type))
            renderOverlay(level, pos, direction, bezier, ms, buffer, light, overlay, CUSTOM_OVERLAYS.get(type), scale, false);
    }

    @OnlyIn(Dist.CLIENT)
    public static void renderOverlay(LevelAccessor level, BlockPos pos, Direction.AxisDirection direction,
                                     BezierTrackPointLocation bezier, PoseStack ms, MultiBufferSource buffer, int light, int overlay,
                                     PartialModel model, float scale) {
        renderOverlay(level, pos, direction, bezier, ms, buffer, light, overlay, model, scale, false);
    }

    //Copied from TrackTargetingBehaviour
    @OnlyIn(Dist.CLIENT)
    public static void renderOverlay(LevelAccessor level, BlockPos pos, Direction.AxisDirection direction,
                              BezierTrackPointLocation bezier, PoseStack ms, MultiBufferSource buffer, int light, int overlay,
                              PartialModel model, float scale, boolean offsetToSide) {
        if (level instanceof SchematicWorld && !(level instanceof PonderWorld))
            return;

        BlockState trackState = level.getBlockState(pos);
        Block block = trackState.getBlock();
        if (!(block instanceof ITrackBlock))
            return;

        ms.pushPose();
        ms.translate(pos.getX(), pos.getY(), pos.getZ());

        PartialModel partial = prepareTrackOverlay(level, pos, trackState, bezier, direction, ms, model);
        if (partial != null)
            CachedBufferer.partial(partial, trackState)
                .translate(.5, 0, .5)
                .scale(scale)
                .translate(offsetToSide ? .5 : -.5, 0, -.5)
                .light(LevelRenderer.getLightColor(level, pos))
                .renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

        ms.popPose();
    }

    //Copied from TrackBlock
    @OnlyIn(Dist.CLIENT)
    public static PartialModel prepareTrackOverlay(BlockGetter world, BlockPos pos, BlockState state,
                                            BezierTrackPointLocation bezierPoint, Direction.AxisDirection direction, PoseStack ms, PartialModel model) {
        TransformStack msr = TransformStack.cast(ms);

        Vec3 axis = null;
        Vec3 diff = null;
        Vec3 normal = null;
        Vec3 offset = null;

        if (bezierPoint != null && world.getBlockEntity(pos) instanceof TrackTileEntity trackTE) {
            BezierConnection bc = trackTE.getConnections().get(bezierPoint.curveTarget());
            if (bc != null) {
                double length = Mth.floor(bc.getLength() * 2);
                int seg = bezierPoint.segment() + 1;
                double t = seg / length;
                double tpre = (seg - 1) / length;
                double tpost = (seg + 1) / length;

                offset = bc.getPosition(t);
                normal = bc.getNormal(t);
                diff = bc.getPosition(tpost)
                    .subtract(bc.getPosition(tpre))
                    .normalize();

                msr.translate(offset.subtract(Vec3.atBottomCenterOf(pos)));
                msr.translate(0, -4 / 16f, 0);
            } else
                return null;
        }

        if (normal == null) {
            axis = state.getValue(TrackBlock.SHAPE)
                .getAxes()
                .get(0);
            diff = axis.scale(direction.getStep())
                .normalize();
            normal = ((ITrackBlock) state.getBlock()).getUpNormal(world, pos, state);
        }

        Vec3 angles = TrackRenderer.getModelAngles(normal, diff);

        msr.centre()
            .rotateYRadians(angles.y)
            .rotateXRadians(angles.x)
            .unCentre();

        if (axis != null)
            msr.translate(0, axis.y != 0 ? 7 / 16f : 0, axis.y != 0 ? direction.getStep() * 2.5f / 16f : 0);
        else {
            msr.translate(0, 4 / 16f, 0);
            if (direction == Direction.AxisDirection.NEGATIVE)
                msr.rotateCentered(Direction.UP, Mth.PI);
        }

        return model;
    }
}
