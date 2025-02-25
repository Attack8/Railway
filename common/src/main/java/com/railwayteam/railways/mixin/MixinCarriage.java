package com.railwayteam.railways.mixin;

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.content.conductor.ConductorEntity;
import com.railwayteam.railways.mixin_interfaces.ICarriageConductors;
import com.simibubi.create.content.trains.entity.*;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.foundation.utility.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(value = Carriage.class, remap = false)
public class MixinCarriage implements ICarriageConductors {

    @Shadow public Train train;
    private final List<UUID> controllingConductors = new ArrayList<>();

    @Override
    public List<UUID> getControllingConductors() {
        return controllingConductors;
    }

    @Redirect(method = "updateConductors", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity/CarriageContraptionEntity;checkConductors()Lcom/simibubi/create/foundation/utility/Couple;"))
    private Couple<Boolean> addControllingConductors(CarriageContraptionEntity instance) {
        controllingConductors.clear();
        if (instance.getContraption() instanceof CarriageContraption cc) {
            for (Entity passenger : instance.getPassengers()) {
                if (!(passenger instanceof ConductorEntity))
                    continue;
                BlockPos seatOf = cc.getSeatOf(passenger.getUUID());
                if (seatOf == null)
                    continue;
                Couple<Boolean> validSides = cc.conductorSeats.get(seatOf);
                if (validSides == null)
                    continue;
                if (validSides.getFirst() || validSides.getSecond())
                    controllingConductors.add(passenger.getUUID());
            }
        }
        return instance.checkConductors();
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void writeControllingConductors(DimensionPalette dimensions, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        ListTag listTag = new ListTag();
        for (UUID uuid : controllingConductors) {
            CompoundTag uuidTag = new CompoundTag();
            uuidTag.putUUID("UUID", uuid);
            listTag.add(uuidTag);
        }
        tag.put("ControllingConductors", listTag);
    }

    @Inject(method = "read", at = @At("RETURN"))
    private static void readControllingConductors(CompoundTag tag, TrackGraph graph, DimensionPalette dimensions, CallbackInfoReturnable<Carriage> cir) {
        Carriage carriage = cir.getReturnValue();
        List<UUID> controllingConductors = ((ICarriageConductors) carriage).getControllingConductors();
        controllingConductors.clear();
        if (tag.contains("ControllingConductors", Tag.TAG_LIST)) {
            ListTag listTag = tag.getList("ControllingConductors", Tag.TAG_COMPOUND);
            for (Tag item : listTag) {
                if (item instanceof CompoundTag uuidTag && uuidTag.hasUUID("UUID")) {
                    controllingConductors.add(uuidTag.getUUID("UUID"));
                }
            }
        }
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void markTravelStart(Level level, TrackGraph graph, double distance, TravellingPoint toFollowForward,
                                 TravellingPoint toFollowBackward, int type, CallbackInfoReturnable<Double> cir) {
        if (train.navigation.isActive()) // only do automatic stuff when automatically operated
            Railways.trackEdgeCarriageTravelling = true;
    }

    @Inject(method = "travel", at = @At("RETURN"))
    private void markTravelEnd(Level level, TrackGraph graph, double distance, TravellingPoint toFollowForward,
                               TravellingPoint toFollowBackward, int type, CallbackInfoReturnable<Double> cir) {
        Railways.trackEdgeCarriageTravelling = false;
    }
}
