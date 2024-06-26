package com.androsa.badbeacon;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

public class BadBeaconScreen extends AbstractContainerScreen<BadBeaconMenu> {
    private static final ResourceLocation BEACON_GUI_TEXTURES = new ResourceLocation(BadBeaconMod.MODID, "textures/gui/container/beacon.png");
    private static final Component PRIMARY_EFFECT_NAME = Component.translatable("block.minecraft.beacon.primary");
    private static final Component SECONDARY_EFFECT_NAME = Component.translatable("block.minecraft.beacon.secondary");
    private final List<BadBeaconButton> buttons = Lists.newArrayList();
    private Holder<MobEffect> primaryEffect;
    private Holder<MobEffect> secondaryEffect;

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
			int s1 = BadBeaconBlockEntity.EFFECTS_LIST.get(tier).size();
			int w1 = s1 * 22 + (s1 - 1) * 2;

			for(int i = 0; i < s1; ++i) {
                Holder<MobEffect> primary = BadBeaconBlockEntity.EFFECTS_LIST.get(tier).get(i);
				BadBeaconScreen.PowerButton powerbutton1 = new BadBeaconScreen.PowerButton(this.leftPos + 76 + i * 24 - w1 / 2, this.topPos + 22 + tier * 25, primary, true, tier);
				powerbutton1.active = false;
				this.addButton(powerbutton1);
			}
		}

		int t3 = BadBeaconBlockEntity.EFFECTS_LIST.get(3).size() + 1;
		int w2 = t3 * 22 + (t3 - 1) * 2;

		for(int tier2 = 0; tier2 < t3 - 1; ++tier2) {
            Holder<MobEffect> secondary = BadBeaconBlockEntity.EFFECTS_LIST.get(3).get(tier2);
			BadBeaconScreen.PowerButton powerbutton2 = new BadBeaconScreen.PowerButton(this.leftPos + 167 + tier2 * 24 - w2 / 2, this.topPos + 47, secondary, false, 3);
			powerbutton2.active = false;
			this.addButton(powerbutton2);
		}

		BadBeaconScreen.PowerButton upgradebutton = new BadBeaconScreen.UpgradeButton(this.leftPos + 167 + (t3 - 1) * 24 - w2 / 2, this.topPos + 47, BeaconBlockEntity.BEACON_EFFECTS.getFirst().getFirst());
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
    protected void renderLabels(GuiGraphics stack, int mouseX, int mouseY) {
        stack.drawCenteredString(this.font, PRIMARY_EFFECT_NAME, 62, 10, 14737632);
        stack.drawCenteredString(this.font, SECONDARY_EFFECT_NAME, 169, 10, 14737632);
    }

    @Override
    protected void renderBg(GuiGraphics stack, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        stack.blit(BEACON_GUI_TEXTURES, x, y, 0, 0, this.imageWidth, this.imageHeight);
        stack.pose().pushPose();
        stack.pose().translate(0.0F, 0.0F, 100.0F);
        stack.renderItem(new ItemStack(Items.COPPER_INGOT), x + 20, y + 109);
        stack.renderItem(new ItemStack(Items.COAL), x + 41, y + 109);
        stack.renderItem(new ItemStack(Items.LAPIS_LAZULI), x + 41 + 22, y + 109);
        stack.renderItem(new ItemStack(Items.REDSTONE), x + 42 + 44, y + 109);
        stack.renderItem(new ItemStack(Items.QUARTZ), x + 42 + 66, y + 109);
        stack.pose().popPose();
    }

    @Override
    public void render(GuiGraphics stack, int mouseX, int mouseZ, float ticks) {
        super.render(stack, mouseX, mouseZ, ticks);
        this.renderTooltip(stack, mouseX, mouseZ);
    }

	interface BadBeaconButton {
    	void updateStatus(int id);
	}

    abstract static class Button extends AbstractButton implements BadBeaconButton {
        private boolean selected;

        protected Button(int x, int y) {
            super(x, y, 22, 22, CommonComponents.EMPTY);
        }

        protected Button(int x, int y, Component component) {
        	super(x, y, 22, 22, component);
		}

        @Override
        public void renderWidget(GuiGraphics stack, int backX, int backY, float partial) {
            int j = 0;
            if (!this.active) {
                j += this.width * 2;
            } else if (this.selected) {
                j += this.width * 1;
            } else if (this.isHoveredOrFocused()) {
                j += this.width * 3;
            }

            stack.blit(BadBeaconScreen.BEACON_GUI_TEXTURES, this.getX(), this.getY(), j, 219, this.width, this.height);
            this.blitButton(stack);
        }

        protected abstract void blitButton(GuiGraphics stack);

        public boolean isSelected() {
            return this.selected;
        }

        public void setSelected(boolean selectedIn) {
            this.selected = selectedIn;
        }

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			this.defaultButtonNarrationText(output);
		}
	}

    class PowerButton extends BadBeaconScreen.Button {
		private final boolean isPrimary;
		protected final int tier;
        private Holder<MobEffect> effect;
        private TextureAtlasSprite textureSprite;

        public PowerButton(int x, int y, Holder<MobEffect> effectIn, boolean primary, int tier) {
            super(x, y);
            this.isPrimary = primary;
            this.tier = tier;
            this.setEffect(effectIn);
        }

        protected void setEffect(Holder<MobEffect> effect) {
        	this.effect = effect;
        	this.textureSprite = Minecraft.getInstance().getMobEffectTextures().get(effect);
        	this.setTooltip(Tooltip.create(this.createDescription(effect), null));
		}

		protected MutableComponent createDescription(Holder<MobEffect> effect) {
        	return Component.translatable(effect.value().getDescriptionId());
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
        protected void blitButton(GuiGraphics stack) {
            stack.blit(this.getX() + 2, this.getY() + 2, 0, 18, 18, this.textureSprite);
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

    abstract static class SpriteButton extends BadBeaconScreen.Button {
        private final int iconX;
        private final int iconY;

        protected SpriteButton(int x, int y, int offsetX, int offsetY, Component component) {
            super(x, y, component);
            this.iconX = offsetX;
            this.iconY = offsetY;
        }

        @Override
        protected void blitButton(GuiGraphics stack) {
            stack.blit(BadBeaconScreen.BEACON_GUI_TEXTURES, this.getX() + 2, this.getY() + 2, this.iconX, this.iconY, 18, 18);
        }
	}

    class ConfirmButton extends BadBeaconScreen.SpriteButton {
        public ConfirmButton(int x, int y) {
            super(x, y, 90, 220, CommonComponents.GUI_DONE);
        }

        @Override
        public void onPress() {
            PacketDistributor.sendToServer(new ServerboundBadBeaconPacket(Optional.ofNullable(BadBeaconScreen.this.primaryEffect), Optional.ofNullable(BadBeaconScreen.this.secondaryEffect)));
            BadBeaconScreen.this.minecraft.player.closeContainer();
        }

		@Override
		public void updateStatus(int id) {
			this.active = BadBeaconScreen.this.menu.isActive() && BadBeaconScreen.this.primaryEffect != null;
		}
	}

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

	class UpgradeButton extends BadBeaconScreen.PowerButton {
		public UpgradeButton(int x, int y, Holder<MobEffect> effect) {
			super(x, y, effect, false, 3);
		}

		protected MutableComponent createDescription(Holder<MobEffect> effect) {
			return (Component.translatable(effect.value().getDescriptionId())).append(" II");
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
