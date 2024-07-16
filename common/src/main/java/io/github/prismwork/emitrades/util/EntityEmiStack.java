package io.github.prismwork.emitrades.util;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.screen.tooltip.RemainderTooltipComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.component.ComponentChanges;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("DuplicatedCode")
public class EntityEmiStack extends EmiStack {
    private final @Nullable Entity entity;
    private final float scale;

    protected EntityEmiStack(@Nullable Entity entity, float scale) {
        this.entity = entity;
        this.scale = scale;
    }

    public static EntityEmiStack of(@Nullable Entity entity) {
        return ofScaled(entity, 8.0f);
    }

    public static EntityEmiStack ofScaled(@Nullable Entity entity, float scale) {
        return new EntityEmiStack(entity, scale);
    }

    @Override
    public EmiStack copy() {
        EntityEmiStack stack = ofScaled(entity, scale);
        stack.setRemainder(getRemainder().copy());
        stack.comparison = comparison;
        return stack;
    }

    @Override
    public boolean isEmpty() {
        return entity == null;
    }

    @Override
    public void render(DrawContext draw, int x, int y, float delta, int flags) {
        if (entity != null) {
            Mouse mouse = MinecraftClient.getInstance().mouse;
            if (entity instanceof LivingEntity living)
                drawLivingEntity(draw, x, y, scale, (float) mouse.getX(), (float) mouse.getY(), living);
            else
                drawEntity(draw, x, y, scale, (float) mouse.getX(), (float) mouse.getY(), entity);
        }
    }

    @Override
    public ComponentChanges getComponentChanges() {
        return ComponentChanges.EMPTY;
    }

    @Override
    public Object getKey() {
        return entity;
    }

    @Override
    public Identifier getId() {
        if (entity == null) throw new RuntimeException("Entity is null");
        return Registries.ENTITY_TYPE.getId(entity.getType());
    }

    @Override
    public List<Text> getTooltipText() {
        return List.of(getName());
    }

    @Override
    public List<TooltipComponent> getTooltip() {
        List<TooltipComponent> list = new ArrayList<>();
        if (entity != null) {
            list.addAll(getTooltipText().stream().map(EmiPort::ordered).map(TooltipComponent::of).toList());
            String mod;
            if (entity instanceof VillagerEntity villager) {
                mod = EmiUtil.getModName(Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession()).getNamespace());
            } else {
                mod = EmiUtil.getModName(Registries.ENTITY_TYPE.getId(entity.getType()).getNamespace());
            }
            list.add(TooltipComponent.of(EmiPort.ordered(EmiPort.literal(mod, Formatting.BLUE, Formatting.ITALIC))));
            if (!getRemainder().isEmpty()) {
                list.add(new RemainderTooltipComponent(this));
            }
        }
        return list;
    }

    @Override
    public Text getName() {
        return entity != null ? entity.getName() : EmiPort.literal("yet another missingno");
    }

    public static void drawLivingEntity(DrawContext ctx, int x, int y, float size, float mouseX, float mouseY, LivingEntity entity) {
        float mouseX0 = (ctx.getScaledWindowWidth() + 51) - mouseX;
        float mouseY0 = (ctx.getScaledWindowHeight() + 75 - 50) - mouseY;
        float f = (float) Math.atan(mouseX0 / 40.0F);
        float g = (float) Math.atan(mouseY0 / 40.0F);
        Quaternionf quaternionf = (new Quaternionf()).rotateZ(3.1415927F);
        Quaternionf quaternionf2 = (new Quaternionf()).rotateX(g * 20.0F * 0.017453292F);
        quaternionf.mul(quaternionf2);
        float h = entity.bodyYaw;
        float i = entity.getYaw();
        float j = entity.getPitch();
        float k = entity.prevHeadYaw;
        float l = entity.headYaw;
        entity.bodyYaw = 180.0F + f * 20.0F;
        entity.setYaw(180.0F + f * 40.0F);
        entity.setPitch(-g * 20.0F);
        entity.headYaw = entity.getYaw();
        entity.prevHeadYaw = entity.getYaw();
        draw(ctx, x, y, size, quaternionf, quaternionf2, entity);
        entity.bodyYaw = h;
        entity.setYaw(i);
        entity.setPitch(j);
        entity.prevHeadYaw = k;
        entity.headYaw = l;
    }

    public static void drawEntity(DrawContext ctx, int x, int y, float size, float mouseX, float mouseY, Entity entity) {
        float mouseX0 = (ctx.getScaledWindowWidth() + 51) - mouseX;
        float mouseY0 = (ctx.getScaledWindowHeight() + 75 - 50) - mouseY;
        float f = (float) Math.atan(mouseX0 / 40.0F);
        float g = (float) Math.atan(mouseY0 / 40.0F);
        Quaternionf quaternionf = (new Quaternionf()).rotateZ(3.1415927F);
        Quaternionf quaternionf2 = (new Quaternionf()).rotateX(g * 20.0F * 0.017453292F);
        quaternionf.mul(quaternionf2);
        float i = entity.getYaw();
        float j = entity.getPitch();
        entity.setYaw(180.0F + f * 40.0F);
        entity.setPitch(-g * 20.0F);
        draw(ctx, x, y, size, quaternionf, quaternionf2, entity);
        entity.setYaw(i);
        entity.setPitch(j);
    }

    @SuppressWarnings("deprecation")
    private static void draw(DrawContext ctx, int x, int y, float size, Quaternionf quaternion, @Nullable Quaternionf quaternion2, Entity entity) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + 8, y + 16, 50.0);
        ctx.getMatrices().multiplyPositionMatrix((new Matrix4f()).scaling(size, size, -size));
        ctx.getMatrices().multiply(quaternion);
        DiffuseLighting.method_34742();
        EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        if (quaternion2 != null) {
            quaternion2.conjugate();
            dispatcher.setRotation(quaternion2);
        }

        dispatcher.setRenderShadows(false);
        RenderSystem.runAsFancy(() ->
                dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0F, 1.0F, ctx.getMatrices(), ctx.getVertexConsumers(), 15728880)
        );
        ctx.draw();
        dispatcher.setRenderShadows(true);
        ctx.getMatrices().pop();
        DiffuseLighting.enableGuiDepthLighting();
    }
}
