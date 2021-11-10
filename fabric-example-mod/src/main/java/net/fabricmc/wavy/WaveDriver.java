package net.fabricmc.wavy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.MessageType;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;

public class WaveDriver {
    public static WaveDriver instance;

    private MinecraftClient mc;
    public void Mc(MinecraftClient m){ mc = m; }

    private BlockPos pos1;
    public void Pos1(BlockPos p){ pos1 = p; }
    public BlockPos Pos1(){ return pos1; }

    private BlockPos pos2;
    public void Pos2(BlockPos p){ pos2 = p; }
    public BlockPos Pos2(){ return pos2; }

    private int constraint = 25;
    public void Constraint(int c){ constraint = c; }
    public int Constraint(){ return constraint; }

    public WaveDriver(){
        instance = this;
    }

    @Override
    public String toString(){
        return ("Position 1: " + pos1 + "\nPosition 2 " + pos2);
    }

    //Print's string to the chat log
    private void print(String str){
        if(mc != null){
            mc.inGameHud.addChatMessage(MessageType.SYSTEM, new LiteralText(str), mc.player.getUuid());
        } else {
            System.out.println("Somehow Minecraft Client was null");
        }
    }

    public int firstStepWFC(){

        //Check to see if either coordinate is null (not set yet)
        if(pos1 == null || pos2 == null){
            print("Failed to validate Position 1 or Position 2" + "\n Position 1 is " + pos1 + "\n Position 2 is " + pos2);
            return -1;
        }   

        //Test to see if the selection is too large
        int lenX = Math.abs( pos1.getX() - pos2.getX() );
        int lenY = Math.abs( pos1.getY() - pos2.getY() );
        int lenZ = Math.abs( pos1.getZ() - pos2.getZ() );
        int area = lenX * lenY * lenZ;
        if( area >= constraint){
            print("Your selected area appears to be too large with size " + area + ". The current constraint is set at " + constraint + ". If you would like to change this, please use the /constrain command (Non-functional), but procede with caution");
            return -2;
        }

        //Read in the input data
        int j = buildAdjacencies();
        if(j < 0){
            return j;
        }

        return 0;
    }

    private int buildAdjacencies(){

        return 0;
    }

    


}
