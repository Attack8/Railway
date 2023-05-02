package com.railwayteam.railways.content.custom_bogeys.monobogey;

import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.util.transform.Transform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.logistics.trains.BogeyRenderer;
import com.simibubi.create.content.logistics.trains.BogeySizes;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.nbt.CompoundTag;

import static com.railwayteam.railways.registry.CRBlockPartials.MONOBOGEY_FRAME;
import static com.railwayteam.railways.registry.CRBlockPartials.MONOBOGEY_WHEEL;

public class MonoBogeyRenderer {
    public static class SmallMonoBogeyRenderer extends BogeyRenderer {

        @Override
        public void initialiseContraptionModelData(MaterialManager materialManager) {
            createModelInstances(materialManager, MONOBOGEY_WHEEL, 4);
            createModelInstances(materialManager, MONOBOGEY_FRAME);
        }

        @Override
        public BogeyRenderer createNewInstance() {
            return new SmallMonoBogeyRenderer();
        }

        @Override
        public BogeySizes.BogeySize getSize() {
            return BogeySizes.SMALL;
        }

        @Override
        public void render(boolean upsideDown, CompoundTag bogeyData, float wheelAngle, PoseStack ms, int light, VertexConsumer vb) {
            boolean inContraption = vb == null;
            Transform<?> transform = getTransformFromPartial(MONOBOGEY_FRAME, ms, inContraption);
            finalize(transform, ms, light, vb);

            Transform<?>[] wheels = getTransformsFromPartial(MONOBOGEY_WHEEL, ms, inContraption, 4);
            /*for (int side : Iterate.positiveAndNegative) {
                if (!inContraption)
                    ms.pushPose();
                Transform<?> wheel = wheels[(side + 1) / 2];
                wheel.translate(0, 12 / 16f, side)
                    .rotateX(wheelAngle);
                finalize(wheel, ms, light, vb);
                if (!inContraption)
                    ms.popPose();
            }*/
            for (boolean left : Iterate.trueAndFalse) {
                for (int front : Iterate.positiveAndNegative) {
                    if (!inContraption)
                        ms.pushPose();
                    Transform<?> wheel = wheels[(left ? 1 : 0) + (front + 1)];
                    wheel.translate(left ? -12 / 16f : 12 / 16f, upsideDown ? 3 /16f : 3 / 16f, front * 15 / 16f) //base position
                        .rotateY(left ? wheelAngle : -wheelAngle)
                        .translate(15/16f, 0, 0/16f);
                    finalize(wheel, ms, light, vb);
//                        .light(light)
  //                      .renderInto(ms, vb);
                    if (!inContraption)
                        ms.popPose();
                }
            }
        }
    }
}
