package com.androsa.badbeacon;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.ParallelDispatchEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(BadBeaconMod.MODID)
public class BadBeaconMod {
    public static final String MODID = "badbeacon";
    private static final Logger LOGGER = LogManager.getLogger();
    public static BadBeaconConfig config;

    public static final Tag.Named<Block> BAD_BEACON_BASE = BlockTags.bind("badbeacon:bad_beacon_base");
    public static final Tag.Named<Item> BAD_BEACON_PAYMENT = ItemTags.bind("badbeacon:bad_beacon_payment");

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, MODID);
    public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, MODID);

    public static final RegistryObject<Block> BAD_BEACON = BLOCKS.register("bad_beacon", BadBeaconBlock::new);
    public static final RegistryObject<Item> BAD_BEACON_ITEM = ITEMS.register("bad_beacon", () -> new BlockItem(BAD_BEACON.get(), new Item.Properties().tab(CreativeModeTab.TAB_MISC).rarity(Rarity.RARE)));
    public static final RegistryObject<BlockEntityType<BadBeaconBlockEntity>> BAD_BEACON_TILEENTITY = BLOCK_ENTITIES.register("bad_beacon_tileentity", () -> BlockEntityType.Builder.of(BadBeaconBlockEntity::new, BadBeaconMod.BAD_BEACON.get()).build(null));
    public static final RegistryObject<MenuType<BadBeaconMenu>> BAD_BEACON_CONTAINER = CONTAINERS.register("bad_beacon_container", () -> new MenuType<>(BadBeaconMenu::new));

    public BadBeaconMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::dispatch);
        modBus.addListener(this::clientSetup);

        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        CONTAINERS.register(modBus);

        final Pair<BadBeaconConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(BadBeaconConfig::new);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, specPair.getRight());
        config = specPair.getLeft();
    }

    public void dispatch(ParallelDispatchEvent e) {
    	e.enqueueWork(PacketHandler::register);
	}

    public void clientSetup(final FMLClientSetupEvent e) {
        BadBeaconMod.registerScreen();
        BadBeaconMod.registerBinds();
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerScreen() {
        MenuScreens.register(BAD_BEACON_CONTAINER.get(), BadBeaconScreen::new);
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerBinds() {
        ItemBlockRenderTypes.setRenderLayer(BAD_BEACON.get(), RenderType.cutout());
        BlockEntityRenderers.register(BAD_BEACON_TILEENTITY.get(), BadBeaconRenderer::new);
    }

    public static class BadBeaconConfig {
        public static ForgeConfigSpec.BooleanValue affectAllEntities;

        public BadBeaconConfig(ForgeConfigSpec.Builder builder) {
            affectAllEntities = builder
                    .translation(BadBeaconMod.MODID + ".config.affect_all_entities")
                    .comment("Determines whether Bad Beacons can affect all living entities. If Enabled, anything that is a 'living entity' can be targeted.")
                    .define("affectAllEntities", false);
        }
    }
}
