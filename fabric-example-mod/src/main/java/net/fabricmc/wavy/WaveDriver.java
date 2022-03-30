package net.fabricmc.wavy;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.system.CallbackI.B;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.StairShape;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.network.MessageType;
import net.minecraft.state.property.Properties;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;


public class WaveDriver {
    public static WaveDriver instance;

    private World world;
    public void World(World w) {world = w; }
    public World World() {return world;}

    private MinecraftClient mc;
    public void Mc(MinecraftClient m){ mc = m; }

    private BlockPos pos1; //Load position 1
    public void Pos1(BlockPos p){ pos1 = p; }
    public BlockPos Pos1(){ return pos1; }

    private BlockPos pos2; //Load position 2
    public void Pos2(BlockPos p){ pos2 = p; }
    public BlockPos Pos2(){ return pos2; }

    private BlockPos runPos1; //Run position 1
    public void Run1(BlockPos p) { runPos1 = p; }
    public BlockPos Run1() {return runPos1; }

    private BlockPos runPos2; //Run position 2
    public void Run2(BlockPos p) { runPos2 = p; }
    public BlockPos Run2() {return runPos2; }

    private int constraint = 25;
    public void Constraint(int c){ constraint = c; }
    public int Constraint(){ return constraint; }

    //represents the degrees that we should wrap over (0 for false, 1 for true)
    private Vector3d WRAP = new Vector3d(1, 0, 1);
    boolean SHOULDWRAP = false;

    //Used to convert from an intger to a block. Used when initially reading through the input
    HashMap<Integer, BlockState> integerToBlockMap = new HashMap<Integer, BlockState>( ); //Needed in Save
    HashMap<BlockState, Integer> blockToIntegerMap = new HashMap<BlockState, Integer>( );

    //Lists all the blocks (As integers) that we have seen, and their count
    HashMap<Integer, Integer> listOfSeenBlocks = new HashMap<Integer, Integer> ( ); //Needed in Save
    int currentIndex = 0;

    private BlockPos originalRunPos1;
    private BlockPos originalRunPos2;

    //Map of Integer ID to A list of length 6 with each element being the adjacencies found at that direction
    /*
    0 - Up
    1 - Down
    2 - Left
    3 - Right
    4 - Forward
    5 - Back
    */
    HashMap<Integer, Vector<Vector<Integer>> > adj = new HashMap<Integer, Vector<Vector<Integer>>>(); //Needed in Save
    HashMap<BlockPos, Vector<Integer>> collapseMap = new HashMap<BlockPos, Vector<Integer>>(); //List of all block positions in the map and their possibilites
    Set<BlockPos> collapsed = new HashSet<BlockPos>(); //Set of all collapsed nodse
    
    //Saving Information
    String filename = "WaveMatrix";

    public boolean SaveFile() throws IOException{
        Path path = FabricLoader.getInstance().getConfigDir();
        File save = path.resolve( filename + ".properties" ).toFile();

        // try creating missing files
        save.getParentFile().mkdirs();
        try {
            Files.createFile( save.toPath() );
        } catch (IOException e) {
            System.out.println("Did not create new file");
        }
        //TODO figure out how to add edge adjacencies
        HashMap<Integer, String> saveIntegerToBlockMap = new HashMap<Integer, String>();

        for (Integer i : integerToBlockMap.keySet()) {
            saveIntegerToBlockMap.put(i, integerToBlockMap.get(i).toString());
        }

        FileOutputStream f = new FileOutputStream(save);
        ObjectOutputStream o = new ObjectOutputStream(f);

        o.writeObject(saveIntegerToBlockMap);
        o.writeObject(adj);
        o.writeObject(listOfSeenBlocks);

        o.close();

        return true;
    }

    public boolean LoadFile() throws IOException, ClassNotFoundException{
        Path path = FabricLoader.getInstance().getConfigDir();
        File save = path.resolve ( filename + ".properties").toFile();

        FileInputStream f = new FileInputStream(save);
        ObjectInputStream o = new ObjectInputStream(f);

        HashMap<Integer, String> test = (HashMap<Integer, String>) o.readObject();

        adj = (HashMap<Integer, Vector<Vector<Integer>> >) o.readObject();
        listOfSeenBlocks = (HashMap<Integer, Integer>) o.readObject();
        //TODO figure out how to load the edge adjacencies
        //Convert test to integerToBlockMap and blockToIntegerMap;
        for(int i : test.keySet()){
            convertStringToBlockState(test.get(i), i);
        }
        
        o.close();

        System.out.println("Printing Data from Load File: ");
        System.out.println("Integer To Block Map: " + integerToBlockMap);
        System.out.println("List of Seen Blocks: " + listOfSeenBlocks);
        System.out.println("Printing out Adjacency Matrix: " + adj);
        hasRunFirstStep = true;
        return true;
    }

