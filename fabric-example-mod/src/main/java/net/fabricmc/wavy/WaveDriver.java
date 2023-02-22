package net.fabricmc.wavy;

import java.util.*;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;


public class WaveDriver {
    public static WaveDriver instance;

    private World world;
    public void World(World w) {world = w; }
    public World World() {return world;}

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
    
    public class WFCChunk {
        int _id;

        Integer[][][] _chunkBlockValues = new Integer[chunkSize][chunkSize][chunkSize];

        int _count;

        WFCChunk() {
            _id = -4;
        }

        WFCChunk(int id, Integer[][][] adjs){
            _chunkBlockValues = adjs;
            _id = id;
            _count = 0;
        }

        @Override
        public String toString() {

            String ret = "";

            for(int x = 0; x < _chunkBlockValues.length; x++){
                ret += "[";
                for(int y = 0; y < _chunkBlockValues[x].length; y++){
                    ret += "[";
                    for(int z = 0; z < _chunkBlockValues[x][y].length; z++){
                        ret += _chunkBlockValues[x][y][z].toString() + ",";
                    }
                    ret = ret.substring(0, ret.length()-1);
                    ret += "],";
                }
                ret = ret.substring(0, ret.length()-1);
                ret += "] , ";
            }
            ret = ret.substring(0, ret.length()-3);

            return "(" + this._id + ": " + ret + ")";
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

//#region Convert All Blocks Into Integers

    private HashSet<BlockState> blockStates = new HashSet<>();
    private HashMap<BlockState, Integer> blockStateToID = new HashMap<>();
    private HashMap<Integer, BlockState> idToBlockState = new HashMap<>();
    private int nextID = 0;
    public void indexBlockStates(BlockPos startPos, BlockPos endPos){
        for (int x = startPos.getX(); x <= endPos.getX(); x++) {
            for (int y = startPos.getY(); y <= endPos.getY(); y++) {
                for (int z = startPos.getZ(); z <= endPos.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);

                    if (!blockStates.contains(state)) {
                        blockStates.add(state);
                        blockStateToID.put(state, nextID);
                        idToBlockState.put(nextID, state);
                        nextID++;
                    }
                }
            }
        }
    }

    public int getBlockStateID(BlockState state) {
        if (blockStateToID.containsKey(state)) {
            return blockStateToID.get(state);
        } else {
            return -1;
        }
    }

