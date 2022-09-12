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
import net.minecraft.util.math.Vec3d;
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
            //System.out.println("Did not create new file");
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

        //System.out.println("Printing Data from Load File: ");
        //System.out.println("Integer To Block Map: " + integerToBlockMap);
        //System.out.println("List of Seen Blocks: " + listOfSeenBlocks);
        //System.out.println("Printing out Adjacency Matrix: " + adj);
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

        ////System.out.println(block + ":" + propertyMap); 

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
            //System.out.println("Somehow Minecraft Client was null");
        }
    }

    boolean hasRunFirstStep = false;    

    //Start Chunk Implementation
    public class WFCChunk {
        int _id;

        Integer[][][] _chunkBlockValues = new Integer[chunkSize][chunkSize][chunkSize];

        WFCChunk() {
            _id = -4;
        }

        WFCChunk(int id, Integer[][][] adjs){
            _chunkBlockValues = adjs;
            _id = id;
        }

        @Override
        public String toString() {
            return "(" + this._id + ":" + this._chunkBlockValues.toString() + ")";
        }

        @Override
        public boolean equals(Object o) {
 
            // If the object is compared with itself then return true 
            if (o == this) {
                return true;
            }

            /* Check if o is an instance of Complex or not
              "null instanceof [type]" also returns false */
            if (!(o instanceof WFCChunk)) {
                return false;
            }
            
            // typecast o to Complex so that we can compare data members
            WFCChunk c = (WFCChunk) o;
            
            // Compare the data members and return accordingly
            for(int x = 0; x < chunkSize; x++){
                for(int y = 0; y < chunkSize; y++){
                    for(int z = 0; z < chunkSize; z++){
                        if(this._chunkBlockValues[x][y][z] != c._chunkBlockValues[x][y][z]){
                            return false;
                        }
                    }
                }
            }

            return true;
        }
    }

    //Setting up the default new structures
    
    //integer to block map equivalent
    HashMap<Integer, WFCChunk> integerToChunkMap = new HashMap<Integer, WFCChunk>();

    //Block to integer map equivalent
    HashMap<WFCChunk, Integer> chunkToIntegerMap = new HashMap<WFCChunk, Integer>();

    //list of seen blocks equivalent
    HashMap<Integer, Integer> listOfSeenChunks = new HashMap<Integer, Integer> ();
    //current index is 0;

    //Stores the blocks that we've seen in chunks
    HashMap<BlockState, Integer> seenBlocksToInt = new HashMap<BlockState, Integer>();
    HashMap<Integer, BlockState> seenIntToBlock = new HashMap<Integer, BlockState>();
    int blockInt = 0;
    //Uses normal adj list

    boolean _DEBUG = true;

    public int firstStepWFCChunks(){
        if(pos1 == null || pos2 == null){
            print("Failed to validate Position 1 or Position 2" + "\n Position 1 is " + pos1 + "\n Position 2 is " + pos2);
            return -1;
        }
        
        if(world == null){
            print("Failed to get world correctly");
            return -3;
        }

        //Reset all Data Structures
        integerToChunkMap = new HashMap<Integer, WFCChunk>();
        chunkToIntegerMap = new HashMap<WFCChunk, Integer> ();
        listOfSeenChunks = new HashMap<Integer, Integer> ();
        seenBlocksToInt = new HashMap<BlockState, Integer> ();
        seenIntToBlock = new HashMap<Integer, BlockState> ();
        adj = new HashMap<Integer, Vector<Vector<Integer>>>();
        currentIndex = 0;
        blockInt = 0;


        //Set the mine and max positions of the grid.
        min = new BlockPos(pos1.getX() < pos2.getX() ? pos1.getX() : pos2.getX(), 
                                    pos1.getY() < pos2.getY() ? pos1.getY() : pos2.getY(), 
                                    pos1.getZ() < pos2.getZ() ? pos1.getZ() : pos2.getZ());

        max = new BlockPos(pos1.getX() > pos2.getX() ? pos1.getX() : pos2.getX(), 
                                    pos1.getY() > pos2.getY() ? pos1.getY() : pos2.getY(), 
                                    pos1.getZ() > pos2.getZ() ? pos1.getZ() : pos2.getZ());

        //Read in all singleton blocks so that it's easy
        validateAllBlocks(min, max);

        //Goes through the chunks in the matrix and assembles blocks without associating adjcencies. 
        createChunks();

        //Read in Input Data with chunk size
        //int j = buildAdjacenciesChunks();
        hasRunFirstStep = true;

        return 1;
    }

    BlockPos min;
    BlockPos max;

    BlockPos runMin;
    BlockPos runMax;

    //Grid of the input as integers
    Integer[][][] inputIntegersGrid;
    static int chunkSize = 5;

    private void createChunks(){

        System.out.println("Chunk Size: " + chunkSize);

        int xLength = max.getX() - min.getX() + 2 - chunkSize;
        int yLength = max.getY() - min.getY() - chunkSize + 2;
        int zLength = max.getZ() - min.getZ() - chunkSize + 2;


        System.out.println("X Length of Array: " + xLength);
        System.out.println("Y Length of Array: " + yLength);
        System.out.println("Z Length of Array: " + zLength);


        inputIntegersGrid = new Integer[xLength][yLength][zLength];
        //Go from max to (min + chunkSize)
        //Each block position is treated as 'maximal' position of the chunk
        //Read in the values for that specific block and make sure that we don't already have it, then add it
        //Read that as an integer, and then put that integer into the (x, y, z) coordinates
        /*for(int x = max.getX(); x >= min.getX() + chunkSize; x--){
            for(int y = max.getY(); y >= min.getY() + chunkSize; y--){
                for(int z = max.getZ(); z >= min.getZ() + chunkSize; z--){
                    WFCChunk ret = addChunk(x, y, z, true); //Validated with single chunk size

                    //Take the returned chunk, add it to the pre-map of integers


                }
            }
        }*/
    }

    private int buildAdjacenciesChunks(){

        //Add edge
        Vector<Vector<Integer>> a = new Vector<Vector<Integer>>();
        for(int i = 0; i < 6; i++){
            Vector<Integer> b = new Vector<Integer>();
            a.add(b);
        }
        adj.put(-1, a);
        
        //For each position, assume top left and build from there
        for(int x = max.getX(); x >= min.getX() + chunkSize-1; x--){
            for(int y = max.getY(); y >= min.getY() + chunkSize-1; y--){
                for(int z = max.getZ(); z >= min.getZ() + chunkSize-1; z--){
                    ////System.out.println("Chunk Start Pos: " + x + ", " + y + ", " + z);
                    WFCChunk newChunk = addChunk(x, y, z, true); //Validated with single chunk size

                    AddChunkAdjacencies(newChunk._id, new BlockPos(x, y, z));
                }
            }
        }

        return 1;
    }

    private boolean evaulatePositionRework(BlockPos test){

        if(test.getX() > max.getX() || test.getX() < min.getX()){
            return false;
        }

        if(test.getY() > max.getY() || test.getY() < min.getY()){
            return false;
        }

        if(test.getZ() > max.getZ() || test.getZ() < min.getZ()){
            return false;
        }

        return true;
    }

    //Modify these methods - I never get to a blockposition that cannot be read in it's entirety, so I just need to check if it's got an edge

    //Its not that specific directions don't work, it's that multiple elements in the adjs don't work
    private void AddChunkAdjacencies(int id, BlockPos pos) {
        /*
            Up - 0, Down - 1
            West - 2, East - 3
            North - 4, South - 5
        */

        //Checking the 3 min-chunk borders
        Vector3d minBorders = chunkIsBorderingEdgeMin(pos);

        Vector3d maxBorders = chunkIsBorderingEdgeMax(pos);
        
        if(minBorders.x == 1){         
            addChunkEdgeAdjacency(id, 2, 3);
        } else { //Normal
            addChunkAdjacency(id, 2, pos.west());
        }

        if(minBorders.y == 1) { //We have a down edge (1) 
            addChunkEdgeAdjacency(id, 1, 0);
        } else { //Normal
            addChunkAdjacency(id, 1, pos.down());
        }

        if(minBorders.z == 1){ //We have a northward edge (4)
            addChunkEdgeAdjacency(id, 4, 5);
        } else { //Normal
            addChunkAdjacency(id, 4, pos.north());
        }

        //Checking the 3 max borders
        

        if(maxBorders.x == 1){ //We have a Eastward Edge (3)
            addChunkEdgeAdjacency(id, 3, 2);
        } else {
            addChunkAdjacency(id, 3, pos.east());
        }

        if(maxBorders.y == 1){ //We have an up edge (0)
            addChunkEdgeAdjacency(id, 0, 1);
        } else {
            addChunkAdjacency(id, 0, pos.up());
        }

        if(maxBorders.z == 1) {//We have a Southern Edge (5)
            addChunkEdgeAdjacency(id, 5, 4);
        } else {
            addChunkAdjacency(id, 5, pos.south());
        }
    }

    private Vector3d chunkIsBorderingEdgeMax(BlockPos curr) {
        int xThreshold = max.getX();
        int yThreshold = max.getY();
        int zThreshold = max.getZ();

        Vector3d ret = new Vector3d(0,0, 0);

        if(curr.getX() == xThreshold){
            ret.x = 1;
        }

        if(curr.getY() == yThreshold){
            ret.y = 1;
        }

        if(curr.getZ() == zThreshold){
            ret.z = 1;
        }
    
        return ret;
    }

    //If the block position that I am currently on is equal to x, y, or z chunk thresholds, return the axis to add edge adjacency
    //returns a vector of axis that border

    private Vector3d chunkIsBorderingEdgeMin(BlockPos curr){
        int xThreshold = min.getX() + chunkSize - 1;
        int yThreshold = min.getY() + chunkSize - 1;
        int zThreshold = min.getZ() + chunkSize - 1;

        Vector3d ret = new Vector3d(0,0,0);

        if(curr.getX() == xThreshold){
            ret.x = 1;
        }

        if(curr.getY() == yThreshold){
            ret.y = 1;
        }

        if(curr.getZ() == zThreshold){
            ret.z = 1;
        }
    
        return ret;
    }

    //Add chunk adjacency and then edge adjacency in the opposite direction
    private void addChunkEdgeAdjacency(int id, int direction, int opposite){
        Vector<Integer> prevAdj = adj.get(id).get(direction);
        if(!prevAdj.contains(-1)){
            prevAdj.add(-1);
            adj.get(id).set(direction, prevAdj);
        }
        
        Vector<Integer> testPrev = adj.get(-1).get(opposite); 
        if(!testPrev.contains(id)){
            testPrev.add(id);
            adj.get(-1).set(opposite, testPrev);
        }
        
    }

    private void addChunkAdjacency(int id, int direction, BlockPos newPos){
        WFCChunk adjChunk = addChunk(newPos.getX(), newPos.getY(), newPos.getZ(), false);
        ////System.out.println("Adjacent Chunk: " + adjChunk);
        Vector<Integer> originalAdjs = adj.get(id).get(direction);

        if(!originalAdjs.contains(adjChunk._id)){
            originalAdjs.add(adjChunk._id);
            adj.get(id).set(direction, originalAdjs);
        }
    }

    //Adds all singleton blocks within the input grid for use within chunk adjacencies
    //This doesn't need chunk size adjustment because its for singletons
    private void validateAllBlocks(BlockPos min, BlockPos max) {

        for(int x = min.getX(); x <= max.getX(); x++){
            for(int y = min.getY(); y <= max.getY(); y++){
                for(int z = min.getZ(); z <= max.getZ(); z++){

                    BlockPos p = new BlockPos(x, y, z);

                    if(!seenBlocksToInt.keySet().contains(world.getBlockState(p))){
                        seenIntToBlock.put(blockInt, world.getBlockState(p));
                        seenBlocksToInt.put(seenIntToBlock.get(blockInt), blockInt);
                        blockInt++;
                    }
                }
            }
        }
    }

    //Given the top left corner of a block, extends to read in blocks in the size of the chunk
    private WFCChunk addChunk(int x, int y, int z, boolean actual){
        WFCChunk ret = new WFCChunk();

        //Actually read in the values from the set
        Integer[][][] a = readChunkByChunkSize(x, y, z);
        ret._chunkBlockValues = a;

        Vector<Vector<Integer>> newVec = new Vector<Vector<Integer>>();
        for(int i = 0; i < 6; i++){
            newVec.add(new Vector<Integer>());
        }

        //Check to see if we have seen this particular arrangement of chunk before
        int flag = checkChunkSeenBefore(ret);

        if(flag == -2){ //This is a new chunk
            ret._id = currentIndex;
            currentIndex++;
            listOfSeenChunks.put(ret._id, 1);
            adj.put(ret._id, newVec);
            integerToChunkMap.put(ret._id, ret);
            chunkToIntegerMap.put(ret, ret._id);
            ////System.out.println("New Ret: " + ret);
        } else if (actual){ //This is a seen chunk, so increase amt by 1
            ret = integerToChunkMap.get(flag);
            
            listOfSeenChunks.put(flag, listOfSeenChunks.get(flag) + 1);
            ////System.out.println("Found old chunk with values " + ret);
        } else {
            ret = integerToChunkMap.get(flag);
        }
        return ret;
    }

    //Given a starting coordinate, returns the blocks inside that chunk
    private Integer[][][] readChunkByChunkSize(int c, int u, int v){
        Integer[][][] a = new Integer[chunkSize][chunkSize][chunkSize];

        for(int x = 0; x < chunkSize; x++){
            for(int y = 0; y < chunkSize; y++){
                for(int z = 0; z < chunkSize; z++){
                    BlockPos curr = new BlockPos(c - x, u - y, v - z);
                    ////System.out.println("Printing current position inside readChunkByChunkSize: " + curr);

                    //If our position is inside the grid
                    if(evaulatePositionRework(curr)){
                        //Adds that int to our array
                        a[x][y][z] = seenBlocksToInt.get(world.getBlockState(curr));


                    } else {
                        //System.out.println("we should not get here if I have done my math properly");
                        //a.add(-1);
                    }
                }
            }
        }
        return a;
    }

    //Checks a given chunk to see if it's blocks match another in the set
    //If they don't match, pass back -2 flag
    //If do match, pass back index of that chunk
    private Integer checkChunkSeenBefore(WFCChunk inc){
        Set<WFCChunk> setOfChunks = chunkToIntegerMap.keySet();
        Iterator<WFCChunk> itr = setOfChunks.iterator();

        while(itr.hasNext()){
            WFCChunk curr = itr.next();
            boolean matches = inc.equals(curr);

            //If we get through the entire Vector and don't find anything different, then we have found a match
            if(matches){
                return chunkToIntegerMap.get(curr);
            }

        }
        return -2;
    }

    //README

    /*

    At the end of the semester you had just finished working on the algorithm so that it worked with single chunks
    You have not tested it with chunks of different sizes yet. 

    */

    public int secondStepChunkWFC(){

        collapseMap = new HashMap<BlockPos, Vector<Integer>>();
        collapsed = new HashSet<BlockPos>();

        if(!hasRunFirstStep){
            print("It appears you haven't run the first WFC Step");
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

        Vector<Integer> t = new Vector<Integer>(listOfSeenChunks.keySet());

        //Setup minimum and maximum positions for the matrix
        runMin = new BlockPos((runPos1.getX() < runPos2.getX() ? runPos1.getX() : runPos2.getX())  + chunkSize -2, 
                                (runPos1.getY() < runPos2.getY() ? runPos1.getY() : runPos2.getY()) + chunkSize -2, 
                                (runPos1.getZ() < runPos2.getZ() ? runPos1.getZ() : runPos2.getZ()) + chunkSize -2);

        runMax = new BlockPos((runPos1.getX() > runPos2.getX() ? runPos1.getX() : runPos2.getX()) + 1, 
                                (runPos1.getY() > runPos2.getY() ? runPos1.getY() : runPos2.getY()) + 1, 
                                (runPos1.getZ() > runPos2.getZ() ? runPos1.getZ() : runPos2.getZ()) + 1);

        //System.out.println("RunMin: " + runMin);
        //System.out.println("Run Max: " + runMax);

        //Add all possible positions to map
        for(int x = runMax.getX(); x >= runMin.getX(); x--){
            for(int y = runMax.getY(); y >= runMin.getY(); y--){
                for(int z = runMax.getZ(); z >= runMin.getZ(); z--){
                    collapseMap.put(new BlockPos(x, y, z), t);
                }
            }
        }

        //Expand the run position by 1 and add a ring of edge adjacencies
        //AddEdges(); We don't have to run this because all it does it update the positions, we can just use runMin and runMax instead
        ManipulateEdges();

        //System.out.println("Collapse Map: " + collapseMap);
        //System.out.println("Collapsed: " + collapsed);

        int itr = 1000;
        boolean done = false;
        boolean validConfig = true;

        ////System.out.println("Test 1");
        
        while(!done && itr > 0){
            itr--;
            ////System.out.println(collapsed.size());
            if(collapsed.size() == collapseMap.keySet().size()){
                done = true;
                break;
            }
            ////System.out.println("Test 2");

            BlockPos current = findLeastEntropy();

            ////System.out.println("Test 3");

            if(current == null){
                //System.out.println("Found invalid entropic block");
                validConfig = false;
                break;
            }

            ////System.out.println("Test 4");

            //Collapse it
            if(!collapseNode(current)){
                //System.out.println("Found invalid config");
                validConfig = false;
                break;
            }

            ////System.out.println("Test 5");

            changeSurrounding(current, SHOULDWRAP);

            ////System.out.println("Test 6");
        }

        ////System.out.println("Test Final");

        RemoveEdgeBlocks();

        runPos1 = originalRunPos1;
        runPos2 = originalRunPos2;

        if(validConfig){
            return 1;
        } else {
            return -4;
        }
    }

    public void GenerateChunkWorld(){
        print("started to generate world");
        Iterator<BlockPos> iter = collapsed.iterator();
        while(iter.hasNext()){
            BlockPos current = iter.next();

            //This needs to figure out how to generate in a certain direction should the condition be met
                //For now just generates the maximum block
            for(int x = 0; x < chunkSize; x++){
                for(int y = 0; y < chunkSize; y++){
                    for(int z = 0; z < chunkSize; z++){
                        world.setBlockState(current, seenIntToBlock.get(integerToChunkMap.get(collapseMap.get(current).get(0))._chunkBlockValues[x][y][z]), 1);
                    }
                }
            }
           
        }
    }

    public int secondStepWrapper(int runs){
        print("Running WFC with " + runs + " runs.");
        int ret = 0;
        for(int i = 0; i < runs; i++){
            ret = secondStepChunkWFC();
            if(ret == 1){
                break;
            }
        }

        print("We got out!");

        if(ret == 1){
            GenerateChunkWorld();
        } else {
            print("Failed to find valid configuration in " + runs + " runs.");
        }

        return ret;
    }

    private void RemoveEdgeBlocks(){

        Set<BlockPos> temp = collapseMap.keySet();
        Vector<BlockPos> stuff = new Vector<BlockPos>();

        for(BlockPos b : temp ){
            if(collapseMap.get(b).contains(-1)){
                stuff.add(b);
            }
        }
        ////System.out.println("Made it past adding bad blocks");

        for(BlockPos b : stuff){
            collapseMap.remove(b);
            collapsed.remove(b);
        }
    }

    //Add's the -1 adjacency to all the edges and updates the nodes on the inside
    private void ManipulateEdges(){

        Vector<BlockPos> edges = new Vector<BlockPos>();

        for(BlockPos bp : collapseMap.keySet()){
            ////System.out.println("Block position: " + bp);
            int x = bp.getX();
            int y = bp.getY();
            int z = bp.getZ();
            
            if(!collapsed.contains(bp)){ //If we have not seen this before
                ////System.out.println("Inside of the if statement");
                //If we are on one of the xEdges
                if(x == runMax.getX() ||  x == runMin.getX()) {
                    ////System.out.println("Inside of the second if statement");
                    collapseMap.put(bp, new Vector<Integer>(){{add(-1);}});
                    collapsed.add(bp);
                    edges.add(bp);
                }

                //if we are on one of the yEdges
                if(y == runMax.getY() || y == runMin.getY()) {
                    collapseMap.put(bp, new Vector<Integer>(){{add(-1);}});
                    collapsed.add(bp);
                    edges.add(bp);
                }

                //if we are on one of the zEdges 
                if(z == runMax.getZ() || z == runMin.getZ()){
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

        ////System.out.println("Found block " + ret + " with least entropy of " + collapseMap.get(ret));

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
        ////System.out.println(collapseMap);
    }

    public int testCollapseNode(){
        Vector<Integer> holder = collapseMap.get(runPos1);

        //System.out.println("Attempting to collapse a single node! Before collapse potentials are "  + collapseMap.get(runPos1));

        collapseNode(runPos1);

        //System.out.println("After collapse: " + collapseMap.get(runPos1));

        collapseMap.put(runPos1, holder);
        collapsed.remove(runPos1);

        return 0;
    }

    //Returns true if it was able to collapse a node, and false if it was not
    private boolean collapseNode(BlockPos blockPos){

        //Get adj's from that current block
        Vector<Integer> potential = collapseMap.get(blockPos);
        Vector<Integer> pickVec = new Vector<Integer>();

        ////System.out.println("Test 4.1");

        if(potential.contains(-1)){
            return false;
        }

        ////System.out.println("Test 4.2");

        for(int i : potential){
            for(int j = 0; j < listOfSeenChunks.get(i); j++){
                pickVec.add(i);
            }
        }

        ////System.out.println("Test 4.3");
        
        int picked = (int) (Math.random() * pickVec.size());

        Vector<Integer> t = new Vector<Integer>();
        t.add(pickVec.get(picked));

        ////System.out.println("Test 4.4");

        collapsed.add(blockPos);
        collapseMap.put(blockPos, t);

        ////System.out.println("Test 4.5");

        return true;
    }

    /*
    0 - Up
    1 - Down
    2 - Left
    3 - Right
    4 - Forward
    5 - Back
    */

    //Spawns the read chunks into the world for debug purposes
    public int debugChunks(Vec3d playerPos) {

        BlockPos startPos = new BlockPos(playerPos.getX(), playerPos.getY(), playerPos.getZ());

        for(int c : listOfSeenChunks.keySet()){
            //System.out.println(integerToChunkMap.get(x)._chunkAdjs);
            //is read in by z, y, x, figure out what that means
            for (int x = 0; x < chunkSize; x++){
                for (int y = 0; y < chunkSize; y++){
                    for (int z = 0; z < chunkSize; z++){
                        BlockPos toSpawn = new BlockPos(startPos.getX() + x, startPos.getY() + y, startPos.getZ() + z);
                        world.setBlockState(toSpawn, seenIntToBlock.get(integerToChunkMap.get(c)._chunkBlockValues[x][y][z]),1);
                    }
                }
            }
        }   


        return 1;
    }

}