    public void convertStringToBlockState(String s, int i){

        BlockState tester;
        String block = "COBBLESTONE";
        HashMap<String, String> propertyMap = new HashMap<String, String> ();

        if(s.contains("[")){ //Need to do all the property handling
            String preProperties = s.split("}")[1];
            preProperties = preProperties.replace("[", "");
            preProperties = preProperties.replace("]", "");

            for(String p : preProperties.contains(",") ? preProperties.split(",") : new String[]{preProperties}){
                propertyMap.put(p.split("=")[0], p.split("=")[1]);
            }

            block = s.split(":")[1];
            block = block.split("}")[0];
            block = block.toUpperCase();   
            
        } else { //we can just do the block
            block = StringUtils.chop(s.split(":")[1].toUpperCase());
        }

        //System.out.println(block + ":" + propertyMap); 

        //world.setBlockState(new BlockPos(0,80,0), Blocks.DISPENSER.getDefaultState().with(Properties.FACING, Direction.DOWN), 3);
        try {

            Field t = Blocks.class.getField(block);
            Block r = (Block) t.get(null);
            tester = r.getDefaultState();

            //Property Testing

            //There are two types of facings for some reason
            if(propertyMap.containsKey("facing")){
                switch(propertyMap.get("facing")){
                    case "down":
                        tester.with(Properties.FACING, Direction.DOWN);
                        break;
                    case "north":
                        tester.with(Properties.FACING, Direction.NORTH);
                        tester.with(Properties.HORIZONTAL_FACING, Direction.NORTH);
                        break;
                    case "south":
                        tester.with(Properties.FACING, Direction.SOUTH);
                        tester.with(Properties.HORIZONTAL_FACING, Direction.SOUTH);
                        break;
                    case "west":
                        tester.with(Properties.FACING, Direction.WEST);
                        tester.with(Properties.HORIZONTAL_FACING, Direction.WEST);
                        break;
                    case "east":
                        tester.with(Properties.FACING, Direction.EAST);
                        tester.with(Properties.HORIZONTAL_FACING, Direction.EAST);
                        break;
                    case "up":
                        tester.with(Properties.FACING, Direction.UP);
                        break;
                }
            }

            if(propertyMap.containsKey("waterlogged")){
                switch(propertyMap.get("waterlogged")){
                    case "true":
                        tester.with(Properties.WATERLOGGED, true);
                        break;
                    case "false":
                        tester.with(Properties.WATERLOGGED, false);
                        break;
                }
            }

            if(propertyMap.containsKey("shape") && block.contains("STAIR")){ //Stair Shape
                switch(propertyMap.get("shape")){
                    case "straight":
                        tester.with(Properties.STAIR_SHAPE, StairShape.STRAIGHT);
                        break;
                    case "inner_left":
                        tester.with(Properties.STAIR_SHAPE, StairShape.INNER_LEFT);
                        break;
                    case "outer_left":
                        tester.with(Properties.STAIR_SHAPE, StairShape.OUTER_LEFT);
                        break;
                    case "inner_right":
                        tester.with(Properties.STAIR_SHAPE, StairShape.INNER_RIGHT);
                        break;
                    case "outer_right":
                        tester.with(Properties.STAIR_SHAPE, StairShape.OUTER_RIGHT);
                        break;
                }
            }

            //if(propertyMap.containsKey("half")){

            if(propertyMap.containsKey("axis")){
                switch(propertyMap.get("axis")){
                    case "x":
                        tester.with(Properties.AXIS, Direction.Axis.X);
                        break;
                    case "y":
                        tester.with(Properties.AXIS, Direction.Axis.Y);
                        break;
                    case "z":
                        tester.with(Properties.AXIS, Direction.Axis.Z);
                        break;
                }
            }

            integerToBlockMap.put(i, tester);
            blockToIntegerMap.put(tester, i);
            
        } catch (NoSuchFieldException | SecurityException e) {
            // Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // Auto-generated catch block
            e.printStackTrace();
        }
    }

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

