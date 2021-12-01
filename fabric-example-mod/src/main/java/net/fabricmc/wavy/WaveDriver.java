package net.fabricmc.wavy;


import java.util.*;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.MessageType;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
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
    //private Vector3d wrap = new Vector3d(1, 0, 1);

    //Used to convert from an intger to a block. Used when initially reading through the input
    Map<Integer, BlockState> integerToBlockMap = new HashMap<Integer, BlockState>( );
    Map<BlockState, Integer> blockToIntegerMap = new HashMap<BlockState, Integer>( );

    //Lists all the blocks (As integers) that we have seen, and their count
    Map<Integer, Integer> listOfSeenBlocks = new HashMap<Integer, Integer> ( );
    int currentIndex = 0;

    //Map of Integer ID to A list of length 6 with each element being the adjacencies found at that direction
    /*
    0 - Up
    1 - Down
    2 - Left
    3 - Right
    4 - Forward
    5 - Back
    */
    Map<Integer, Vector<Vector<Integer>> > adj = new HashMap<Integer, Vector<Vector<Integer>>>();
    Map<BlockPos, Vector<Integer>> collapseMap = new HashMap<BlockPos, Vector<Integer>>();
    Map<BlockPos, Vector<Integer>> backupMap = new HashMap<>(collapseMap); //Backup map

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
        int lenX = Math.abs( pos1.getX() - pos2.getX() );
        int lenY = Math.abs( pos1.getY() - pos2.getY() );
        int lenZ = Math.abs( pos1.getZ() - pos2.getZ() );
        int area = lenX * lenY * lenZ;
        if( area >= constraint){
            print("Your selected area appears to be too large with size " + area + ". The current constraint is set at " + constraint + ". If you would like to change this, please use the /constrain command (Non-functional), but procede with caution");
            return -2;
        }

        //Reset all the nonsense
        integerToBlockMap = new HashMap<Integer, BlockState>( );
        blockToIntegerMap = new HashMap<BlockState, Integer>( );
        listOfSeenBlocks = new HashMap<Integer, Integer> ( );
        adj = new HashMap<Integer, Vector<Vector<Integer>>>();
        currentIndex = 0;

        //Read in the input data
        int j = buildAdjacencies();

        if(j < 0){
            return j;
        }

        //System.out.println(blockToIntegerMap);
        //System.out.println(listOfSeenBlocks);

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

        
        backupMap = new HashMap<>(collapseMap);
        //System.out.println(collapseMap.size());

        CollapseCorners();

        int itr = 500;
        boolean done = false;
        /*
        while(!done && itr > 0){
            itr--;

            //Find lowest entropy to collapse
            BlockPos current = findLeastEntropy();
            //System.out.println("Current " + current);
            //Something here doesn't work, I don't think that finding least entropy works the way that it is supposed to
            //Collapse it
            collapseNode(current);
            
            //Change the surrounding nodes
            changeSurrounding(current);

            if(collapsed.size() == collapseMap.keySet().size()){
                done = true;
            }
            
        }*/

        GenerateWorld();
        return 1;
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
            collapseNode(a);
            changeSurrounding(a);
        }
    }

    private void GenerateWorld(){
        Iterator<BlockPos> iter = collapsed.iterator();
        System.out.println(collapsed);
        while(iter.hasNext()){
            BlockPos current = iter.next();
            world.setBlockState(current, integerToBlockMap.get(collapseMap.get(current).get(0)));
        }
    }

    Set<BlockPos> collapsed = new HashSet<BlockPos>();
    private BlockPos findLeastEntropy(){
        BlockPos[] blockArr = new BlockPos[collapseMap.size()];
        collapseMap.keySet().toArray(blockArr);
        BlockPos ret = blockArr[0];
        for(int i = 0; i < blockArr.length; i++){
            if(!collapsed.contains(blockArr[i])){ //If we haven't collapsed this node before
                if(collapseMap.get(blockArr[i]).size() <= collapseMap.get(ret).size()){ //If our size is less
                    ret = blockArr[i];
                }
            }
        }
        
        return ret;
    }

    private void handleSingleSurroundingChange(BlockPos target, BlockPos current, int direction){
        Vector<Integer> thisAdj = adj.get(collapseMap.get(current).get(0)).get(direction); //Gets the current integer of the block that is at curent and then goes and finds that in adj
        Vector<Integer> newAdj = new Vector<Integer>();
        if(collapseMap.containsKey(target) && !collapsed.contains(target)){ //Test if the position exists on the map AND if we have no seen the block before
            Vector<Integer> targetAdj = collapseMap.get(target);
            for(int i = 0; i < targetAdj.size(); i++){
                if(thisAdj.contains(targetAdj.get(i))){
                    newAdj.add(targetAdj.get(i));
                }
            }
            collapseMap.put(target, newAdj);
        }
    }

    private void changeSurrounding(BlockPos current){

        //Wrap current + direction
        //Change up 0
        handleSingleSurroundingChange(current.up(), current, 0);

        //Change DOwn 1
        handleSingleSurroundingChange(current.down(), current, 1);

        //CHange West 2
        handleSingleSurroundingChange(current.west(), current, 2);

        //Change east
        handleSingleSurroundingChange(current.east(), current, 3);

        //Change north
        handleSingleSurroundingChange(current.north(), current, 4);
        
        //change south
        handleSingleSurroundingChange(current.south(), current, 5);
        System.out.println(collapseMap);
    }

    private void collapseNode(BlockPos block){

        //Get adj's from that current block
        Vector<Integer> potential = collapseMap.get(block);

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
        collapsed.add(block);
        System.out.println("Adding " + myRandomItem + " at " + block);
        //Pick one
        collapseMap.put(block, t);
    }

    //Builds Adjacency matrix and setups block to int conversion
    private int buildAdjacencies(){
        
        //Run from pos1 to pos2

        //If Pos2.X is greater than Pos1.X, we want to increase to pos2, otherwise decrease to pos1;
        //The same logic applies for y and z direction
        int xDir = pos1.getX() < pos2.getX() ? 1 : -1;
        int yDir = pos1.getY() < pos2.getY() ? 1 : -1;
        int zDir = pos1.getZ() < pos2.getZ() ? 1 : -1;

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
                    addAdjacencies(b, thisPos);
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
    private void addAdjacencies(BlockState b, BlockPos p){

        //Wrap p + direction
        
        //Up
        BlockPos newPos = p.up();
        BlockState newBlock = world.getBlockState(newPos);
        testSymbolSeenBefore(newBlock);
        addSingleAdjacency(b, newBlock, 0);

        //Down
        newPos = p.down();
        newBlock = world.getBlockState(newPos);
        testSymbolSeenBefore(newBlock);
        addSingleAdjacency(b, newBlock, 1);

        //Left
        newPos = p.west();
        newBlock = world.getBlockState(newPos);
        testSymbolSeenBefore(newBlock);
        addSingleAdjacency(b, newBlock, 2);

        //Right
        newPos = p.east();
        newBlock = world.getBlockState(newPos);
        testSymbolSeenBefore(newBlock);
        addSingleAdjacency(b, newBlock, 3);

        //Forward
        newPos = p.north();
        newBlock = world.getBlockState(newPos);
        testSymbolSeenBefore(newBlock);
        addSingleAdjacency(b, newBlock, 4);

        //Back
        newPos = p.south();
        newBlock = world.getBlockState(newPos);
        testSymbolSeenBefore(newBlock);
        addSingleAdjacency(b, newBlock, 5);
    }

    //Adds the adjacencies in the direction
    private void addSingleAdjacency(BlockState b, BlockState newBlock, int direction){
        Vector<Integer> prevAdj = adj.get(blockToIntegerMap.get(b)).get(direction);
        prevAdj.add(blockToIntegerMap.get(newBlock));
        //Scuffed way of removing duplicates
        LinkedHashSet<Integer> hashSet = new LinkedHashSet<Integer> (prevAdj);
        prevAdj.clear();
        prevAdj.addAll(hashSet);

        adj.get(blockToIntegerMap.get(b)).set(direction, prevAdj);
    }

    //Tests a single block if it's been seen before, and if it has not then we add all the appropriate stuff
    private void testSymbolSeenBefore(BlockState b){
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
    }

}