package com.androsa.badbeacon;

import net.minecraft.block.Block;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Rarity;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(BadBeaconMod.MODID)
public class BadBeaconMod {
    public static final String MODID = "badbeacon";
    private static final Logger LOGGER = LogManager.getLogger();
    public static BadBeaconConfig config;

    public static final Tag<Block> BAD_BEACON_BASE = new BlockTags.Wrapper(new ResourceLocation("badbeacon", "bad_beacon_base"));
    public static final Tag<Item> BAD_BEACON_PAYMENT = new ItemTags.Wrapper(new ResourceLocation("badbeacon", "bad_beacon_payment"));

    public static final DeferredRegister<Block> BLOCKS = new DeferredRegister<>(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = new DeferredRegister<>(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = new DeferredRegister<>(ForgeRegistries.TILE_ENTITIES, MODID);
    public static final DeferredRegister<ContainerType<?>> CONTAINERS = new DeferredRegister<>(ForgeRegistries.CONTAINERS, MODID);

    public static final RegistryObject<Block> BAD_BEACON = BLOCKS.register("bad_beacon", BadBeaconBlock::new);
    public static final RegistryObject<Item> BAD_BEACON_ITEM = ITEMS.register("bad_beacon", () -> new BlockItem(BAD_BEACON.get(), new Item.Properties().group(ItemGroup.MISC).rarity(Rarity.RARE)));
    public static final RegistryObject<TileEntityType<BadBeaconTileEntity>> BAD_BEACON_TILEENTITY = TILE_ENTITIES.register("bad_beacon_tileentity", () -> TileEntityType.Builder.create(BadBeaconTileEntity::new, BadBeaconMod.BAD_BEACON.get()).build(null));
    public static final RegistryObject<ContainerType<BadBeaconContainer>> BAD_BEACON_CONTAINER = CONTAINERS.register("bad_beacon_container", () -> new ContainerType<>(BadBeaconContainer::new));

    public BadBeaconMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::setup);
        modBus.addListener(this::clientSetup);

        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TILE_ENTITIES.register(modBus);
        CONTAINERS.register(modBus);

        final Pair<BadBeaconConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(BadBeaconConfig::new);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, specPair.getRight());
        config = specPair.getLeft();
    }

    public void setup(FMLCommonSetupEvent e) {
        DistExecutor.runWhenOn(Dist.CLIENT, () -> BadBeaconMod::registerScreen);
        DeferredWorkQueue.runLater(PacketHandler::register);
    }

    public void clientSetup(final FMLClientSetupEvent e) {
        DistExecutor.runWhenOn(Dist.CLIENT, () -> BadBeaconMod::registerBinds);
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerScreen() {
        ScreenManager.registerFactory(BAD_BEACON_CONTAINER.get(), BadBeaconScreen::new);
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerBinds() {
        RenderTypeLookup.setRenderLayer(BAD_BEACON.get(), RenderType.func_228643_e_());
        ClientRegistry.bindTileEntityRenderer(BAD_BEACON_TILEENTITY.get(), BadBeaconTileEntityRenderer::new);
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