    boolean hasRunFirstStep = false;
    public int firstStepWFC(){

        //Check to see if either coordinate is null (not set yet)
        if(pos1 == null || pos2 == null){
            print("Failed to validate Position 1 or Position 2" + "\n Position 1 is " + pos1 + "\n Position 2 is " + pos2);
            return -1;
        }
        
        if(world == null){
            print("Failed to get world correctly");
            return -3;
        }

        //Test to see if the selection is too large
        //int lenX = Math.abs( pos1.getX() - pos2.getX() );
        //int lenY = Math.abs( pos1.getY() - pos2.getY() );
        //int lenZ = Math.abs( pos1.getZ() - pos2.getZ() );
        //int area = lenX * lenY * lenZ;
        // if( area >= constraint){
        //     print("Your selected area appears to be too large with size " + area + ". The current constraint is set at " + constraint + ". If you would like to change this, please use the /constrain command (Non-functional), but procede with caution");
        //     return -2;
        // }

        //Reset all the nonsense
        integerToBlockMap = new HashMap<Integer, BlockState>( );
        blockToIntegerMap = new HashMap<BlockState, Integer>( );
        listOfSeenBlocks = new HashMap<Integer, Integer> ( );
        adj = new HashMap<Integer, Vector<Vector<Integer>>>();
        currentIndex = 0;

        //Read in the input data
        int j = buildAdjacencies();

        System.out.println(listOfSeenBlocks);

        if(j < 0){
            return j;
        }

        //System.out.println(blockToIntegerMap);
        System.out.println(adj);

        //System.out.println("Block to integer map \n" + blockToIntegerMap);
        //System.out.println("List of seen blocks \n" + listOfSeenBlocks);

        hasRunFirstStep = true;
        return 1;
    }

    public int secondStepWrapper(){
        int ret = 0;
        int runs = 30;
        for(int i = 0; i < runs; i++){
            ret = secondStepWFC();
        }

        if(ret == 1){
            GenerateWorld();
        } else {
            print("Failed to find valid configuration in " + runs + " runs.");
        }

        return ret;
    }

