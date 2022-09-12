package net.fabricmc.wavy;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.network.MessageType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.io.IOException;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WFC implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static WaveDriver waveDriver = new WaveDriver();
	public static final Logger LOGGER = LogManager.getLogger("WFC");
	public static final WaveFunctionItem FABRIC_ITEM = new WaveFunctionItem(new FabricItemSettings().group(ItemGroup.MISC));
	public static final WaveFunctionItem2 FABRIC_ITEM2 = new WaveFunctionItem2(new FabricItemSettings().group(ItemGroup.MISC));
	public static final WaveRunnerItem RUN_ITEM = new WaveRunnerItem(new FabricItemSettings().group(ItemGroup.MISC));
	public static final WaveRunnerItem2 RUN_ITEM2 = new WaveRunnerItem2(new FabricItemSettings().group(ItemGroup.MISC));

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		Registry.register(Registry.ITEM, new Identifier("wfc", "fabric_item"), FABRIC_ITEM);
		Registry.register(Registry.ITEM, new Identifier("wfc", "fabric_item2"), FABRIC_ITEM2);
		Registry.register(Registry.ITEM, new Identifier("wfc", "run_item"), RUN_ITEM);
		Registry.register(Registry.ITEM, new Identifier("wfc", "run_item2"), RUN_ITEM2);

		//Command to load in the adjacencies for WFC
		CommandRegistrationCallback.EVENT.register((dispatcher, decdicated) -> {
			dispatcher.register(CommandManager.literal("saveWFC").executes(context-> {
				MinecraftClient mc = MinecraftClient.getInstance();

				//Setup the initial requirements for the Wave Driving function
				waveDriver.Constraint(125);
				waveDriver.Mc(mc);
				//int ret = waveDriver.firstStepWFC();
				int ret = waveDriver.firstStepWFCChunks();
				mc.inGameHud.addChatMessage(MessageType.SYSTEM, new LiteralText("First WFC finished with value " + ret), mc.player.getUuid());
				return 1;
			}));
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, decdicated) -> {
			dispatcher.register(CommandManager.literal("saveToFile").executes(context -> {
				MinecraftClient mc = MinecraftClient.getInstance();
				try {
					waveDriver.SaveFile();
					mc.inGameHud.addChatMessage(MessageType.SYSTEM, new LiteralText("Successfully saved WFC Matrix to file."), mc.player.getUuid());
				} catch (IOException e) {
					mc.inGameHud.addChatMessage(MessageType.SYSTEM, new LiteralText("Failed to save WFC Matrix to file."), mc.player.getUuid());
					e.printStackTrace();
				}
				
				return 1;
			}));
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, decdicated) -> {
			dispatcher.register(CommandManager.literal("loadFromFile").executes(context -> {
				MinecraftClient mc = MinecraftClient.getInstance();
				try {
					waveDriver.LoadFile();
					mc.inGameHud.addChatMessage(MessageType.SYSTEM, new LiteralText("Successfully loaded WFC Matrix from file."), mc.player.getUuid());
				} catch (ClassNotFoundException | IOException e) {
					mc.inGameHud.addChatMessage(MessageType.SYSTEM, new LiteralText("Failed to load WFC Matrix from file."), mc.player.getUuid());
					e.printStackTrace();
				}

				return 1;
			}));
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, decdicated) -> {
			dispatcher.register(CommandManager.literal("runWFC").executes(context-> {
				MinecraftClient mc = MinecraftClient.getInstance();
				waveDriver.Mc(mc);

				int ret = waveDriver.secondStepWrapper(1);
				mc.inGameHud.addChatMessage(MessageType.SYSTEM, new LiteralText("Second WFC finished with value " + ret), mc.player.getUuid());
				return 1;
			}));
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, decdicated) -> {
			dispatcher.register(CommandManager.literal("constrain").executes(context-> {
				MinecraftClient mc = MinecraftClient.getInstance();
				mc.inGameHud.addChatMessage(MessageType.SYSTEM, new LiteralText("This command doesn't work yet"), mc.player.getUuid());
				return 1;
			}));
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			dispatcher.register(CommandManager.literal("runArgs")
				.then(CommandManager.argument("runs", IntegerArgumentType.integer())
					.executes(context -> {

						MinecraftClient mc = MinecraftClient.getInstance();
						waveDriver.Mc(mc);
						int runs = IntegerArgumentType.getInteger(context, "runs");
						int ret = waveDriver.secondStepWrapper(runs);
						mc.inGameHud.addChatMessage(MessageType.SYSTEM, new LiteralText("Second WFC finished with value " + ret), mc.player.getUuid());

						return 1;
					})));
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			dispatcher.register(CommandManager.literal("collapsetest").executes(context -> {
				waveDriver.testCollapseNode();
				return 1;
			}));
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			dispatcher.register(CommandManager.literal("chunkSize")
			.then(CommandManager.argument("num", IntegerArgumentType.integer())
				.executes(context -> {
					int num = IntegerArgumentType.getInteger(context, "num");
					waveDriver.chunkSize = num;
					return 1;
				})));
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated)-> {
			dispatcher.register(CommandManager.literal("debugChunks").executes(context -> {
				MinecraftClient mc = MinecraftClient.getInstance();
				waveDriver.debugChunks(mc.player.getPos());
				return 1;
			}));
		});

		LOGGER.info("Hello Fabric world!");


		//Initialize Config File using https://github.com/magistermaks/fabric-simplelibs/tree/master/simple-config Simple Configs
		//ConfigRequest CONFIG = SimpleConfig.of( "WFCconfig" ).provider( this::provider ).request();
	}

}
