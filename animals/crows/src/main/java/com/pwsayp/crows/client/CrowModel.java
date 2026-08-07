package com.pwsayp.crows.client;

import com.pwsayp.anima.bird.client.FlightPose;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Модель вороны: силуэт и анимация.
 *
 * <p>Первая версия получалась «общипанным попугаем», и виноваты были не цвета, а
 * пропорции: голова-куб почти в размер тела, широкий плоский хвост торчком и крылья
 * лопастями по бокам. У живой вороны наоборот — <b>тело сужается к хвосту</b>: глубокая
 * грудь впереди, крестец тоньше и чуть выше, между корпусом и головой видна шея. Крыло
 * тоже составное: плечо лежит на боку, маховые уходят назад и накрывают основание
 * хвоста, ровно как у сидящей птицы.</p>
 *
 * <p>Оживляет птицу не размах движений, а мелочи, которых от неё ждёшь: <b>раскрытый на
 * крике клюв</b>, рывки головой на месте (птица не поворачивает голову плавно, она её
 * переставляет), покачивание корпуса в такт шагам и хвост, который отзывается на каждое
 * движение с запозданием.</p>
 *
 * <p>Развёртка 64×32 повторяет texOffs ниже; числа здесь и в
 * {@code tools/make_textures.py} обязаны совпадать.</p>
 */