    public int secondStepWFC(){
        collapseMap = new HashMap<BlockPos, Vector<Integer>>();
        collapsed = new HashSet<BlockPos>();

        if(!hasRunFirstStep){
            print("It appears that you have not loaded up adjacencies");
            return -1;
        }

        if(runPos1 == null || runPos2 == null){
            print("Failed to validate Run Position 1 or Run Position 2" + "\n Run Position 1 is " + runPos1 + "\n Run Position 2 is " + runPos2);
            return -2;
        }
        
        if(world == null){
            print("Failed to get world correctly");
            return -3;
        }

        Vector<Integer> t = new Vector<Integer>(listOfSeenBlocks.keySet()); //Makes a list of all the integers of seen blocks
        System.out.println(t);
        //Setup The Beginning Collapse Map

        //runPos1 = new BlockPos(0, 0, 0);
        //runPos2 = new BlockPos(0, 0, 0);

        //Verify integrity of Add edges
        System.out.println("Before Change \n Run Pos 1: " + runPos1 + "\nRun Pos 2: " + runPos2);
        AddEdges();
        System.out.println("After Change \n Run Pos 1: " + runPos1 + "\n Run Pos 2: " + runPos2);
        System.out.println("\n Should match original runPositions \n Original Run Pos1: " + originalRunPos1 + "\n Original Run Pos2: " + originalRunPos2);

       //Verify Integrity of Directions
        int xDist = Math.abs(runPos1.getX() - runPos2.getX());
        int yDist = Math.abs(runPos1.getY() - runPos2.getY());
        int zDist = Math.abs(runPos1.getZ() - runPos2.getZ());

        System.out.println("\nxDist " + xDist + "\nyDist " + yDist + " \nzDist " + zDist);

        //Verify Integrity of Collapse Map Adding
        for(int x = 0; x <= xDist; x++){
            for(int y = 0; y <= yDist; y++){
                for(int z = 0; z <= zDist; z++){
                    //System.out.println((x + runPos2.getX()) + ", " + (y + runPos2.getY()) + ", " + (z + runPos2.getZ()));
                    collapseMap.put(new BlockPos((x + runPos2.getX()), (y + runPos2.getY()), (z + runPos2.getZ())), t); //Adds all the default adjacencies to the nodes
                }
            }
        }

        System.out.println("Size of CollapseMap before Manipulate Edges: " + collapseMap.size());
        System.out.println("Size of Collapsed Nodes before Manipulate Edges: " + collapsed.size());
        System.out.println("\nRun Pos1: " + runPos1 + "\nRun Pos2: " + runPos2);
        
        ManipulateEdges();

        System.out.println("Size of CollapseMap after Manipulate Edges: " + collapseMap.size());
        System.out.println("Size of Collapsed Nodes after Manipulate Edges: " + collapsed.size());
        System.out.println("\nRun Pos1: " + runPos1 + "\nRun Pos2: " + runPos2);

        System.out.println(collapsed.contains(originalRunPos1));

        //Setup backup copies of the two necessary, mutable changes
        //HashMap<BlockPos, Vector<Integer>> backupCollapseMap = (HashMap<BlockPos, Vector<Integer>>) collapseMap.clone();
        //HashSet<BlockPos> backupCollapsed = new HashSet<BlockPos>(collapsed);
              
        //CollapseCorners();

        //System.out.println("Made it past Collapsing Corners");

        //Attempt an X number of iterations to try and find a valid configuration
        //int itrLimit = 30;
        //for(int i = 0; i < itrLimit; i++){
            //System.out.println("Collapse Map Equality:" + collapseMap.equals(backupCollapseMap));
            //collapseMap = (HashMap<BlockPos, Vector<Integer>>) backupCollapseMap.clone();

            //System.out.println("Collapsed Equality: " + collapsed.equals(backupCollapsed));
            //collapsed = new HashSet<BlockPos>(backupCollapsed);
        
        int itr = 10000;
        boolean done = false;
        boolean validConfig = true;
        //boolean validConfig = true;
        
        while(!done && itr > 0){
            itr--;
            //System.out.println(collapsed.size());
            if(collapsed.size() == collapseMap.keySet().size()){
                done = true;
                break;
            }

            //Find lowest entropy to collapse
            BlockPos current = findLeastEntropy();

            if(current == null){
                validConfig = false;
                break;
            }

            //Collapse it
            if(!collapseNode(current)){
                validConfig = false;
                break;
            }

            //Change the surrounding nodes
            changeSurrounding(current, SHOULDWRAP);

            //GenerateSingleBlock(current);
        }
            //if(validConfig){
            //    break;
            //}
        //}

        System.out.println("Made it past the waavy step");

        RemoveEdgeBlocks();

        System.out.println(collapseMap);

        runPos1 = originalRunPos1;
        runPos2 = originalRunPos2;

        if(validConfig){
            return 1;
        } else {
            return -4;
        }
    }

    private void GenerateSingleBlock(BlockPos current){
        //System.out.println("Generating single block at position " + current);
        world.setBlockState(current, integerToBlockMap.get(collapseMap.get(current).get(0)), 1);
    }

    private void RemoveEdgeBlocks(){

        Set<BlockPos> temp = collapseMap.keySet();
        Vector<BlockPos> stuff = new Vector<BlockPos>();

        for(BlockPos b : temp ){
            if(collapseMap.get(b).contains(-1)){
                stuff.add(b);
            }
        }
        //System.out.println("Made it past adding bad blocks");

        for(BlockPos b : stuff){
            collapseMap.remove(b);
            collapsed.remove(b);
        }
    }

    //Method to add an outer ring to the collapse map by updating the runPositions
    private void AddEdges(){
        int maxX = 0;
        int minX = 0;
        
        int maxY = 0;
        int minY = 0;

        int maxZ = 0;
        int minZ = 0;
        
        //This is repeated code in a number of places, should probably make a glogalvariable
        maxX = (runPos1.getX() > runPos2.getX() ? runPos1.getX() : runPos2.getX()) + 1;
        maxY = (runPos1.getY() > runPos2.getY() ? runPos1.getY() : runPos2.getY()) + 1;
        maxZ = (runPos1.getZ() > runPos2.getZ() ? runPos1.getZ() : runPos2.getZ()) + 1;

        minX = (runPos1.getX() < runPos2.getX() ? runPos1.getX() : runPos2.getX()) - 1;
        minY = (runPos1.getY() < runPos2.getY() ? runPos1.getY() : runPos2.getY()) - 1;
        minZ = (runPos1.getZ() < runPos2.getZ() ? runPos1.getZ() : runPos2.getZ()) - 1;

        originalRunPos1 = runPos1;
        originalRunPos2 = runPos2;

        runPos1 = new BlockPos(maxX, maxY, maxZ);
        runPos2 = new BlockPos(minX, minY, minZ);

    }
    