    public BlockState getBlockState(int id) {
        if (idToBlockState.containsKey(id)) {
            return idToBlockState.get(id);
        } else {
            return null;
        }
    }
//#endregion

//#region Convert the input area into an integer grid
    public int[][][] convertBlockStatesToIntegers(BlockPos startPos, BlockPos endPos) {

        int[][][] grid = new int[endPos.getX() - startPos.getX() + 1][endPos.getY() - startPos.getY() + 1][endPos.getZ() - startPos.getZ() + 1];

        for (int x = startPos.getX(); x <= endPos.getX(); x++) {
            for (int y = startPos.getY(); y <= endPos.getY(); y++) {
                for (int z = startPos.getZ(); z <= endPos.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    int stateID = getBlockStateID(state);
                    grid[x - startPos.getX()][y - startPos.getY()][z - startPos.getZ()] = stateID;
                }
            }
        }
        return grid;
    }

//#endregion

//#region Generate Chunks of size chunkSize, adding them to a hashset and then 
    public static int chunkSize = 1;
    public int chunkId = 0;
    public HashMap<WFCChunk, Integer> chunksToID = new HashMap<WFCChunk, Integer>();
    public HashMap<Integer, WFCChunk> idToChunks = new HashMap<Integer, WFCChunk>();
    public int[][][] convertIntegersToChunks(int[][][] grid) {

        int gridX = grid.length + 1 - chunkSize;
        int gridY = grid[0].length + 1 - chunkSize;
        int gridZ = grid[0][0].length + 1 - chunkSize;
    
        // Create a new grid of chunks
        int[][][] chunks = new int[gridX][gridY][gridZ];
    
        // Fill the new grid with chunks created from the integer values
        for (int i = 0; i < gridX; i++) {
            for (int j = 0; j < gridY; j++) {
                for (int k = 0; k < gridZ; k++) {
    
                    Integer[][][] chunkBlockValues = new Integer[chunkSize][chunkSize][chunkSize];
    
                    for (int x = 0; x < chunkSize; x++) {
                        for (int y = 0; y < chunkSize; y++) {
                            for (int z = 0; z < chunkSize; z++) {
                                chunkBlockValues[x][y][z] = grid[i+x][j+y][k+z];
                            }
                        }
                    }
    
                    WFCChunk chunk = new WFCChunk(-1, chunkBlockValues);

                    //If the chunk doesn't exist, then put them in the hashmaps. Otherwise, get the chunk and update it's count by 1
                    int flag = checkChunkSeenBefore(chunk);
                    if(flag == -2){
                        chunk._id  = chunkId;
                        chunksToID.put(chunk, chunkId);
                        idToChunks.put(chunkId, chunk);
                        chunks[i][j][k] = chunkId;
                        chunkId++;
                    } else {
                        try {
                            chunk = idToChunks.get(flag);
                            chunk._count++;
                            chunks[i][j][k] = chunk._id;
                        } catch(Exception e){
                            System.out.println(e);
                        }
                    }
                }
            }
        }
        return chunks;
    }
//#endregion

//#region Stage 1 Driver

public int stage1(){
    maximizeCoordinates();
    
    resetBasicDataStructures();
    indexBlockStates(pos1, pos2);

    int[][][] grid = convertBlockStatesToIntegers(pos1, pos2);
    
    int[][][] chunkGrid = convertIntegersToChunks(grid);
    System.out.println(grid[0][1][0] + " " + grid[1][1][0]);
    System.out.println(grid[0][0][0] + " " + grid[1][0][0]);
    
    return 1;
}

//#endregion

//#region Utility - Stage 1

private Integer checkChunkSeenBefore(WFCChunk inc){
    Set<WFCChunk> setOfChunks = chunksToID.keySet();
    Iterator<WFCChunk> itr = setOfChunks.iterator();

    while(itr.hasNext()){
        WFCChunk curr = itr.next();
        boolean matches = inc.equals(curr);

        //If we get through the entire Vector and don't find anything different, then we have found a match
        if(matches){
            return chunksToID.get(curr);
        }

    }
    return -2;
}

public void resetBasicDataStructures(){
    chunkId = 0;
    chunksToID = new HashMap<WFCChunk, Integer>();
    idToChunks = new HashMap<Integer, WFCChunk>();

    blockStates = new HashSet<>();
    blockStateToID = new HashMap<>();
    idToBlockState = new HashMap<>();
    nextID = 0;
}

public void maximizeCoordinates(){
    float xMax = Math.max(pos1.getX(), pos2.getX());
    float yMax = Math.max(pos1.getY(), pos2.getY());
    float zMax = Math.max(pos1.getZ(), pos2.getZ());
    float xMin = Math.min(pos1.getX(), pos2.getX());
    float yMin = Math.min(pos1.getY(), pos2.getY());
    float zMin = Math.min(pos1.getZ(), pos2.getZ());

    BlockPos maxVector = new BlockPos(xMax, yMax, zMax);
    BlockPos minVector = new BlockPos(xMin, yMin, zMin);
    pos1 = minVector;
    pos2 = maxVector;
}

public static void print3DIntArray(int[][][] arr) {
    for (int x = 0; x < arr.length; x++) {
        System.out.print("[");
        for (int y = 0; y < arr[x].length; y++) {
            System.out.print("[");
            for (int z = 0; z < arr[x][y].length; z++) {
                System.out.print(arr[x][y][z]);
                if (z < arr[x][y].length - 1) {
                    System.out.print(", ");
                }
            }
            System.out.print("]");
            if (y < arr[x].length - 1) {
                System.out.print(", ");
            }
        }
        System.out.print("]\n");
    }
}


//#endregion

}





