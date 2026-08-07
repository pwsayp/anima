package com.pwsayp.locusts.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * Модель саранчи.
 *
 * <p>Особь размером с треть блока и почти никогда не видна одна, поэтому модель держится не
 * на мелких деталях, а на силуэте — по нему саранча и узнаётся с трёх шагов. Силуэт складывают
 * три вещи, и все три взяты с натуры:</p>
 *
 * <ul>
 *   <li><b>Переднеспинка «седлом»</b> — горб сразу за головой, самая заметная примета
 *       прямокрылых. Без неё насекомое читается как муха.</li>
 *   <li><b>Крылья вдоль спины</b>, заходящие за конец брюшка. Начинаются они <b>позади
 *       горба</b> — и это не мелочь: пока крыло начиналось от головы, его передняя половина
 *       была спрятана внутри переднеспинки, наружу торчал огрызок, и выглядело это поломкой.</li>
 *   <li><b>Коленчатые задние ноги</b>: бедро задрано вверх-назад, голень от колена вниз.
 *       Длина у них с половину брюшка — длиннее уже кузнечик, а не саранча.</li>
 * </ul>
 *
 * <p>Крылья полупрозрачные, поэтому модель рисуется {@link RenderTypes#entityTranslucent}, а
 * не обычным cutout: тот умеет только «пиксель есть или нет» и любую полупрозрачность
 * округлил бы до непрозрачной.</p>
 *
 * <p>Развёртка 64×32 повторяет texOffs ниже: тело (0,0), крыло (17,0), переднеспинка (36,0),
 * голова (48,0), бедро (0,9), голень (9,9), лапка (18,9), усик (23,9).</p>
 */
public class LocustModel extends EntityModel<LocustRenderState> {
    private final ModelPart body;
    private final ModelPart thorax;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart leftShin;
    private final ModelPart rightShin;
    private final ModelPart leftAntenna;
    private final ModelPart rightAntenna;

    public LocustModel(final ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        this.body = root.getChild("body");
        this.thorax = root.getChild("thorax");
        this.head = root.getChild("head");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.leftShin = this.leftLeg.getChild("left_shin");
        this.rightShin = this.rightLeg.getChild("right_shin");
        this.leftAntenna = root.getChild("left_antenna");
        this.rightAntenna = root.getChild("right_antenna");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Брюшко.
        root.addOrReplaceChild(
                "body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 6.0F),
                PartPose.offset(0.0F, 22.0F, 0.0F));

        // Переднеспинка — тот самый горб за головой. Заканчивается на z = 0.3, и всё, что
        // должно быть видно, обязано начинаться позади этой границы.
        root.addOrReplaceChild(
                "thorax",
                CubeListBuilder.create().texOffs(36, 0).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 21.8F, -1.2F));

        root.addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 22.2F, -2.6F));

        // Усики тонкие и короткие: толстые торчат рогами, длинные — примета кузнечика.
        CubeListBuilder antenna = CubeListBuilder.create().texOffs(23, 9)
                .addBox(-0.25F, -0.25F, -2.0F, 0.5F, 0.5F, 2.0F);
        root.addOrReplaceChild("left_antenna", antenna,
                PartPose.offsetAndRotation(0.5F, 21.5F, -4.3F, -0.35F, 0.25F, 0.0F));
        root.addOrReplaceChild("right_antenna", antenna,
                PartPose.offsetAndRotation(-0.5F, 21.5F, -4.3F, -0.35F, -0.25F, 0.0F));

        // Крылья: широкие, лежат на спине и заходят за конец брюшка.
        //
        // Две тонкости, и обе выяснились на глаз. Толщина ровно 0.5, а не ноль: у плоской
        // коробки верх и низ совпадают в одной плоскости, и полупрозрачная текстура на них
        // мерцает — видеокарта каждый кадр заново решает, какая грань ближе. И начало на
        // z = 0.4, позади горба (тот кончается на 0.3): пока крыло начиналось раньше, его
        // основание торчало внутри переднеспинки и мерцало уже об неё.
        root.addOrReplaceChild(
                "left_wing",
                CubeListBuilder.create().texOffs(17, 0).addBox(0.0F, 0.0F, 0.0F, 3.0F, 0.5F, 6.0F),
                PartPose.offsetAndRotation(0.2F, 20.5F, 0.4F, 0.0F, 0.0F, -0.12F));
        root.addOrReplaceChild(
                "right_wing",
                CubeListBuilder.create().texOffs(17, 0).mirror()
                        .addBox(-3.0F, 0.0F, 0.0F, 3.0F, 0.5F, 6.0F),
                PartPose.offsetAndRotation(-0.2F, 20.5F, 0.4F, 0.0F, 0.0F, 0.12F));

        // Задние ноги: бедро вверх-назад, голень от колена вниз — «домик» выше спины.
        CubeListBuilder femur = CubeListBuilder.create().texOffs(0, 9)
                .addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F);
        CubeListBuilder shin = CubeListBuilder.create().texOffs(9, 9)
                .addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F);

        PartDefinition left = root.addOrReplaceChild("left_leg", femur,
                PartPose.offsetAndRotation(1.0F, 21.9F, 1.2F, -0.8F, 0.0F, -0.15F));
        left.addOrReplaceChild("left_shin", shin,
                PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 2.1F, 0.0F, 0.0F));

        PartDefinition right = root.addOrReplaceChild("right_leg", femur,
                PartPose.offsetAndRotation(-1.0F, 21.9F, 1.2F, -0.8F, 0.0F, 0.15F));
        right.addOrReplaceChild("right_shin", shin,
                PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 2.1F, 0.0F, 0.0F));

        // Передние и средние лапки: мелочь, но без них насекомое висит в воздухе брюхом.
        CubeListBuilder foot = CubeListBuilder.create().texOffs(18, 9)
                .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F);
        root.addOrReplaceChild("left_front_leg", foot,
                PartPose.offsetAndRotation(0.9F, 22.6F, -1.6F, 0.3F, 0.0F, -0.4F));
        root.addOrReplaceChild("right_front_leg", foot,
                PartPose.offsetAndRotation(-0.9F, 22.6F, -1.6F, 0.3F, 0.0F, 0.4F));
        root.addOrReplaceChild("left_mid_leg", foot,
                PartPose.offsetAndRotation(0.9F, 22.6F, 0.2F, 0.0F, 0.0F, -0.4F));
        root.addOrReplaceChild("right_mid_leg", foot,
                PartPose.offsetAndRotation(-0.9F, 22.6F, 0.2F, 0.0F, 0.0F, 0.4F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(final LocustRenderState state) {
        super.setupAnim(state);

        if (state.feeding) {
            // Села: крылья сложены домиком вдоль спины, голова уткнулась в грядку, задние
            // ноги поджаты — из этой позы саранча и прыгает.
            this.leftWing.zRot = -0.12F;
            this.rightWing.zRot = 0.12F;
            this.leftWing.yRot = 0.0F;
            this.rightWing.yRot = 0.0F;
            this.head.xRot = 0.6F;

            this.leftLeg.xRot = -1.0F;
            this.rightLeg.xRot = -1.0F;
            this.leftShin.xRot = 2.4F;
            this.rightShin.xRot = 2.4F;
        } else {
            // В полёте крылья разведены и дрожат — быстро и мелко, как у настоящей саранчи.
            float flap = state.ageInTicks * 2.2F;
            this.leftWing.zRot = -0.55F - Mth.cos(flap) * 0.45F;
            this.rightWing.zRot = 0.55F + Mth.cos(flap) * 0.45F;
            this.leftWing.yRot = -0.2F;
            this.rightWing.yRot = 0.2F;
            this.head.xRot = 0.0F;

            // Ноги вытянуты назад: в воздухе саранча их не поджимает.
            this.leftLeg.xRot = -0.55F;
            this.rightLeg.xRot = -0.55F;
            this.leftShin.xRot = 1.7F;
            this.rightShin.xRot = 1.7F;
        }

        // Брюшко чуть покачивается, усики поводят — насекомое не умеет замирать совсем.
        this.body.xRot = Mth.cos(state.ageInTicks * 0.2F) * 0.06F;
        this.thorax.xRot = this.body.xRot * 0.5F;

        float twitch = Mth.cos(state.ageInTicks * 0.35F) * 0.12F;
        this.leftAntenna.yRot = 0.25F + twitch;
        this.rightAntenna.yRot = -0.25F - twitch;
    }
}