    //Add's the -1 adjacency to all the edges and updates the nodes on the inside
    private void ManipulateEdges(){

        Vector<BlockPos> edges = new Vector<BlockPos>();

        for(BlockPos bp : collapseMap.keySet()){
            //System.out.println("Block position: " + bp);
            int x = bp.getX();
            int y = bp.getY();
            int z = bp.getZ();
            
            if(!collapsed.contains(bp)){ //If we have not seen this before
                //System.out.println("Inside of the if statement");
                //If we are on one of the xEdges
                if(x == runPos1.getX() ||  x == runPos2.getX()) {
                    //System.out.println("Inside of the second if statement");
                    collapseMap.put(bp, new Vector<Integer>(){{add(-1);}});
                    collapsed.add(bp);
                    edges.add(bp);
                }

                //if we are on one of the yEdges
                if(y == runPos1.getY() || y == runPos2.getY()) {
                    collapseMap.put(bp, new Vector<Integer>(){{add(-1);}});
                    collapsed.add(bp);
                    edges.add(bp);
                }

                //if we are on one of the zEdges 
                if(z == runPos1.getZ() || z == runPos2.getZ()){
                    collapseMap.put(bp, new Vector<Integer>(){{add(-1);}});
                    collapsed.add(bp);
                    edges.add(bp);
                }
            }
        }

        //At this point, we should have collapsed all of the edges, so we should then close the adjacencies, and the only thing in collapsed IS the edges
        for(BlockPos bp : edges){
            changeSurrounding(bp, SHOULDWRAP);
        }
    }

    private void CollapseCorners(){
        //Collapse all 8 corners
        int xDif = originalRunPos1.getX() - originalRunPos2.getX();
        int yDif = originalRunPos1.getY() - originalRunPos2.getY();
        int zDif = originalRunPos1.getZ() - originalRunPos2.getZ();

        Vector<BlockPos> r = new Vector<BlockPos>();

        r.add(new BlockPos(originalRunPos1.getX() - xDif, originalRunPos1.getY(), originalRunPos1.getZ()));
        r.add(new BlockPos(originalRunPos1.getX(), originalRunPos1.getY() - yDif, originalRunPos1.getZ()));
        r.add(new BlockPos(originalRunPos1.getX(), originalRunPos1.getY(), originalRunPos1.getZ() - zDif));

        r.add(new BlockPos(originalRunPos2.getX() + xDif, runPos2.getY(), originalRunPos2.getZ()));
        r.add(new BlockPos(originalRunPos2.getX(), originalRunPos2.getY() + yDif, originalRunPos2.getZ()));
        r.add(new BlockPos(originalRunPos2.getX(), originalRunPos2.getY(), originalRunPos2.getZ() + zDif));

        r.add(originalRunPos1);
        r.add(originalRunPos2);

        for(BlockPos a : r){

            collapseNode(a);
            //System.out.println("Collapsed corner " + a + " to " + collapseMap.get(a));
            changeSurrounding(a, false);
        }
    }

    private void GenerateWorld(){
        //System.out.println("Made into Generate world");
        Iterator<BlockPos> iter = collapsed.iterator();
        //Add in, if you're void air do not generate
        while(iter.hasNext()){
            BlockPos current = iter.next();
            if(collapseMap.get(current).get(0) == -1){ 
                //world.setBlockState(current, AirBlock.getStateFromRawId(0));
                System.out.println("Should never get here");
            } else {
                world.setBlockState(current, integerToBlockMap.get(collapseMap.get(current).get(0)), 1);
            }
        }
    }

    //Unable to find least entropic value because it uncludes [] arrays
    private BlockPos findLeastEntropy(){

        BlockPos ret = null;

        for(BlockPos b : collapseMap.keySet()){
            if(!collapsed.contains(b)){
                int size1 = collapseMap.get(b).size();
                int size2 = (ret == null) ? Integer.MAX_VALUE : collapseMap.get(ret).size();
                if((size1 <= size2) && size1 != 0){
                    ret = b;
                }
            }
        }

        //System.out.println("Found block " + ret + " with least entropy of " + collapseMap.get(ret));

        return ret;
    }

