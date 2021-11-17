package net.fabricmc.wavy;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.MessageType;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class WaveFunctionItem2 extends Item {
    
    public WaveFunctionItem2(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx){
        if(ctx.getWorld().isClient()) {
            String str = ctx.getBlockPos().toString();
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.inGameHud.addChatMessage(MessageType.SYSTEM, new LiteralText("Set position 2 to: " + str), ctx.getPlayer().getUuid());
            WFC.waveDriver.Pos2(ctx.getBlockPos());
        }
        return ActionResult.PASS;
    }

    @Override
    public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
		return false;
	}

}
