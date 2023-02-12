package com.railwayteam.railways.mixin.client;

import com.railwayteam.railways.content.custom_tracks.TrackMaterial;
import com.railwayteam.railways.util.AllBlocksWrapper;
import com.simibubi.create.foundation.ponder.content.PonderIndex;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = PonderIndex.class, remap = false)
public abstract class MixinPonderIndex {
  @ModifyArg(method = "register", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/ponder/PonderRegistrationHelper;forComponents([Lcom/tterrag/registrate/util/entry/ItemProviderEntry;)Lcom/simibubi/create/foundation/ponder/PonderRegistrationHelper$MultiSceneBuilder;"))
  private static ItemProviderEntry<?>[] changeTrackRegistration(ItemProviderEntry<?>[] components) {
    if (components.length >= 1) {
      List<ItemProviderEntry<?>> out = new ArrayList<>(List.of(components));
      if (out.contains(AllBlocksWrapper.track())) {
        out.addAll(TrackMaterial.allCustomBlocks());
        return out.toArray(new ItemProviderEntry[0]);
      }
    }
    return components;
  }
}