    private void handleSingleSurroundingChange(BlockPos target, BlockPos current, int direction, boolean shouldWrap){
        Vector<Integer> thisAdj = adj.get(collapseMap.get(current).get(0)).get(direction); //Gets the current integer of the block that is at curent and then goes and finds that in adj
        Vector<Integer> newAdj = new Vector<Integer>();

        //Wrapping
        if(shouldWrap){
            target = wrapBlockPos(current);
        }

        if(collapseMap.containsKey(target) && !collapsed.contains(target)){ //Test if the position exists on the map AND if we have not collapsed the block before
            Vector<Integer> targetAdj = collapseMap.get(target);
            for(int i = 0; i < targetAdj.size(); i++){
                if(thisAdj.contains(targetAdj.get(i))){
                    newAdj.add(targetAdj.get(i));
                }
            }
            collapseMap.put(target, newAdj);
        }

        // if(newAdj.size() == 0){
        //     return false;
        // } else {
        //     return true;
        // }
    }

    private BlockPos wrapBlockPos(BlockPos current) {
        if(collapseMap.containsKey(current)){
            return current;
        } else {
            int newX = current.getX();
            int newY = current.getY();
            int newZ = current.getZ();

            //Test Run Pos 1 = 567, 65, -119
            //Test Run Pos 2 = 683, 19, 210

            int maxX = runPos1.getX() > runPos2.getX() ? runPos1.getX() : runPos2.getX(); //683
            int minX = runPos1.getX() < runPos2.getX() ? runPos1.getX() : runPos2.getX(); //567

            int maxY = runPos1.getY() > runPos2.getY() ? runPos1.getY() : runPos2.getY(); //65
            int minY = runPos1.getY() < runPos2.getY() ? runPos1.getY() : runPos2.getY(); //19

            int maxZ = runPos1.getZ() > runPos2.getZ() ? runPos1.getZ() : runPos2.getZ(); //210
            int minZ = runPos1.getZ() < runPos2.getZ() ? runPos1.getZ() : runPos2.getZ(); //-119

            if(WRAP.x == 1){
                if(current.getX() > maxX){
                    newX = minX;
                } else if (current.getX() < minX){
                    newX = maxX;
                }
            }

            if(WRAP.y == 1){
                if(current.getY() > maxY){
                    newY = minY;
                } else if (current.getY() < minY){
                    newY = maxY;
                }
            }

            if(WRAP.z == 1){
                if(current.getZ() > maxZ){
                    newZ = minZ;
                } else if (current.getZ() < minZ){
                    newZ = maxZ;
                }
            }
           
            return new BlockPos(newX, newY, newZ);
        }
        
    }
    
    private void changeSurrounding(BlockPos current, boolean shouldWrap){
        boolean ret = true;
        //Wrap current + direction
        //Change up 0
        handleSingleSurroundingChange(current.up(), current, 0, shouldWrap);
        //if(!ret){
        //    return false;
        //}

        //Change DOwn 1
        handleSingleSurroundingChange(current.down(), current, 1, shouldWrap);
        //if(!ret){
        //    return false;
        //}

        //CHange West 2
        handleSingleSurroundingChange(current.west(), current, 2, shouldWrap);
        // if(!ret){
        //     return false;
        // }

        //Change east
        handleSingleSurroundingChange(current.east(), current, 3, shouldWrap);
        // if(!ret){
        //     return false;
        // }

        //Change north
        handleSingleSurroundingChange(current.north(), current, 4, shouldWrap);
        // if(!ret){
        //     return false;
        // }
        
        //change south
        handleSingleSurroundingChange(current.south(), current, 5, shouldWrap);
        // if(!ret){
        //     return false;
        // }

        //return ret;
        //System.out.println(collapseMap);
    }


