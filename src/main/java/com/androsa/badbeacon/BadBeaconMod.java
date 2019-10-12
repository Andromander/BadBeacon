package com.androsa.badbeacon;

import net.minecraft.block.Block;
import net.minecraft.client.gui.ScreenManager;
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
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ObjectHolder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(BadBeaconMod.MODID)
@Mod.EventBusSubscriber(modid = BadBeaconMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BadBeaconMod {
    public static final String MODID = "badbeacon";
    private static final Logger LOGGER = LogManager.getLogger();
    public static BadBeaconConfig config;

    @ObjectHolder(MODID + ":bad_beacon")
    public static final Block BAD_BEACON = new BadBeaconBlock();
    public static final Tag<Block> BAD_BEACON_BASE = new BlockTags.Wrapper(new ResourceLocation("badbeacon", "bad_beacon_base"));
    public static final Tag<Item> BAD_BEACON_PAYMENT = new ItemTags.Wrapper(new ResourceLocation("badbeacon", "bad_beacon_payment"));
    @ObjectHolder(MODID + ":bad_beacon_tileentity")
    public static final TileEntityType<BadBeaconTileEntity> BAD_BEACON_TILEENTITY = TileEntityType.Builder.create(BadBeaconTileEntity::new, BadBeaconMod.BAD_BEACON).build(null);
    @ObjectHolder(MODID + ":bad_beacon_container")
    public static final ContainerType<BadBeaconContainer> BAD_BEACON_CONTAINER = new ContainerType<>(BadBeaconContainer::new);

    public BadBeaconMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);

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

    @SubscribeEvent
    public static void registerBlock(RegistryEvent.Register<Block> e) {
        e.getRegistry().register(BAD_BEACON.setRegistryName("bad_beacon"));
        LOGGER.debug("Registered Block");
    }

    @SubscribeEvent
    public static void registerItem(RegistryEvent.Register<Item> e) {
        e.getRegistry().register(new BlockItem(BAD_BEACON, new Item.Properties().group(ItemGroup.MISC).rarity(Rarity.RARE)).setRegistryName(BAD_BEACON.getRegistryName()));
        LOGGER.debug("Registered Item");
    }

    @SubscribeEvent
    public static void registerTileEntity(RegistryEvent.Register<TileEntityType<?>> e) {
        e.getRegistry().register(BAD_BEACON_TILEENTITY.setRegistryName("bad_beacon_tileentity"));
        LOGGER.debug("Registered Tile Entity");
    }

    @SubscribeEvent
    public static void registerContainer(RegistryEvent.Register<ContainerType<?>> e) {
        e.getRegistry().register(BAD_BEACON_CONTAINER.setRegistryName("bad_beacon_container"));
        LOGGER.debug("Registered Container");
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerScreen() {
        ScreenManager.registerFactory(BAD_BEACON_CONTAINER, BadBeaconScreen::new);
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerBinds() {
        ClientRegistry.bindTileEntitySpecialRenderer(BadBeaconTileEntity.class, new BadBeaconTileEntityRenderer());
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
