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
 * <p>Особь размером с треть блока, поэтому держится всё на силуэте. С натуры взяты четыре
 * приметы, и без любой из них насекомое перестаёт быть саранчой:</p>
 *
 * <ul>
 *   <li><b>Переднеспинка щитом</b> — жёсткое седло за головой, <b>шире самого тела</b> и
 *       с провисающими боками. Это первое, что видно на любом фото, и первое, чего у нас
 *       не было: горбик в размер туловища читался как обычный жук.</li>
 *   <li><b>Бедро-окорочок</b> — задняя нога у прямокрылых устроена как молоток: толстое
 *       короткое бедро и вдвое более тонкая длинная голень. Пока обе части были одной
 *       толщины, нога выглядела палкой от паука.</li>
 *   <li><b>Крылья за конец брюшка</b> — сложенные надкрылья длиннее тела и торчат за
 *       хвостом узким клином. Короткие крылья превращают саранчу в кузнечика.</li>
 *   <li><b>Крупная голова с большим глазом</b>, посаженная наклонно, и короткие усики.</li>
 * </ul>
 *
 * <p>Брюшко составное: толстое основание и суженный хвост. Коробкой сужение не передать,
 * но двумя коробками разного размера — вполне, и силуэт сразу перестаёт быть бруском.</p>
 *
 * <p>Крылья полупрозрачные, поэтому модель рисуется {@link RenderTypes#entityTranslucent}, а
 * не обычным cutout: тот умеет только «пиксель есть или нет». Толщина у крыла ненулевая
 * (0.5) — у плоской коробки верх и низ лежат в одной плоскости, и полупрозрачная текстура
 * на них мерцает.</p>
 *
 * <p>Про крылья отдельно, потому что это была настоящая ошибка. Пара была одна: длинное
 * узкое надкрылье, которое на взмахе поворачивалось вокруг своей оси и то и дело
 * оказывалось к зрителю ребром — прямоугольник схлопывался в полоску и обратно. У живой
 * саранчи так не бывает: <b>жёсткие надкрылья почти неподвижны и лишь разведены в стороны,
 * а машет вторая пара — широкая и прозрачная</b>. Так теперь и здесь.</p>
 *
 * <p>Развёртка 64×32: брюшко (0,0), хвост (0,8), надкрылье (16,0), летательное крыло (36,0),
 * переднеспинка (36,6), голова (50,6), бедро (0,13), голень (12,13), лапка (22,13),
 * усик (26,13).</p>
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

    /** Летательная пара — та, что машет. */
    private final ModelPart leftHindwing;
    private final ModelPart rightHindwing;

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
        this.leftHindwing = root.getChild("left_hindwing");
        this.rightHindwing = root.getChild("right_hindwing");
    }

    public static LayerDefinition createBodyLayer() {

        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 5.0F),
                PartPose.offset(0.0F, 22.0F, 0.3F));
        body.addOrReplaceChild(
                "abdomen_tip",
                CubeListBuilder.create().texOffs(0, 8).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.5F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -0.2F, 4.0F, -0.12F, 0.0F, 0.0F));

        // Щит переднеспинки — шире тела.
        root.addOrReplaceChild(
                "thorax",
                CubeListBuilder.create().texOffs(36, 6).addBox(-1.5F, -1.25F, -1.5F, 3.0F, 2.5F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 21.9F, -1.4F, 0.1F, 0.0F, 0.0F));

        root.addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(50, 6).addBox(-1.25F, -1.5F, -2.0F, 2.5F, 2.5F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 22.1F, -2.8F, 0.25F, 0.0F, 0.0F));

        antennae(root);

        // Надкрылья: жёсткие, длинные, почти неподвижные.
        root.addOrReplaceChild(
                "left_wing",
                CubeListBuilder.create().texOffs(16, 0).addBox(0.0F, 0.0F, 0.0F, 2.0F, 0.5F, 7.0F),
                PartPose.offsetAndRotation(0.15F, 20.6F, -0.9F, 0.0F, 0.0F, -0.16F));
        root.addOrReplaceChild(
                "right_wing",
                CubeListBuilder.create().texOffs(16, 0).mirror()
                        .addBox(-2.0F, 0.0F, 0.0F, 2.0F, 0.5F, 7.0F),
                PartPose.offsetAndRotation(-0.15F, 20.6F, -0.9F, 0.0F, 0.0F, 0.16F));

        // Летательная пара: шире, короче, прозрачнее. В покое прячется под надкрыльями.
        root.addOrReplaceChild(
                "left_hindwing",
                CubeListBuilder.create().texOffs(36, 0).addBox(0.0F, 0.0F, 0.0F, 3.0F, 0.5F, 5.0F),
                PartPose.offsetAndRotation(0.2F, 21.1F, -0.7F, 0.0F, 0.0F, -0.05F));
        root.addOrReplaceChild(
                "right_hindwing",
                CubeListBuilder.create().texOffs(36, 0).mirror()
                        .addBox(-3.0F, 0.0F, 0.0F, 3.0F, 0.5F, 5.0F),
                PartPose.offsetAndRotation(-0.2F, 21.1F, -0.7F, 0.0F, 0.0F, 0.05F));

        hindLegs(root);
        smallLegs(root);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(final LocustRenderState state) {
        super.setupAnim(state);

        if (state.feeding) {
            // Села: крылья сложены домиком вдоль спины, голова уткнулась в грядку, задние
            // ноги поджаты — из этой позы саранча и прыгает.
            this.leftWing.zRot = -0.16F;
            this.rightWing.zRot = 0.16F;
            this.leftWing.yRot = 0.0F;
            this.rightWing.yRot = 0.0F;
            this.hindwings(-0.05F, 0.0F);
            this.head.xRot = 0.7F;

            this.leftLeg.xRot = -1.1F;
            this.rightLeg.xRot = -1.1F;
            this.leftShin.xRot = 2.5F;
            this.rightShin.xRot = 2.5F;
        } else {
            float flap = state.ageInTicks * 2.2F;

            // Надкрылья в полёте почти неподвижны — лишь разведены в стороны. Машет
            // вторая пара.
            this.leftWing.zRot = -0.5F;
            this.rightWing.zRot = 0.5F;
            this.leftWing.yRot = -0.12F;
            this.rightWing.yRot = 0.12F;
            this.hindwings(-0.35F - Mth.cos(flap) * 0.75F, -0.18F);
            this.head.xRot = 0.25F;

            // Ноги вытянуты назад: в воздухе саранча их не поджимает.
            this.leftLeg.xRot = -0.6F;
            this.rightLeg.xRot = -0.6F;
            this.leftShin.xRot = 1.7F;
            this.rightShin.xRot = 1.7F;
        }

        // Брюшко чуть покачивается, усики поводят — насекомое не умеет замирать совсем.
        this.body.xRot = Mth.cos(state.ageInTicks * 0.2F) * 0.06F;
        this.thorax.xRot = 0.1F + this.body.xRot * 0.5F;

        float twitch = Mth.cos(state.ageInTicks * 0.35F) * 0.12F;
        this.leftAntenna.yRot = 0.22F + twitch;
        this.rightAntenna.yRot = -0.22F - twitch;
    }

    private void hindwings(final float zRot, final float yRot) {
        this.leftHindwing.zRot = zRot;
        this.rightHindwing.zRot = -zRot;
        this.leftHindwing.yRot = yRot;
        this.rightHindwing.yRot = -yRot;
    }

    /** Усики короткие: длинные — примета кузнечика, а не саранчи. */
    private static void antennae(final PartDefinition root) {
        CubeListBuilder antenna = CubeListBuilder.create().texOffs(26, 13)
                .addBox(-0.25F, -0.25F, -2.0F, 0.5F, 0.5F, 2.0F);
        root.addOrReplaceChild("left_antenna", antenna,
                PartPose.offsetAndRotation(0.55F, 21.2F, -4.2F, -0.3F, 0.22F, 0.0F));
        root.addOrReplaceChild("right_antenna", antenna,
                PartPose.offsetAndRotation(-0.55F, 21.2F, -4.2F, -0.3F, -0.22F, 0.0F));
    }

    /**
     * Задние ноги молотком: толстое короткое бедро и вдвое более тонкая длинная голень.
     *
     * <p>Пока обе части были одной толщины, нога выглядела паучьей палкой. Разница толщин —
     * и есть то, по чему прямокрылое узнаётся мгновенно.</p>
     */
    private static void hindLegs(final PartDefinition root) {
        CubeListBuilder femur = CubeListBuilder.create().texOffs(0, 13)
                .addBox(-0.75F, -1.0F, 0.0F, 1.5F, 2.0F, 4.0F);
        CubeListBuilder shin = CubeListBuilder.create().texOffs(12, 13)
                .addBox(-0.25F, -0.25F, 0.0F, 0.5F, 0.5F, 4.0F);

        PartDefinition left = root.addOrReplaceChild("left_leg", femur,
                PartPose.offsetAndRotation(1.15F, 21.7F, 0.6F, -0.9F, 0.0F, -0.12F));
        left.addOrReplaceChild("left_shin", shin,
                PartPose.offsetAndRotation(0.0F, 0.0F, 3.8F, 2.2F, 0.0F, 0.0F));

        PartDefinition right = root.addOrReplaceChild("right_leg", femur,
                PartPose.offsetAndRotation(-1.15F, 21.7F, 0.6F, -0.9F, 0.0F, 0.12F));
        right.addOrReplaceChild("right_shin", shin,
                PartPose.offsetAndRotation(0.0F, 0.0F, 3.8F, 2.2F, 0.0F, 0.0F));
    }

    /** Передние и средние лапки: на них насекомое и стоит. */
    private static void smallLegs(final PartDefinition root) {
        CubeListBuilder foot = CubeListBuilder.create().texOffs(22, 13)
                .addBox(-0.25F, 0.0F, -0.25F, 0.5F, 2.0F, 0.5F);
        root.addOrReplaceChild("left_front_leg", foot,
                PartPose.offsetAndRotation(0.9F, 22.6F, -2.1F, 0.35F, 0.0F, -0.5F));
        root.addOrReplaceChild("right_front_leg", foot,
                PartPose.offsetAndRotation(-0.9F, 22.6F, -2.1F, 0.35F, 0.0F, 0.5F));
        root.addOrReplaceChild("left_mid_leg", foot,
                PartPose.offsetAndRotation(1.0F, 22.6F, -0.4F, 0.0F, 0.0F, -0.55F));
        root.addOrReplaceChild("right_mid_leg", foot,
                PartPose.offsetAndRotation(-1.0F, 22.6F, -0.4F, 0.0F, 0.0F, 0.55F));
    }
}
