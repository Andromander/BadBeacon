package com.androsa.badbeacon;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.AbstractButton;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CCloseWindowPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.Effects;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BadBeaconScreen extends ContainerScreen<BadBeaconContainer> {
    private static final ResourceLocation BEACON_GUI_TEXTURES = new ResourceLocation(BadBeaconMod.MODID, "textures/gui/container/beacon.png");
    private BadBeaconScreen.ConfirmButton beaconConfirmButton;
    private boolean buttonsNotDrawn;
    private Effect primaryEffect;
    private Effect secondaryEffect;

    public BadBeaconScreen(final BadBeaconContainer container, PlayerInventory inventory, ITextComponent component) {
        super(container, inventory, component);
        this.xSize = 230;
        this.ySize = 219;
        container.addListener(new IContainerListener() {

            @Override
            public void sendAllContents(Container containerToSend, NonNullList<ItemStack> itemsList) {
            }

            @Override
            public void sendSlotContents(Container containerToSend, int slotInd, ItemStack stack) {
            }

            @Override
            public void sendWindowProperty(Container containerIn, int varToUpdate, int newValue) {
                BadBeaconScreen.this.primaryEffect = container.getPrimaryEffect();
                BadBeaconScreen.this.secondaryEffect = container.getSecondaryEffect();
                BadBeaconScreen.this.buttonsNotDrawn = true;
            }
        });
    }

    @Override
    protected void init() {
        super.init();
        this.beaconConfirmButton = this.addButton(new BadBeaconScreen.ConfirmButton(this.guiLeft + 164, this.guiTop + 107));
        this.addButton(new BadBeaconScreen.CancelButton(this.guiLeft + 190, this.guiTop + 107));
        this.buttonsNotDrawn = true;
        this.beaconConfirmButton.active = false;
    }

    @Override
    public void tick() {
        super.tick();
        int level = this.container.getLevels();
        if (this.buttonsNotDrawn && level >= 0) {
            this.buttonsNotDrawn = false;

            for(int j = 0; j <= 2; ++j) {
                int k = BadBeaconTileEntity.EFFECTS_LIST[j].length;
                int l = k * 22 + (k - 1) * 2;

                for(int i1 = 0; i1 < k; ++i1) {
                    Effect effect = BadBeaconTileEntity.EFFECTS_LIST[j][i1];
                    BadBeaconScreen.PowerButton beaconscreen$powerbutton = new BadBeaconScreen.PowerButton(this.guiLeft + 76 + i1 * 24 - l / 2, this.guiTop + 22 + j * 25, effect, true);
                    this.addButton(beaconscreen$powerbutton);
                    if (j >= level) {
                        beaconscreen$powerbutton.active = false;
                    } else if (effect == this.primaryEffect) {
                        beaconscreen$powerbutton.setSelected(true);
                    }
                }
            }

            int k1 = BadBeaconTileEntity.EFFECTS_LIST[3].length + 1;
            int l1 = k1 * 22 + (k1 - 1) * 2;

            for(int i2 = 0; i2 < k1 - 1; ++i2) {
                Effect effect1 = BadBeaconTileEntity.EFFECTS_LIST[3][i2];
                BadBeaconScreen.PowerButton beaconscreen$powerbutton2 = new BadBeaconScreen.PowerButton(this.guiLeft + 167 + i2 * 24 - l1 / 2, this.guiTop + 47, effect1, false);
                this.addButton(beaconscreen$powerbutton2);
                if (3 >= level) {
                    beaconscreen$powerbutton2.active = false;
                } else if (effect1 == this.secondaryEffect) {
                    beaconscreen$powerbutton2.setSelected(true);
                }
            }

            if (this.primaryEffect != null) {
                BadBeaconScreen.PowerButton beaconscreen$powerbutton1 = new BadBeaconScreen.PowerButton(this.guiLeft + 167 + (k1 - 1) * 24 - l1 / 2, this.guiTop + 47, this.primaryEffect, false);
                this.addButton(beaconscreen$powerbutton1);
                if (3 >= level) {
                    beaconscreen$powerbutton1.active = false;
                } else if (this.primaryEffect == this.secondaryEffect) {
                    beaconscreen$powerbutton1.setSelected(true);
                }
            }
        }

        this.beaconConfirmButton.active = this.container.isActive() && this.primaryEffect != null;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack stack, int mouseX, int mouseY) {
        drawCenteredString(stack, this.font, I18n.format("block.minecraft.beacon.primary"), 62, 10, 14737632);
        drawCenteredString(stack, this.font, I18n.format("block.minecraft.beacon.secondary"), 169, 10, 14737632);

        for(Widget widget : this.buttons) {
            if (widget.isHovered()) {
                widget.renderToolTip(stack, mouseX - this.guiLeft, mouseY - this.guiTop);
                break;
            }
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack stack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(BEACON_GUI_TEXTURES);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        this.blit(stack, x, y, 0, 0, this.xSize, this.ySize);
        this.itemRenderer.zLevel = 100.0F;
        this.itemRenderer.renderItemAndEffectIntoGUI(new ItemStack(Items.BONE), x + 20, y + 109);
        this.itemRenderer.renderItemAndEffectIntoGUI(new ItemStack(Items.COAL), x + 41, y + 109);
        this.itemRenderer.renderItemAndEffectIntoGUI(new ItemStack(Items.LAPIS_LAZULI), x + 41 + 22, y + 109);
        this.itemRenderer.renderItemAndEffectIntoGUI(new ItemStack(Items.REDSTONE), x + 42 + 44, y + 109);
        this.itemRenderer.renderItemAndEffectIntoGUI(new ItemStack(Items.QUARTZ), x + 42 + 66, y + 109);
        this.itemRenderer.zLevel = 0.0F;
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseZ, float ticks) {
        this.renderBackground(stack);
        super.render(stack, mouseX, mouseZ, ticks);
        this.func_230459_a_(stack, mouseX, mouseZ); //renderHoveredToolTip
    }

    @OnlyIn(Dist.CLIENT)
    abstract static class Button extends AbstractButton {
        private boolean selected;

        protected Button(int x, int y) {
            super(x, y, 22, 22, StringTextComponent.EMPTY);
        }

        @Override
        public void renderButton(MatrixStack stack, int backX, int backY, float partial) {
            Minecraft.getInstance().getTextureManager().bindTexture(BadBeaconScreen.BEACON_GUI_TEXTURES);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            int j = 0;
            if (!this.active) {
                j += this.width * 2;
            } else if (this.selected) {
                j += this.width * 1;
            } else if (this.isHovered()) {
                j += this.width * 3;
            }

            this.blit(stack, this.x, this.y, j, 219, this.width, this.height);
            this.blitButton(stack);
        }

        protected abstract void blitButton(MatrixStack stack);

        public boolean isSelected() {
            return this.selected;
        }

        public void setSelected(boolean selectedIn) {
            this.selected = selectedIn;
        }
    }

    @OnlyIn(Dist.CLIENT)
    class PowerButton extends BadBeaconScreen.Button {
        private final Effect effect;
        private final TextureAtlasSprite textureSprite;
        private final boolean isPrimary;

        public PowerButton(int x, int y, Effect effectIn, boolean primary) {
            super(x, y);
            this.effect = effectIn;
            this.textureSprite = Minecraft.getInstance().getPotionSpriteUploader().getSprite(effectIn);
            this.isPrimary = primary;
        }

        @Override
        public void onPress() {
            if (!this.isSelected()) {
                if (this.isPrimary) {
                    BadBeaconScreen.this.primaryEffect = this.effect;
                } else {
                    BadBeaconScreen.this.secondaryEffect = this.effect;
                }

                BadBeaconScreen.this.buttons.clear();
                BadBeaconScreen.this.children.clear();
                BadBeaconScreen.this.init();
                BadBeaconScreen.this.tick();
            }
        }

        @Override
        public void renderToolTip(MatrixStack stack, int x, int y) {
            IFormattableTextComponent effectname = new TranslationTextComponent(this.effect.getName());
            if (!this.isPrimary && this.effect != Effects.POISON) {
                effectname.appendString(" II");
            }

            BadBeaconScreen.this.renderTooltip(stack, effectname, x, y);
        }

        @Override
        protected void blitButton(MatrixStack stack) {
            Minecraft.getInstance().getTextureManager().bindTexture(textureSprite.getAtlasTexture().getTextureLocation());
            blit(stack, this.x + 2, this.y + 2, this.getBlitOffset(), 18, 18, this.textureSprite);
        }
    }

    @OnlyIn(Dist.CLIENT)
    abstract static class SpriteButton extends BadBeaconScreen.Button {
        private final int field_212948_a;
        private final int field_212949_b;

        protected SpriteButton(int x, int y, int offsetX, int offsetY) {
            super(x, y);
            this.field_212948_a = offsetX;
            this.field_212949_b = offsetY;
        }

        @Override
        protected void blitButton(MatrixStack stack) {
            this.blit(stack, this.x + 2, this.y + 2, this.field_212948_a, this.field_212949_b, 18, 18);
        }
    }

    @OnlyIn(Dist.CLIENT)
    class ConfirmButton extends BadBeaconScreen.SpriteButton {
        public ConfirmButton(int x, int y) {
            super(x, y, 90, 220);
        }

        @Override
        public void onPress() {
            PacketHandler.sendToServer(new CUpdateBadBeaconPacket(Effect.getId(BadBeaconScreen.this.primaryEffect), Effect.getId(BadBeaconScreen.this.secondaryEffect)));
            BadBeaconScreen.this.minecraft.player.connection.sendPacket(new CCloseWindowPacket(BadBeaconScreen.this.minecraft.player.openContainer.windowId));
            BadBeaconScreen.this.minecraft.displayGuiScreen(null);
        }

        @Override
        public void renderToolTip(MatrixStack stack, int x, int y) {
            BadBeaconScreen.this.renderTooltip(stack, DialogTexts.field_240632_c_, x, y);
        }
    }

    @OnlyIn(Dist.CLIENT)
    class CancelButton extends BadBeaconScreen.SpriteButton {
        public CancelButton(int x, int y) {
            super(x, y, 112, 220);
        }

        @Override
        public void onPress() {
            BadBeaconScreen.this.minecraft.player.connection.sendPacket(new CCloseWindowPacket(BadBeaconScreen.this.minecraft.player.openContainer.windowId));
            BadBeaconScreen.this.minecraft.displayGuiScreen(null);
        }

        @Override
        public void renderToolTip(MatrixStack stack, int x, int y) {
            BadBeaconScreen.this.renderTooltip(stack, DialogTexts.field_240633_d_, x, y);
        }
    }
}
