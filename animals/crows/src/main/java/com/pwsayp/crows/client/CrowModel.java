package com.pwsayp.crows.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Модель вороны.
 *
 * <p>Ворона — птица тяжёлая, и в силуэте это главное. Не воробей и не попугай: <b>крупная
 * голова, посаженная почти без шеи, толстый прямой клюв с горбинкой и плотное тело,
 * переходящее в длинный клиновидный хвост</b>. Если голову сделать мелкой, а клюв
 * тонким, получается скворец, сколько его ни крась в чёрный.</p>
 *
 * <p>Сидящая ворона держит крылья <b>сложенными вдоль тела</b>, а не разведёнными: сложенное
 * крыло идёт от плеча к хвосту и заходит на него концом. Разводит она их только в полёте —
 * оттуда и вся разница между сидящей и летящей птицей.</p>
 *
 * <p>Развёртка 64×32, регионы кубов не пересекаются: тело (0,0), голова (26,0), клюв (44,0),
 * крыло (0,13), хвост (18,13), цевка (44,13), пальцы (48,13).</p>
 */
public class CrowModel extends EntityModel<CrowRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public CrowModel(final ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.tail = root.getChild("tail");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Тело сужается к хвосту не формой, а тем, что хвост уже его: коробкой этого не
        // передать, зато читается посадка — грудь вперёд, зад приподнят.
        root.addOrReplaceChild(
                "body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, 0.0F, -3.5F, 5.0F, 5.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 16.5F, 0.0F, 0.15F, 0.0F, 0.0F));

        // Голова крупная и сидит вплотную к плечам: шеи у сидящей вороны почти не видно.
        PartDefinition head = root.addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(26, 0).addBox(-2.0F, -3.5F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 15.5F, -3.0F));

        // Клюв толстый у основания и длинный — примета врановых. Тонкая палочка на его
        // месте превращала ворону в скворца.
        head.addOrReplaceChild(
                "beak",
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.75F, -2.25F, -4.5F, 1.5F, 1.5F, 3.0F),
                PartPose.ZERO);

        // Хвост длинный и плоский, чуть задран.
        root.addOrReplaceChild(
                "tail",
                CubeListBuilder.create().texOffs(18, 13).addBox(-2.5F, 0.0F, 0.0F, 5.0F, 1.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 17.5F, 3.0F, 0.2F, 0.0F, 0.0F));

        // Крыло сложено вдоль тела: длинное, узкое, конец заходит на хвост.
        root.addOrReplaceChild(
                "left_wing",
                CubeListBuilder.create().texOffs(0, 13).addBox(0.0F, 0.0F, -3.5F, 1.0F, 4.0F, 8.0F),
                PartPose.offset(2.5F, 17.0F, -0.5F));
        root.addOrReplaceChild(
                "right_wing",
                CubeListBuilder.create().texOffs(0, 13).mirror()
                        .addBox(-1.0F, 0.0F, -3.5F, 1.0F, 4.0F, 8.0F),
                PartPose.offset(-2.5F, 17.0F, -0.5F));

        // Лапы с пальцами: без пальцев птица «висит» над землёй на двух палках.
        CubeListBuilder shank = CubeListBuilder.create().texOffs(44, 13)
                .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F);
        CubeListBuilder toes = CubeListBuilder.create().texOffs(48, 13)
                .addBox(-1.0F, 0.0F, -1.5F, 2.0F, 1.0F, 3.0F);

        PartDefinition left = root.addOrReplaceChild("left_leg", shank,
                PartPose.offset(1.2F, 21.0F, 0.5F));
        left.addOrReplaceChild("left_toes", toes, PartPose.offset(0.0F, 3.0F, -0.5F));

        PartDefinition right = root.addOrReplaceChild("right_leg", shank,
                PartPose.offset(-1.2F, 21.0F, 0.5F));
        right.addOrReplaceChild("right_toes", toes, PartPose.offset(0.0F, 3.0F, -0.5F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(final CrowRenderState state) {
        super.setupAnim(state);

        if (state.pecking) {
            // Клюёт: голова ныряет вниз короткими рывками, по сторонам не смотрит.
            this.head.xRot = 0.9F + Mth.cos(state.ageInTicks * 0.8F) * 0.5F;
            this.head.yRot = 0.0F;
        } else {
            this.head.xRot = state.xRot * ((float) Math.PI / 180.0F);
            this.head.yRot = state.yRot * ((float) Math.PI / 180.0F);
        }

        if (state.flying) {
            // Корпус в полёте вытягивается горизонтально, лапы подобраны, хвост распущен —
            // ворона в воздухе это стрела, а не сидящий комок.
            this.body.xRot = 0.02F;
            this.leftLeg.xRot += (float) (Math.PI * 2.0 / 9.0);
            this.rightLeg.xRot += (float) (Math.PI * 2.0 / 9.0);
            this.tail.xRot -= 0.2F;

            if (state.gliding) {
                // Планирование: крылья разведены и неподвижны, чуть отведены вперёд.
                this.leftWing.zRot = -0.12F;
                this.rightWing.zRot = 0.12F;
                this.leftWing.yRot = -0.15F;
                this.rightWing.yRot = 0.15F;
            } else {
                // Взмах — редкий и глубокий: от заметно выше спины до заметно ниже брюха.
                // Именно глубина и редкость отличают врановый полёт от трепета попугая.
                float beat = Mth.sin(state.wingPhase);
                this.leftWing.zRot = -0.3F - beat * 0.95F;
                this.rightWing.zRot = 0.3F + beat * 0.95F;
                // Гребок: на махе вниз крыло уходит немного вперёд.
                this.leftWing.yRot = -beat * 0.12F;
                this.rightWing.yRot = beat * 0.12F;

                // Тело подбрасывает в такт: на махе вниз птица приподнимается.
                float bob = Mth.cos(state.wingPhase) * 0.25F;
                this.body.y += bob;
                this.head.y += bob;
                this.tail.y += bob;
                this.leftWing.y += bob;
                this.rightWing.y += bob;
                this.leftLeg.y += bob;
                this.rightLeg.y += bob;
            }
        } else {
            // На земле крылья прижаты к телу — сидящая ворона не растопыривается.
            this.leftWing.zRot = -0.02F;
            this.rightWing.zRot = 0.02F;
            this.leftWing.yRot = 0.0F;
            this.rightWing.yRot = 0.0F;
            this.leftLeg.xRot += Mth.cos(state.walkAnimationPos * 0.6662F) * 1.4F * state.walkAnimationSpeed;
            this.rightLeg.xRot += Mth.cos(state.walkAnimationPos * 0.6662F + (float) Math.PI) * 1.4F
                    * state.walkAnimationSpeed;
            this.tail.xRot += Mth.cos(state.walkAnimationPos * 0.6662F) * 0.3F * state.walkAnimationSpeed;
        }
    }
}
