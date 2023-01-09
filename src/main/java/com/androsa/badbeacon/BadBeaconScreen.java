package com.androsa.badbeacon;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class BadBeaconScreen extends AbstractContainerScreen<BadBeaconMenu> {
    private static final ResourceLocation BEACON_GUI_TEXTURES = new ResourceLocation(BadBeaconMod.MODID, "textures/gui/container/beacon.png");
    private static final Component PRIMARY_EFFECT_NAME = Component.translatable("block.minecraft.beacon.primary");
    private static final Component SECONDARY_EFFECT_NAME = Component.translatable("block.minecraft.beacon.secondary");
    private final List<BadBeaconButton> buttons = Lists.newArrayList();
    private MobEffect primaryEffect;
    private MobEffect secondaryEffect;

    public BadBeaconScreen(final BadBeaconMenu container, Inventory inventory, Component component) {
        super(container, inventory, component);
        this.imageWidth = 230;
        this.imageHeight = 219;
        container.addSlotListener(new ContainerListener() {
            @Override
            public void slotChanged(AbstractContainerMenu containerToSend, int slotInd, ItemStack stack) {
            }

            @Override
            public void dataChanged(AbstractContainerMenu containerIn, int varToUpdate, int newValue) {
                BadBeaconScreen.this.primaryEffect = container.getPrimaryEffect();
                BadBeaconScreen.this.secondaryEffect = container.getSecondaryEffect();
            }
        });
    }

    @Override
    protected void init() {
        super.init();
        this.buttons.clear();
        this.addButton(new BadBeaconScreen.ConfirmButton(this.leftPos + 164, this.topPos + 107));
        this.addButton(new BadBeaconScreen.CancelButton(this.leftPos + 190, this.topPos + 107));

		for(int tier = 0; tier <= 2; ++tier) {
			int s1 = BadBeaconBlockEntity.EFFECTS_LIST[tier].length;
			int w1 = s1 * 22 + (s1 - 1) * 2;

			for(int i = 0; i < s1; ++i) {
				MobEffect primary = BadBeaconBlockEntity.EFFECTS_LIST[tier][i];
				BadBeaconScreen.PowerButton powerbutton1 = new BadBeaconScreen.PowerButton(this.leftPos + 76 + i * 24 - w1 / 2, this.topPos + 22 + tier * 25, primary, true, tier);
				powerbutton1.active = false;
				this.addButton(powerbutton1);
			}
		}

		int t3 = BadBeaconBlockEntity.EFFECTS_LIST[3].length + 1;
		int w2 = t3 * 22 + (t3 - 1) * 2;

		for(int tier2 = 0; tier2 < t3 - 1; ++tier2) {
			MobEffect secondary = BadBeaconBlockEntity.EFFECTS_LIST[3][tier2];
			BadBeaconScreen.PowerButton powerbutton2 = new BadBeaconScreen.PowerButton(this.leftPos + 167 + tier2 * 24 - w2 / 2, this.topPos + 47, secondary, false, 3);
			powerbutton2.active = false;
			this.addButton(powerbutton2);
		}

		BadBeaconScreen.PowerButton upgradebutton = new BadBeaconScreen.UpgradeButton(this.leftPos + 167 + (t3 - 1) * 24 - w2 / 2, this.topPos + 47, BeaconBlockEntity.BEACON_EFFECTS[0][0]);
		upgradebutton.visible = false;
		this.addButton(upgradebutton);
    }

	private <T extends AbstractWidget & BadBeaconScreen.BadBeaconButton> void addButton(T button) {
		this.addRenderableWidget(button);
		this.buttons.add(button);
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		this.updateButtons();
	}

	void updateButtons() {
		int levels = this.menu.getLevels();
		this.buttons.forEach((button) -> button.updateStatus(levels));
	}

	@Override
    protected void renderLabels(PoseStack stack, int mouseX, int mouseY) {
        drawCenteredString(stack, this.font, PRIMARY_EFFECT_NAME, 62, 10, 14737632);
        drawCenteredString(stack, this.font, SECONDARY_EFFECT_NAME, 169, 10, 14737632);

        for(BadBeaconButton button : this.buttons) {
            if (button.isShowingTooltip()) {
                button.renderToolTipFuck(stack, mouseX - this.leftPos, mouseY - this.topPos);
                break;
            }
        }
    }

    @Override
    protected void renderBg(PoseStack stack, float partialTicks, int mouseX, int mouseY) {
    	RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, BEACON_GUI_TEXTURES);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        this.blit(stack, x, y, 0, 0, this.imageWidth, this.imageHeight);
        this.itemRenderer.blitOffset = 100.0F;
        this.itemRenderer.renderAndDecorateItem(new ItemStack(Items.COPPER_INGOT), x + 20, y + 109);
        this.itemRenderer.renderAndDecorateItem(new ItemStack(Items.COAL), x + 41, y + 109);
        this.itemRenderer.renderAndDecorateItem(new ItemStack(Items.LAPIS_LAZULI), x + 41 + 22, y + 109);
        this.itemRenderer.renderAndDecorateItem(new ItemStack(Items.REDSTONE), x + 42 + 44, y + 109);
        this.itemRenderer.renderAndDecorateItem(new ItemStack(Items.QUARTZ), x + 42 + 66, y + 109);
        this.itemRenderer.blitOffset = 0.0F;
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseZ, float ticks) {
        this.renderBackground(stack);
        super.render(stack, mouseX, mouseZ, ticks);
        this.renderTooltip(stack, mouseX, mouseZ);
    }

    @OnlyIn(Dist.CLIENT)
	interface BadBeaconButton {
    	boolean isShowingTooltip();

    	void updateStatus(int id);

    	void renderToolTipFuck(PoseStack stack, int x, int z);
	}

    @OnlyIn(Dist.CLIENT)
    abstract static class Button extends AbstractButton implements BadBeaconButton {
        private boolean selected;

        protected Button(int x, int y) {
            super(x, y, 22, 22, CommonComponents.EMPTY);
        }

        protected Button(int x, int y, Component component) {
        	super(x, y, 22, 22, component);
		}

        @Override
        public void renderButton(PoseStack stack, int backX, int backY, float partial) {
        	RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, BadBeaconScreen.BEACON_GUI_TEXTURES);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            int j = 0;
            if (!this.active) {
                j += this.width * 2;
            } else if (this.selected) {
                j += this.width * 1;
            } else if (this.isHoveredOrFocused()) {
                j += this.width * 3;
            }

            this.blit(stack, this.getX(), this.getY(), j, 219, this.width, this.height);
            this.blitButton(stack);
        }

        protected abstract void blitButton(PoseStack stack);

        public boolean isSelected() {
            return this.selected;
        }

        public void setSelected(boolean selectedIn) {
            this.selected = selectedIn;
        }

		@Override
		public boolean isShowingTooltip() {
			return this.isHovered;
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			this.defaultButtonNarrationText(output);
		}
	}

    @OnlyIn(Dist.CLIENT)
    class PowerButton extends BadBeaconScreen.Button {
		private final boolean isPrimary;
		protected final int tier;
        private MobEffect effect;
        private TextureAtlasSprite textureSprite;
        private Component tooltip;

        public PowerButton(int x, int y, MobEffect effectIn, boolean primary, int tier) {
            super(x, y);
            this.isPrimary = primary;
            this.tier = tier;
            this.setEffect(effectIn);
        }

        protected void setEffect(MobEffect effect) {
        	this.effect = effect;
        	this.textureSprite = Minecraft.getInstance().getMobEffectTextures().get(effect);
        	this.tooltip = this.createDescription(effect);
		}

		protected MutableComponent createDescription(MobEffect effect) {
        	return Component.translatable(effect.getDescriptionId());
		}

        @Override
        public void onPress() {
            if (!this.isSelected()) {
                if (this.isPrimary) {
                    BadBeaconScreen.this.primaryEffect = this.effect;
                } else {
                    BadBeaconScreen.this.secondaryEffect = this.effect;
                }

                BadBeaconScreen.this.updateButtons();
            }
        }

        @Override
        public void renderToolTipFuck(PoseStack stack, int x, int y) {
            BadBeaconScreen.this.renderTooltip(stack, this.tooltip, x, y);
        }

        @Override
        protected void blitButton(PoseStack stack) {
            RenderSystem.setShaderTexture(0, textureSprite.atlasLocation());
            blit(stack, this.getX() + 2, this.getY() + 2, this.getBlitOffset(), 18, 18, this.textureSprite);
        }

		@Override
		public void updateStatus(int id) {
			this.active = this.tier < id;
			this.setSelected(this.effect == (this.isPrimary ? BadBeaconScreen.this.primaryEffect : BadBeaconScreen.this.secondaryEffect));
		}

		@Override
		protected MutableComponent createNarrationMessage() {
			return this.createDescription(this.effect);
		}
	}

    @OnlyIn(Dist.CLIENT)
    abstract class SpriteButton extends BadBeaconScreen.Button {
        private final int iconX;
        private final int iconY;

        protected SpriteButton(int x, int y, int offsetX, int offsetY, Component component) {
            super(x, y, component);
            this.iconX = offsetX;
            this.iconY = offsetY;
        }

        @Override
        protected void blitButton(PoseStack stack) {
            this.blit(stack, this.getX() + 2, this.getY() + 2, this.iconX, this.iconY, 18, 18);
        }

		@Override
		public void renderToolTipFuck(PoseStack stack, int x, int y) {
			BadBeaconScreen.this.renderTooltip(stack, BadBeaconScreen.this.title, x, y);
		}
	}

    @OnlyIn(Dist.CLIENT)
    class ConfirmButton extends BadBeaconScreen.SpriteButton {
        public ConfirmButton(int x, int y) {
            super(x, y, 90, 220, CommonComponents.GUI_DONE);
        }

        @Override
        public void onPress() {
            PacketHandler.HANDLER.sendToServer(new ServerboundBadBeaconPacket(MobEffect.getId(BadBeaconScreen.this.primaryEffect), MobEffect.getId(BadBeaconScreen.this.secondaryEffect)));
            BadBeaconScreen.this.minecraft.player.closeContainer();
        }

		@Override
		public void updateStatus(int id) {
			this.active = BadBeaconScreen.this.menu.isActive();
		}
	}

    @OnlyIn(Dist.CLIENT)
    class CancelButton extends BadBeaconScreen.SpriteButton {
        public CancelButton(int x, int y) {
            super(x, y, 112, 220, CommonComponents.GUI_CANCEL);
        }

        @Override
        public void onPress() {
            BadBeaconScreen.this.minecraft.player.closeContainer();
        }

		@Override
		public void updateStatus(int id) { }
	}

	@OnlyIn(Dist.CLIENT)
	class UpgradeButton extends BadBeaconScreen.PowerButton {
		public UpgradeButton(int x, int y, MobEffect effect) {
			super(x, y, effect, false, 3);
		}

		protected MutableComponent createDescription(MobEffect effect) {
			return (Component.translatable(effect.getDescriptionId())).append(" II");
		}

		public void updateStatus(int id) {
			if (BadBeaconScreen.this.primaryEffect != null) {
				this.visible = true;
				this.setEffect(BadBeaconScreen.this.primaryEffect);
				super.updateStatus(id);
			} else {
				this.visible = false;
			}
		}
	}
}
