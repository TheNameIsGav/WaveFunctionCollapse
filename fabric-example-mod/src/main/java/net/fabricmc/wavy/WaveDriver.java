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

    private BlockPos expandedRunPos1;
    private BlockPos expandedRunPos2;

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
    HashMap<BlockPos, Vector<Integer>> collapseMap = new HashMap<BlockPos, Vector<Integer>>();
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

        //System.out.println(listOfSeenBlocks);

        if(j < 0){
            return j;
        }

        //System.out.println(blockToIntegerMap);
        //System.out.println(adj);

        //System.out.println("Block to integer map \n" + blockToIntegerMap);
        //System.out.println("List of seen blocks \n" + listOfSeenBlocks);

        hasRunFirstStep = true;
        return 1;
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
        //System.out.println(t);
        //Setup The Beginning Collapse Map

        AddEdges();
        System.out.println("Made it past Add Edges");
       
        int xDir = runPos1.getX() < runPos2.getX() ? 1 : -1;
        int yDir = runPos1.getY() < runPos2.getY() ? 1 : -1;
        int zDir = runPos1.getZ() < runPos2.getZ() ? 1 : -1;

        for(int x = runPos1.getX(); x != runPos2.getX() + xDir; x = x + xDir){
            for(int y = runPos1.getY(); y != runPos2.getY() + yDir; y = y + yDir){
                for(int z = runPos1.getZ(); z != runPos2.getZ() + zDir; z = z + zDir){
                    collapseMap.put(new BlockPos(x, y, z), t); //Adds all the default adjacencies to the nodes
                }
            }
        }

        System.out.println("CollapseMap: " + collapseMap);

        System.out.println("Made it past adding blocks to collapse Map");

        ManipulateEdges();

        System.out.println("Made it past Manipulating Edges");
        
        //TODO Change corners so that they get reverted to the original setting
        CollapseCorners();

        System.out.println("Made it past Collapsing Corners");

        int itr = 10;
        boolean done = false;
        
        while(!done && itr > 0){
            itr--;
            
            //Find lowest entropy to collapse
            BlockPos current = findLeastEntropy();
            
            //Collapse it
            collapseNode(current);

            //Change the surrounding nodes
            changeSurrounding(current, SHOULDWRAP);

            if(collapsed.size() == collapseMap.keySet().size()){
                done = true;
            }
            
        }

        System.out.println("Made it past the waavy step");

        System.out.println(collapseMap.keySet().size());

        GenerateWorld();
        return 1;
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

        runPos1 = new BlockPos(maxX, maxY, maxZ);
        runPos2 = new BlockPos(minX, minY, minZ);

    }
    
    //Add's the -1 adjacency to all the edges and updates the nodes on the inside
    private void ManipulateEdges(){

        Vector<BlockPos> edges = new Vector<BlockPos>();

        for(BlockPos bp : collapseMap.keySet()){
            System.out.println("Block position: " + bp);
            int x = bp.getX();
            int y = bp.getY();
            int z = bp.getZ();
            
            if(!collapsed.contains(bp)){ //If we have not seen this before
                System.out.println("Inside of the if statement");
                //If we are on one of the xEdges
                if(x == runPos1.getX() ||  x == runPos2.getX()) {
                    System.out.println("Inside of the second if statement");
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
        int xDif = runPos1.getX() - runPos2.getX();
        int yDif = runPos1.getY() - runPos2.getY();
        int zDif = runPos1.getZ() - runPos2.getZ();

        Vector<BlockPos> r = new Vector<BlockPos>();

        r.add(new BlockPos(runPos1.getX() - xDif, runPos1.getY(), runPos1.getZ()));
        r.add(new BlockPos(runPos1.getX(), runPos1.getY() - yDif, runPos1.getZ()));
        r.add(new BlockPos(runPos1.getX(), runPos1.getY(), runPos1.getZ() - zDif));

        r.add(new BlockPos(runPos2.getX() + xDif, runPos2.getY(), runPos2.getZ()));
        r.add(new BlockPos(runPos2.getX(), runPos2.getY() + yDif, runPos2.getZ()));
        r.add(new BlockPos(runPos2.getX(), runPos2.getY(), runPos2.getZ() + zDif));

        r.add(runPos1);
        r.add(runPos2);

        for(BlockPos a : r){

            System.out.println("Collapsing corner: " + a);
            collapseNode(a);
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
            } else {
                world.setBlockState(current, integerToBlockMap.get(collapseMap.get(current).get(0)), 1);
            }
        }
    }

    private BlockPos findLeastEntropy(){

        BlockPos ret = runPos1; //Position garunteed to be in the original map
        for(BlockPos b : collapseMap.keySet()){
            if(!collapsed.contains(b)){
                if(collapseMap.get(b).size() <= collapseMap.get(ret).size()){
                    ret = b;
                }
            }
        }

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

        //Wrap current + direction
        //Change up 0
        handleSingleSurroundingChange(current.up(), current, 0, shouldWrap);

        //Change DOwn 1
        handleSingleSurroundingChange(current.down(), current, 1, shouldWrap);

        //CHange West 2
        handleSingleSurroundingChange(current.west(), current, 2, shouldWrap);

        //Change east
        handleSingleSurroundingChange(current.east(), current, 3, shouldWrap);

        //Change north
        handleSingleSurroundingChange(current.north(), current, 4, shouldWrap);
        
        //change south
        handleSingleSurroundingChange(current.south(), current, 5, shouldWrap);
        //System.out.println(collapseMap);
    }

    private void collapseNode(BlockPos blockPos){

        //Get adj's from that current block
        Vector<Integer> potential = collapseMap.get(blockPos);
        

        if(potential.size() == 0){
            Vector<Integer> x = new Vector<Integer>();
            System.out.println(potential + " " + blockPos);
            x.add(-1);
            collapseMap.put(blockPos, x);
            return;
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

        System.out.println(blockToIntegerMap);

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

        System.out.println("Adding Adjacencies");

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
                System.out.println("Found " + position + " inside of matrix");
                return true;
           } else {
                System.out.println("Found " + position + " outside of matrix");
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