public class CrowModel extends EntityModel<CrowRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart beakLower;
    private final ModelPart tail;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    /** Исходный наклон корпуса: анимация добавляет к нему, а не заменяет его. */
    private final float bodyRest;
    private final float tailRest;

    public CrowModel(final ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.beakLower = this.head.getChild("beak_lower");
        this.tail = root.getChild("tail");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");

        this.bodyRest = this.body.xRot;
        this.tailRest = this.tail.xRot;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Грудь — главный объём, с наклоном: посадка у вороны «нос вверх, хвост вниз».
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2.5F, -4.0F, 5.0F, 5.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 18.0F, 0.0F, 0.22F, 0.0F, 0.0F));

        // Крестец: уже и короче груди — отсюда сужение к хвосту.
        body.addOrReplaceChild("rump",
                CubeListBuilder.create().texOffs(18, 0).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -0.2F, 0.0F, -0.12F, 0.0F, 0.0F));

        // Шея короткая, но видимая: без неё голова садится на плечи кубом.
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(34, 0).addBox(-1.75F, -3.5F, -1.75F, 3.5F, 3.5F, 3.5F),
                PartPose.offset(0.0F, 15.8F, -3.6F));
        head.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(44, 9).addBox(-1.5F, -2.5F, -0.5F, 3.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.2F, 1.4F, 0.5F, 0.0F, 0.0F));
        head.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(0, 17).addBox(-0.75F, -2.5F, -5.0F, 1.5F, 1.5F, 4.0F),
                PartPose.ZERO);
        head.addOrReplaceChild("beak_lower",
                CubeListBuilder.create().texOffs(28, 17).addBox(-0.75F, -0.85F, -3.5F, 1.5F, 0.85F, 3.5F),
                PartPose.offset(0.0F, -1.65F, -1.5F));

        // Хвост в две ступени: короткие кроющие и длинные рулевые.
        PartDefinition tail = root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(32, 9).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 18.6F, 3.4F, 0.28F, 0.0F, 0.0F));
        tail.addOrReplaceChild("rectrices",
                CubeListBuilder.create().texOffs(12, 9).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 2.8F));

        // Крыло: плечо по боку, маховые назад и вниз, конец ложится на хвост.
        PartDefinition leftWing = root.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(48, 0).addBox(0.0F, 0.0F, -3.0F, 1.0F, 4.0F, 4.0F),
                PartPose.offset(2.4F, 16.6F, -1.4F));
        leftWing.addOrReplaceChild("left_primaries",
                CubeListBuilder.create().texOffs(0, 9).addBox(0.0F, 0.0F, 0.0F, 1.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(-0.1F, 0.6F, 1.0F, 0.12F, 0.0F, 0.0F));

        PartDefinition rightWing = root.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(48, 0).mirror()
                        .addBox(-1.0F, 0.0F, -3.0F, 1.0F, 4.0F, 4.0F),
                PartPose.offset(-2.4F, 16.6F, -1.4F));
        rightWing.addOrReplaceChild("right_primaries",
                CubeListBuilder.create().texOffs(0, 9).mirror()
                        .addBox(-1.0F, 0.0F, 0.0F, 1.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(0.1F, 0.6F, 1.0F, 0.12F, 0.0F, 0.0F));

        legs(root);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(final CrowRenderState state) {
        super.setupAnim(state);

        this.body.xRot = this.bodyRest;
        this.tail.xRot = this.tailRest;

        // Крен, тангаж и ровная голова — общая птичья поза из ядра.
        FlightPose.body(this.root(), this.head, state.bank, state.pitch);

        this.head(state);
        this.beak(state);
        this.wings(state);
        this.legsAndTail(state);
    }

    /**
     * Голова.
     *
     * <p>Птица не ведёт головой плавно — она её <b>переставляет</b>: замерла, дёрнула,
     * снова замерла. Поэтому к обычному слежению за игроком добавлен мелкий рывок по
     * ступеням времени. На кормёжке голова ныряет вниз, а рывки выключаются: там своя,
     * более резкая долбёжка.</p>
     */
    private void head(final CrowRenderState state) {
        if (state.pecking) {
            this.head.zRot = 0.0F;
            this.head.xRot = 0.95F + Mth.cos(state.ageInTicks * 0.8F) * 0.5F;
            this.head.yRot = 0.0F;
            return;
        }

        this.head.xRot += state.xRot * Mth.DEG_TO_RAD;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;

        if (!state.flying) {
            // Ступенчатая функция от времени: раз в полторы секунды новое положение, между
            // ними голова стоит. Фаза берётся из номера особи — постоянная величина, иначе
            // положение пересчитывается каждый кадр и голова трясётся.
            float step = (int) ((state.ageInTicks + state.phase) / 30.0F) * 1.7F;
            this.head.yRot += Mth.sin(step) * 0.3F;
            this.head.xRot += Mth.cos(step * 1.7F) * 0.1F;
        }
    }

    /**
     * Клюв.
     *
     * <p>На крике он раскрывается — и это самая заметная мелочь из всех: молча каркающая
     * птица выглядит куклой. Ворона не разевает клюв единожды на всю длину крика, а
     * приоткрывает его толчками, потому раскрытие считается по синусу от прогресса.</p>
     *
     * <p>На кормёжке клюв тоже приоткрыт: птица долбит зерно, а не тычется в грядку
     * сомкнутым носом.</p>
     */
    private void beak(final CrowRenderState state) {
        float gape = 0.0F;
        if (state.cawProgress > 0.0F) {
            // Три толчка за крик, с плавным затуханием к концу.
            float pulse = Mth.abs(Mth.sin(state.cawProgress * (float) Math.PI * 3.0F));
            gape = pulse * (1.0F - state.cawProgress * 0.35F) * 0.55F;
        } else if (state.pecking) {
            gape = 0.15F + Mth.abs(Mth.cos(state.ageInTicks * 0.8F)) * 0.2F;
        }
        this.beakLower.xRot = gape;
    }

    private void wings(final CrowRenderState state) {
        if (!state.flying) {
            FlightPose.fold(this.leftWing, this.rightWing);
            return;
        }

        if (state.gliding) {
            FlightPose.glide(this.leftWing, this.rightWing);
            return;
        }

        // Размах у вороны широкий: 0.95 радиана в каждую сторону от горизонтали.
        float bob = FlightPose.flap(this.leftWing, this.rightWing, state.wingPhase, 0.95F);
        this.body.y += bob;
        this.head.y += bob;
        this.tail.y += bob;
        this.leftWing.y += bob;
        this.rightWing.y += bob;
    }

    /**
     * Лапы и хвост.
     *
     * <p>Идущая ворона кивает всем корпусом — это её главная примета на земле, заметнее
     * любой детали модели. Хвост отзывается на шаг с запозданием и работает
     * противовесом.</p>
     */
    private void legsAndTail(final CrowRenderState state) {
        if (state.flying) {
            this.leftLeg.xRot = (float) (Math.PI * 2.0 / 9.0);
            this.rightLeg.xRot = (float) (Math.PI * 2.0 / 9.0);
            this.tail.xRot -= 0.2F;
            FlightPose.tail(this.tail, state.bank, state.pitch);
            return;
        }

        this.tail.yRot = 0.0F;

        float walk = state.walkAnimationPos * 0.6662F;
        // Размах шага ограничен: у летающей птицы он разгоняется до величин, на которых
        // кивок превращается в тряску.
        float amount = Math.min(state.walkAnimationSpeed, 1.0F);

        this.leftLeg.xRot = Mth.cos(walk) * 1.4F * amount;
        this.rightLeg.xRot = Mth.cos(walk + (float) Math.PI) * 1.4F * amount;

        // Кивок корпусом и головой в такт шагу — вперёд-назад, а не вверх-вниз.
        float nod = Mth.cos(walk) * 0.1F * amount;
        this.body.xRot += nod;
        this.head.xRot -= nod * 1.4F;
        this.head.z += Mth.sin(walk) * 0.3F * amount;

        // Хвост противовесом, с запозданием на четверть такта.
        this.tail.xRot += Mth.cos(walk - 1.57F) * 0.2F * amount;
    }

    /** Лапы: цевка и пальцы. Без пальцев птица «висит» над землёй на двух палках. */
    private static void legs(final PartDefinition root) {
        CubeListBuilder shank = CubeListBuilder.create().texOffs(12, 17)
                .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F);
        CubeListBuilder toes = CubeListBuilder.create().texOffs(17, 17)
                .addBox(-1.0F, 0.0F, -1.5F, 2.0F, 1.0F, 3.0F);

        PartDefinition left = root.addOrReplaceChild("left_leg", shank,
                PartPose.offset(1.1F, 21.0F, 0.5F));
        left.addOrReplaceChild("left_toes", toes, PartPose.offset(0.0F, 3.0F, -0.5F));

        PartDefinition right = root.addOrReplaceChild("right_leg", shank,
                PartPose.offset(-1.1F, 21.0F, 0.5F));
        right.addOrReplaceChild("right_toes", toes, PartPose.offset(0.0F, 3.0F, -0.5F));
    }
}