    //Returns true if it was able to collapse a node, and false if it was not
    private boolean collapseNode(BlockPos blockPos){

        //Get adj's from that current block
        Vector<Integer> potential = collapseMap.get(blockPos);
        

        if(potential.size() == 0){
            System.out.println("Found no adjacency");
            return false;
        }

        //Go find their weights
        int sum = 0;
        for(int i = 0; i < potential.size(); i++){
            sum += potential.get(i);
        }

        int idx = 0;
        for (double r = Math.random() * sum; idx < potential.size() - 1; ++idx) {
            r -= potential.get(idx);
            if (r <= 0.0) break;
        }
        int myRandomItem = potential.get(idx);
        Vector<Integer> t = new Vector<Integer>();
        t.add(myRandomItem);
        collapsed.add(blockPos);
        //System.out.println("Adding " + myRandomItem + " at " + block);
        //Pick one
        collapseMap.put(blockPos, t);
        return true;
    }

    //Builds Adjacency matrix and setups block to int conversion
    private int buildAdjacencies(){
        
        //Run from pos1 to pos2

        //If Pos2.X is greater than Pos1.X, we want to increase to pos2, otherwise decrease to pos1;
        //The same logic applies for y and z direction
        int xDir = pos1.getX() < pos2.getX() ? 1 : -1;
        int yDir = pos1.getY() < pos2.getY() ? 1 : -1;
        int zDir = pos1.getZ() < pos2.getZ() ? 1 : -1;


        //I hate this duplicate code, for adding in edges to the matrix
        Vector<Vector<Integer>> a = new Vector<Vector<Integer>>();
        for(int i = 0; i < 6; i++){
            a.add(new Vector<Integer>());
        }

        blockToIntegerMap.put(Blocks.VOID_AIR.getDefaultState(), -1);

        adj.put(-1, a);

        for(int x = pos1.getX(); x != pos2.getX() + xDir; x = x + xDir){
            for(int y = pos1.getY(); y != pos2.getY() + yDir; y = y + yDir){
                for(int z = pos1.getZ(); z != pos2.getZ() + zDir; z = z + zDir){
                    BlockPos thisPos = new BlockPos(x, y, z);
                    BlockState b = world.getBlockState(thisPos);
                    //If the block hasn't been seen yet, then we will add it to the list and increment our int
                    if(!blockToIntegerMap.containsKey(b)){
                        integerToBlockMap.put(currentIndex, b);
                        blockToIntegerMap.put(b, currentIndex);
                        listOfSeenBlocks.put(currentIndex, 1);

                        Vector<Vector<Integer>> newVec = new Vector<Vector<Integer>>();
                        //Disgusting way of adding the new vectors
                        newVec.add(new Vector<Integer>());
                        newVec.add(new Vector<Integer>());
                        newVec.add(new Vector<Integer>());
                        newVec.add(new Vector<Integer>());
                        newVec.add(new Vector<Integer>());
                        newVec.add(new Vector<Integer>());
                        
                        adj.put(currentIndex, newVec);
                        currentIndex++;
                    } else {
                        //Add 1 to the block in listOfSeenBlock
                        int thisBlock = blockToIntegerMap.get(b);
                        listOfSeenBlocks.put(thisBlock, listOfSeenBlocks.get(thisBlock) + 1);
                        
                    }

                    //I've now added to the list the block, time to check it's adjacencies
                    addAdjacencies(blockToIntegerMap.get(b), thisPos);
                    //If we added to the index, we need to add 1 at the end. 

                }
            }
        }

        //System.out.println(adj);
        //System.out.println(integerToBlockMap);
        //System.out.println(listOfSeenBlocks);

        return 0;
    }

