package com.railwayteam.railways.mixin;

import com.railwayteam.railways.mixin_interfaces.IHasTrackCasing;
import com.railwayteam.railways.registry.CRTags;
import com.simibubi.create.content.logistics.trains.BezierConnection;
import com.simibubi.create.content.logistics.trains.track.TrackBlock;
import com.simibubi.create.content.logistics.trains.track.TrackShape;
import com.simibubi.create.content.logistics.trains.track.TrackTileEntity;
import com.simibubi.create.foundation.networking.AllPackets;
import com.simibubi.create.foundation.tileEntity.RemoveTileEntityPacket;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(TrackTileEntity.class)
public abstract class MixinTrackTileEntity extends SmartTileEntity implements IHasTrackCasing {
  @Shadow
  Map<BlockPos, BezierConnection> connections;

  protected SlabBlock trackCasing;
  protected boolean isAlternateModel;

  protected MixinTrackTileEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  @Override
  public @Nullable SlabBlock getTrackCasing() {
    return trackCasing;
  }

  @Override
  public void setTrackCasing(@Nullable SlabBlock trackCasing) {
    if (trackCasing != null && CRTags.AllBlockTags.TRACK_CASING_BLACKLIST.matches(trackCasing)) //sanity check
      return;
    this.trackCasing = trackCasing;
    notifyUpdate();
    if (this.trackCasing == null && this.level != null && !this.level.isClientSide) { //Clean up the tile entity if it is no longer needed
      if (!this.connections.isEmpty() || getBlockState().getOptionalValue(TrackBlock.SHAPE)
          .orElse(TrackShape.NONE)
          .isPortal())
        return;
      BlockState blockState = this.level.getBlockState(worldPosition);
      if (blockState.hasProperty(TrackBlock.HAS_TE))
        level.setBlockAndUpdate(worldPosition, blockState.setValue(TrackBlock.HAS_TE, false));
      AllPackets.channel.send(packetTarget(), new RemoveTileEntityPacket(worldPosition));
    }
  }

  @Override
  public boolean isAlternate() {
    return isAlternateModel;
  }

  @Override
  public void setAlternate(boolean alternate) {
    this.isAlternateModel = alternate;
    notifyUpdate();
  }

  // Track casings require a TE to function, so prevent it from being removed.
  @Inject(method = "removeConnection", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", remap = true), cancellable = true, remap = false)
  private void preventTileRemoval(BlockPos target, CallbackInfo ci) {
    if (getTrackCasing() != null) {
      notifyUpdate();
      ci.cancel();
    }
  }

  @Inject(method = "removeInboundConnections", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/network/simple/SimpleChannel;send(Lnet/minecraftforge/network/PacketDistributor$PacketTarget;Ljava/lang/Object;)V"), cancellable = true, remap = false)
  private void preventTileRemoval2(CallbackInfo ci) {
    if (getTrackCasing() != null) {
      notifyUpdate();
      ci.cancel();
    }
  }

  @Inject(method = "write", at = @At("RETURN"), remap = false)
  private void writeCasing(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
    if (this.getTrackCasing() != null) {
      tag.putString("TrackCasing", ForgeRegistries.BLOCKS.getKey(getTrackCasing()).toString());
    }
    tag.putBoolean("AlternateModel", this.isAlternate());
  }

  @Inject(method = "read", at = @At("RETURN"), remap = false)
  private void readCasing(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
    if (tag.contains("AlternateModel")) {
      this.setAlternate(tag.getBoolean("AlternateModel"));
    } else {
      this.setAlternate(false);
    }

    if (tag.contains("TrackCasing")) {
      ResourceLocation casingName = ResourceLocation.of(tag.getString("TrackCasing"), ':');
      if (ForgeRegistries.BLOCKS.containsKey(casingName)) {
        Block casingBlock = ForgeRegistries.BLOCKS.getValue(casingName);
        if (casingBlock instanceof SlabBlock slab) {
          this.setTrackCasing(slab);
          return;
        }
      }
    }
    this.setTrackCasing(null);
  }
}
