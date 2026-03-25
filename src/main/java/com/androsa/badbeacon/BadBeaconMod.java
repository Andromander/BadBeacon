package com.androsa.badbeacon;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

@Mod(BadBeaconMod.MODID)
public class BadBeaconMod {
    public static final String MODID = "badbeacon";
    private static final Logger LOGGER = LogManager.getLogger();
    public static BadBeaconConfig config;

    public static final TagKey<Block> BAD_BEACON_BASE = BlockTags.create(Identifier.fromNamespaceAndPath(MODID, "bad_beacon_base"));
    public static final TagKey<Item> BAD_BEACON_PAYMENT = ItemTags.create(Identifier.fromNamespaceAndPath(MODID, "bad_beacon_payment"));

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(Registries.MENU, MODID);

    public static final DeferredBlock<Block> BAD_BEACON = BLOCKS.registerBlock("bad_beacon", BadBeaconBlock::new);
    public static final DeferredItem<Item> BAD_BEACON_ITEM = ITEMS.registerItem("bad_beacon", (props) -> new BlockItem(BAD_BEACON.get(), props.rarity(Rarity.RARE).useBlockDescriptionPrefix()));
    public static final Supplier<BlockEntityType<BadBeaconBlockEntity>> BAD_BEACON_TILEENTITY = BLOCK_ENTITIES.register("bad_beacon_tileentity", () -> new BlockEntityType<>(BadBeaconBlockEntity::new, BadBeaconMod.BAD_BEACON.get()));
    public static final Supplier<MenuType<BadBeaconMenu>> BAD_BEACON_CONTAINER = CONTAINERS.register("bad_beacon_container", () -> new MenuType<>(BadBeaconMenu::new, FeatureFlags.VANILLA_SET));

    public BadBeaconMod(IEventBus bus, ModContainer container) {
        bus.addListener(this::clientSetup);
        bus.addListener(this::registerPayload);
        bus.addListener(this::buildContents);
        bus.addListener(this::registerScreen);

        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        CONTAINERS.register(bus);

        final Pair<BadBeaconConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(BadBeaconConfig::new);
        container.registerConfig(ModConfig.Type.COMMON, specPair.getRight());
        config = specPair.getLeft();
    }

    public void clientSetup(final FMLClientSetupEvent e) {
        BadBeaconClient.registerBinds();
    }

    public void registerPayload(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("badbeacon").versioned("1").optional();
        registrar.playToServer(ServerboundBadBeaconPacket.ID, ServerboundBadBeaconPacket.CODEC, ServerboundBadBeaconPacket::handle);
    }

    public void buildContents(final BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            e.accept(BAD_BEACON_ITEM.get());
        }
    }

    public void registerScreen(RegisterMenuScreensEvent event) {
        event.register(BAD_BEACON_CONTAINER.get(), BadBeaconScreen::new);
    }

    public static class BadBeaconConfig {
        public static ModConfigSpec.BooleanValue affectAllEntities;

        public BadBeaconConfig(ModConfigSpec.Builder builder) {
            affectAllEntities = builder
                    .translation(BadBeaconMod.MODID + ".config.affect_all_entities")
                    .comment("Determines whether Bad Beacons can affect all living entities. If Enabled, anything that is a 'living entity' can be targeted.")
                    .define("affectAllEntities", false);
        }
    }
}
