package com.railwayteam.railways.mixin.client;


import com.jozufozu.flywheel.core.PartialModel;
import com.railwayteam.railways.content.custom_tracks.TrackMaterial;
import com.railwayteam.railways.mixin_interfaces.IGetBezierConnection;
import com.railwayteam.railways.mixin_interfaces.IHasTrackMaterial;
import com.railwayteam.railways.registry.CRBlockPartials;
import com.simibubi.create.content.logistics.trains.BezierConnection;
import com.simibubi.create.content.logistics.trains.track.TrackInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.simibubi.create.AllBlockPartials.TRACK_TIE;
import static com.simibubi.create.AllBlockPartials.TRACK_SEGMENT_LEFT;
import static com.simibubi.create.AllBlockPartials.TRACK_SEGMENT_RIGHT;

@OnlyIn(Dist.CLIENT)
@Mixin(targets = "com.simibubi.create.content.logistics.trains.track.TrackInstance$BezierTrackInstance", remap = false)
public class MixinTrackInstance_BezierTrackInstance {

  @Final
  @Shadow(aliases = {"this$0"})
  TrackInstance myOuter;

  @Redirect(method = "<init>", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
      target = "Lcom/simibubi/create/AllBlockPartials;TRACK_TIE:Lcom/jozufozu/flywheel/core/PartialModel;"), remap = false)
  private PartialModel replaceTie() {
    BezierConnection bc = ((IGetBezierConnection) myOuter).getBezierConnection();
    if (bc != null) {
      TrackMaterial material = ((IHasTrackMaterial) bc).getMaterial();
      if (material.isCustom()) {
        return CRBlockPartials.TRACK_PARTS.get(material).tie;
      }
    }
    return TRACK_TIE;
  }

  @Redirect(method = "<init>", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
      target = "Lcom/simibubi/create/AllBlockPartials;TRACK_SEGMENT_LEFT:Lcom/jozufozu/flywheel/core/PartialModel;"), remap = false)
  private PartialModel replaceSegLeft() {
    BezierConnection bc = ((IGetBezierConnection) myOuter).getBezierConnection();
    if (bc != null) {
      TrackMaterial material = ((IHasTrackMaterial) bc).getMaterial();
      if (material.isCustom()) {
        return CRBlockPartials.TRACK_PARTS.get(material).segment_left;
      }
    }
    return TRACK_SEGMENT_LEFT;
  }

  @Redirect(method = "<init>", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
      target = "Lcom/simibubi/create/AllBlockPartials;TRACK_SEGMENT_RIGHT:Lcom/jozufozu/flywheel/core/PartialModel;"), remap = false)
  private PartialModel replaceSegRight() {
    BezierConnection bc = ((IGetBezierConnection) myOuter).getBezierConnection();
    if (bc != null) {
      TrackMaterial material = ((IHasTrackMaterial) bc).getMaterial();
      if (material.isCustom()) {
        return CRBlockPartials.TRACK_PARTS.get(material).segment_right;
      }
    }
    return TRACK_SEGMENT_RIGHT;
  }

  @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/trains/BezierConnection;getSegmentCount()I", remap = false))
  private int messWithCtor(BezierConnection instance) {
    return ((IHasTrackMaterial) instance).getMaterial().trackType == TrackMaterial.TrackType.MONORAIL ? 0 : instance.getSegmentCount();
  }

  @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/trains/BezierConnection;getBakedSegments()[Lcom/simibubi/create/content/logistics/trains/BezierConnection$SegmentAngles;", remap = false))
  private BezierConnection.SegmentAngles[] messWithCtor2(BezierConnection instance) {
    return ((IHasTrackMaterial) instance).getMaterial().trackType == TrackMaterial.TrackType.MONORAIL ? new BezierConnection.SegmentAngles[0] : instance.getBakedSegments();
  }
}