    //Add's all the adjacencies for a block
    private void addAdjacencies(int originalBlockInt, BlockPos p){

        //System.out.println("Adding Adjacencies");

        //Wrap p + direction.
        BlockState newBlock;
        int idx;
        
        //Up
        BlockPos newPos = p.up();
        if(!evaulatePosition(newPos))
        {
            addSingleAdjacency(-1, originalBlockInt, 1);
            addSingleAdjacency(originalBlockInt, -1, 0);
        } else {
            newBlock = world.getBlockState(newPos);
            idx = testSymbolSeenBefore(newBlock);
            addSingleAdjacency(originalBlockInt, idx, 0);
        }

        //Down
        newPos = p.down();
        if(!evaulatePosition(newPos))
        {
            addSingleAdjacency(-1, originalBlockInt, 0);
            addSingleAdjacency(originalBlockInt, -1, 1);
        } else {
            newBlock = world.getBlockState(newPos);
            idx = testSymbolSeenBefore(newBlock);
            addSingleAdjacency(originalBlockInt, idx, 1);
        }
        //Left
        newPos = p.west();
        if(!evaulatePosition(newPos))
        {
            addSingleAdjacency(-1, originalBlockInt, 3);
            addSingleAdjacency(originalBlockInt, -1, 2);
        } else {
            newBlock = world.getBlockState(newPos);
            idx = testSymbolSeenBefore(newBlock);
            addSingleAdjacency(originalBlockInt, idx, 2);
        }

        //Right
        newPos = p.east();
        if(!evaulatePosition(newPos))
        {
            addSingleAdjacency(-1, originalBlockInt, 2);
            addSingleAdjacency(originalBlockInt, -1, 3);
        } else {
            newBlock = world.getBlockState(newPos);
            idx = testSymbolSeenBefore(newBlock);
            addSingleAdjacency(originalBlockInt, idx, 3);
        }

        //Forward
        newPos = p.north();
        if(!evaulatePosition(newPos))
        {
            addSingleAdjacency(-1, originalBlockInt, 5);
            addSingleAdjacency(originalBlockInt, -1, 4);
        } else {
            newBlock = world.getBlockState(newPos);
            idx = testSymbolSeenBefore(newBlock);
            addSingleAdjacency(originalBlockInt, idx, 4);
        }

        //Back
        newPos = p.south();
        if(!evaulatePosition(newPos))
        {
            //Add the original block to the list of adjacencies of block -1
            addSingleAdjacency(-1, originalBlockInt, 4);
            addSingleAdjacency(originalBlockInt, -1, 5);
        } else {
            newBlock = world.getBlockState(newPos);
            idx = testSymbolSeenBefore(newBlock);
            addSingleAdjacency(originalBlockInt, idx, 5);
        }
    }

    //Returns true if the position is inside of the boundaries of the input position, and false if it is not
    private boolean evaulatePosition(BlockPos position){
        int maxX = pos1.getX() > pos2.getX() ? pos1.getX() : pos2.getX();
        int minX = pos1.getX() < pos2.getX() ? pos1.getX() : pos2.getX();

        int maxY = pos1.getY() > pos2.getY() ? pos1.getY() : pos2.getY();
        int minY = pos1.getY() < pos2.getY() ? pos1.getY() : pos2.getY();

        int maxZ = pos1.getZ() > pos2.getZ() ? pos1.getZ() : pos2.getZ();
        int minZ = pos1.getZ() < pos2.getZ() ? pos1.getZ() : pos2.getZ();

        //If this position is inside of the boundaries, we don't have to do anything
        if((position.getX() >= minX && position.getX() <= maxX) &&
           (position.getY() >= minY && position.getY() <= maxY) &&
           (position.getZ() >= minZ && position.getZ() <= maxZ) ) {
                //System.out.println("Found " + position + " inside of matrix");
                return true;
           } else {
                //System.out.println("Found " + position + " outside of matrix");
                return false;
           }
    }

    //Adds the adjacencies in the direction
    private void addSingleAdjacency(int originalBlockInt, int newBlock, int direction){
        Vector<Integer> prevAdj = adj.get(originalBlockInt).get(direction);
        prevAdj.add(newBlock);
        //Scuffed way of removing duplicates
        LinkedHashSet<Integer> hashSet = new LinkedHashSet<Integer> (prevAdj);
        prevAdj.clear();
        prevAdj.addAll(hashSet);

        adj.get(originalBlockInt).set(direction, prevAdj);
    }

    //Tests a single block if it's been seen before, and if it has not then we add all the appropriate stuff
    private int testSymbolSeenBefore(BlockState b){
        if(!blockToIntegerMap.containsKey(b)){ //Test if the block has been seen before, if it hasn't we need to add it
            integerToBlockMap.put(++currentIndex, b);
            blockToIntegerMap.put(b, currentIndex);
            listOfSeenBlocks.put(currentIndex, 0); 

            Vector<Vector<Integer>> newVec = new Vector<Vector<Integer>>();
            //Disgusting way of adding the new vectors
            newVec.add(new Vector<Integer>());
            newVec.add(new Vector<Integer>());
            newVec.add(new Vector<Integer>());
            newVec.add(new Vector<Integer>());
            newVec.add(new Vector<Integer>());
            newVec.add(new Vector<Integer>());
            
            adj.put(currentIndex, newVec);    
        }

        return blockToIntegerMap.get(b);
    }

}
