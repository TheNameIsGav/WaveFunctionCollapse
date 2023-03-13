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
    public MinecraftClient mc;

    private World world;
    public void World(World w) {world = w; }
    public World World() {return world;}

    private BlockPos pos1; //Load position 1
    public void Pos1(BlockPos p){ pos1 = p; }
    public BlockPos Pos1(){ return pos1; }

    private BlockPos pos2; //Load position 2
    public void Pos2(BlockPos p){ pos2 = p; }
    public BlockPos Pos2(){ return pos2; }

    private BlockPos runPos1 = new BlockPos(105, 106, 105); //Run position 1
    public void Run1(BlockPos p) { runPos1 = p; }
    public BlockPos Run1() {return runPos1; }

    private BlockPos runPos2 = new BlockPos(106, 105, 105); //Run position 2
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

    public class RandomCollection<E> {
        private final NavigableMap<Double, E> map = new TreeMap<Double, E>();
        private final Random random;
        private double total = 0;
    
        public RandomCollection() {
            this(new Random());
        }
    
        public RandomCollection(Random random) {
            this.random = random;
        }
    
        public RandomCollection<E> add(double weight, E result) {
            if (weight <= 0) return this;
            total += weight;
            map.put(total, result);
            return this;
        }
    
        public E next() {
            double value = random.nextDouble() * total;
            return map.higherEntry(value).getValue();
        }
    }

    public class PQueuePosition{
        public int x;
        public int y;
        public int z;
        public int size;
        public BlockPos pos;

        public PQueuePosition(int i, int j, int k, int tmp){
            x = i;
            y = j;
            z = k;
            size = tmp;
            pos = new BlockPos(x, y, z);
        }

        public int Size(){
            return size;
        }

        @Override
        public boolean equals(Object o) {
 
            // If the object is compared with itself then return true 
            if (o == this) {
                return true;
            }

            /* Check if o is an instance of Complex or not
              "null instanceof [type]" also returns false */
            if (!(o instanceof PQueuePosition)) {
            return false;
            }
            
            // typecast o to Complex so that we can compare data members
            PQueuePosition p = (PQueuePosition) o;

            return (x == p.x) && (y == p.y) && (z == p.z);
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
    public int chunkSize = 2;
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
                        chunk._count = 1;
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

//#region Base Driver

//Read all the information into the grid's and create chunks
int maxRuns = 1000;

public int stage0(){
    maxRuns = 1000;
    return stage1();
}

public int stage1(){
    maximizeCoordinatesInput();
    
    resetBasicDataStructures();
    indexBlockStates(pos1, pos2);

    int[][][] grid = convertBlockStatesToIntegers(pos1, pos2);
    
    int[][][] chunkGrid = convertIntegersToChunks(grid);

    HashMap<Integer, List<HashSet<Integer>>> adj = stage2(chunkGrid);
    HashSet<Integer>[][][] outputGrid = stage3(chunkGrid);
    int[][][] intOutputGrid = stage4(outputGrid, adj);

    if(intOutputGrid != null) {stageX(intOutputGrid); return 1;}
    else  {
        System.out.println(maxRuns);
        if(maxRuns < 0){
            mc.inGameHud.addChatMessage(MessageType.SYSTEM, new LiteralText("Max Runs reached"), mc.player.getUuid());
            return 2;
        }
        mc.inGameHud.addChatMessage(MessageType.SYSTEM, new LiteralText("Attempting to rerun WFC, got bad result"), mc.player.getUuid());
        maxRuns--;
        
        return stage1();
    }
}

//Setup all the adjacencies for the chunks
public HashMap<Integer, List<HashSet<Integer>>> stage2(int[][][] chunkGrid){
    int width = chunkGrid.length - 1;
    int height = chunkGrid[0].length - 1;
    int depth = chunkGrid[0][0].length - 1;

    HashMap<Integer, List<HashSet<Integer>>> adjacencies = new HashMap<>();
    try {

        //region Edge Setup
            adjacencies.put(-1, 
            new ArrayList<HashSet<Integer>>(){
                {
                    add(new HashSet<Integer>());
                    add(new HashSet<Integer>());
                    add(new HashSet<Integer>());
                    add(new HashSet<Integer>());
                    add(new HashSet<Integer>());
                    add(new HashSet<Integer>());
                }
            });
            HashSet<Integer> edgeRight = adjacencies.get(-1).get(3);
            HashSet<Integer> edgeLeft = adjacencies.get(-1).get(2);
            HashSet<Integer> edgeUp = adjacencies.get(-1).get(0);
            HashSet<Integer> edgeDown = adjacencies.get(-1).get(1);
            HashSet<Integer> edgeForward = adjacencies.get(-1).get(4);
            HashSet<Integer> edgeBack = adjacencies.get(-1).get(5);
        //#endregion

        //region Creating Adjacencies
        for (int x = 0; x <= width; x++) {
            for (int y = 0; y <= height; y++) {
                for (int z = 0; z <= depth; z++) {
                    
                    //If the value doesn't exist yet then add it
                    int val = chunkGrid[x][y][z];
                    if(!adjacencies.keySet().contains(val)){
                        adjacencies.put(val, 
                            new ArrayList<HashSet<Integer>>(){
                                {
                                    add(new HashSet<Integer>());
                                    add(new HashSet<Integer>());
                                    add(new HashSet<Integer>());
                                    add(new HashSet<Integer>());
                                    add(new HashSet<Integer>());
                                    add(new HashSet<Integer>());
                                }
                        });
                    }

                    HashSet<Integer> right = adjacencies.get(val).get(3);
                    int px = x + 1;
                    HashSet<Integer> left = adjacencies.get(val).get(2);
                    int nx = x - 1;

                    HashSet<Integer> up = adjacencies.get(val).get(0);
                    int py = y + 1;
                    HashSet<Integer> down = adjacencies.get(val).get(1);
                    int ny = y - 1;

                    HashSet<Integer> forward = adjacencies.get(val).get(4);
                    int pz = z + 1;
                    HashSet<Integer> back = adjacencies.get(val).get(5);
                    int nz = z - 1;

                    if (nx >= 0) {
                        left.add(chunkGrid[nx][y][z]);
                    } else {
                        left.add(-1);
                        edgeRight.add(val);
                    }

                    if (px <= width) {
                        right.add(chunkGrid[px][y][z]);
                    } else {
                        right.add(-1);
                        edgeLeft.add(val);
                    }

                    if (ny >= 0) {
                        down.add(chunkGrid[x][ny][z]);
                    } else {
                        down.add(-1);
                        edgeUp.add(val);
                    }

                    if (py <= height) {
                        up.add(chunkGrid[x][py][z]);
                    } else {
                        up.add(-1);
                        edgeDown.add(val);
                    }
                    
                    if (nz >= 0) {
                        back.add(chunkGrid[x][y][nz]);
                    } else {
                        back.add(-1);
                        edgeForward.add(val);
                    }

                    if (pz <= depth) {
                        forward.add(chunkGrid[x][y][pz]);
                    } else {
                        forward.add(-1);
                        edgeBack.add(val);
                    }
                }
            }
        }

        //#endregion


    } catch (Exception e){
        System.out.println(e);
    }

    return adjacencies;
}

//Setup output grid with chunks
public HashSet<Integer>[][][] stage3(int[][][] chunkGrid){
    try{
        maximizeCoordinatesOutput();

        BlockPos startPos = runPos1;
        BlockPos endPos = runPos2;

        int width = endPos.getX() - startPos.getX() + 1 - chunkSize;
        int height = endPos.getY() - startPos.getY() + 1 - chunkSize;
        int depth = endPos.getZ() - startPos.getZ() + 1 - chunkSize;

        HashSet<Integer>[][][] outputGrid = new HashSet[width+1][height+1][depth+1];

        for (int x = 0; x <= width; x++) {
            for (int y = 0; y <= height; y++) {
                for (int z = 0; z <= depth; z++) {
                    HashSet<Integer> possibilities = new HashSet<Integer>(blockStateToID.values());
                    outputGrid[x][y][z] = new HashSet<Integer>(possibilities);
                }
            }
        }

        return outputGrid;
    } catch (Exception e){
        System.out.println(e.toString());
        return null;
    }
}

//Collapse the output grid and then turn it into a grid of integers
public int[][][] stage4(HashSet<Integer>[][][] outputGrid, HashMap<Integer, List<HashSet<Integer>>> adj){
    try {
        int width = outputGrid.length - 1;
        int height = outputGrid[0].length - 1;
        int depth = outputGrid[0][0].length - 1;

        HashSet<Integer> edgeRight = adj.get(-1).get(3);
        HashSet<Integer> edgeLeft = adj.get(-1).get(2);
        HashSet<Integer> edgeUp = adj.get(-1).get(0);
        HashSet<Integer> edgeDown = adj.get(-1).get(1);
        HashSet<Integer> edgeForward = adj.get(-1).get(4);
        HashSet<Integer> edgeBack = adj.get(-1).get(5);

        PriorityQueue<PQueuePosition> toCollapseQueue = new PriorityQueue<PQueuePosition>(1, Comparator.comparingInt(PQueuePosition::Size));
        HashSet<BlockPos> collapsedSet = new HashSet<BlockPos>(); 

        //region Collapse Edges of Output Grid
            //Bottom Face
            for(int x = 0; x <= width; x++){
                for(int z = 0; z <= depth; z++){
                    outputGrid[x][0][z].retainAll(edgeUp);
                    toCollapseQueue.add(new PQueuePosition(x, 0, z, outputGrid[x][0][z].size()));
                }
            }

            //Top Face
            for(int x = 0; x <= width; x++){
                for(int z = 0; z <= depth; z++){
                    outputGrid[x][height][z].retainAll(edgeDown);
                    toCollapseQueue.add(new PQueuePosition(x, height, z, outputGrid[x][height][z].size()));
                }
            }

            //Left Face
            for(int y = 0; y <= height; y++){
                for(int z = 0; z <= depth; z++){
                    outputGrid[0][y][z].retainAll(edgeRight);
                    toCollapseQueue.add(new PQueuePosition(0, y, z, outputGrid[0][y][z].size()));
                }
            }

            //Right Face
            for(int y = 0; y <= height; y++){
                for(int z = 0; z <= depth; z++){
                    outputGrid[width][y][z].retainAll(edgeLeft);
                    toCollapseQueue.add(new PQueuePosition(width, y, z, outputGrid[width][y][z].size()));
                }
            }

            //Back Face
            for(int x = 0; x <= width; x++){
                for(int y = 0; y <= height; y++){
                    outputGrid[x][y][0].retainAll(edgeForward);
                    toCollapseQueue.add(new PQueuePosition(x, y, 0, outputGrid[x][y][0].size()));
                }
            }

            //Front Face
            for(int x = 0; x <= width; x++){
                for(int y = 0; y <= height; y++){
                    outputGrid[x][y][depth].retainAll(edgeBack);
                    toCollapseQueue.add(new PQueuePosition(x, y, depth, outputGrid[x][y][depth].size()));
                }
            }
        //endregion 
        toCollapseQueue = filterQueue(toCollapseQueue);

        //region Begin Collapse
            while(!toCollapseQueue.isEmpty()){

                System.out.println(collapsedSet.size() + " " + toCollapseQueue.size());

                PQueuePosition current = toCollapseQueue.poll();
                BlockPos currentPos = current.pos;
                HashSet<Integer> currentVal = outputGrid[current.x][current.y][current.z];
                if(currentVal.size() == 0) throw new IndexOutOfBoundsException("Current Value was nothing");

                List<Integer> weighCollection = new ArrayList<Integer>();
                
                for(int id : currentVal){
                    for(int r = 0; r < idToChunks.get(id)._count; r++){
                        weighCollection.add(id);
                    }
                }

                Random rand = new Random();
                int tmp = weighCollection.get(rand.nextInt(weighCollection.size()));;
                currentVal = new HashSet<Integer>();
                currentVal.add(tmp);
                outputGrid[current.x][current.y][current.z] = currentVal;

                collapsedSet.add(currentPos);

                //region Update Adjacent Squares
                    int x = currentPos.getX();
                    int y = currentPos.getY();
                    int z = currentPos.getZ();

                    HashSet<Integer> right = adj.get(tmp).get(3);
                    int px = x + 1;
                    HashSet<Integer> left = adj.get(tmp).get(2);
                    int nx = x - 1;
                    HashSet<Integer> up = adj.get(tmp).get(0);
                    int py = y + 1;
                    HashSet<Integer> down = adj.get(tmp).get(1);
                    int ny = y - 1;
                    HashSet<Integer> forward = adj.get(tmp).get(4);
                    int pz = z + 1;
                    HashSet<Integer> back = adj.get(tmp).get(5);
                    int nz = z - 1;

                    if (nx >= 0) {
                        PQueuePosition p = new PQueuePosition(nx, y, z, outputGrid[nx][y][z].size());
                        if(!collapsedSet.contains(p.pos)) {
                            outputGrid[nx][y][z].retainAll(left);
                            toCollapseQueue.add(p);
                        }
                    }

                    if (px <= width) {
                        PQueuePosition p = new PQueuePosition(px, y, z, outputGrid[px][y][z].size());
                        if(!collapsedSet.contains(p.pos)) {
                            outputGrid[px][y][z].retainAll(right);
                            toCollapseQueue.add(p);
                        }
                    }

                    if (ny >= 0) {
                        PQueuePosition p = new PQueuePosition(x, ny, z, outputGrid[x][ny][z].size());
                        if(!collapsedSet.contains(p.pos)){
                            outputGrid[x][ny][z].retainAll(down);
                            toCollapseQueue.add(p);
                        }
                    }

                    if (py <= height) {
                        PQueuePosition p = new PQueuePosition(x, py, z, outputGrid[x][py][z].size());
                        if(!collapsedSet.contains(p.pos)){
                            outputGrid[x][py][z].retainAll(up);
                            toCollapseQueue.add(p);
                        }
                    }
                    
                    if (nz >= 0) {
                        PQueuePosition p = new PQueuePosition(x, y, nz, outputGrid[x][y][nz].size());
                        if(!collapsedSet.contains(p.pos)){
                            outputGrid[x][y][nz].retainAll(back);
                            toCollapseQueue.add(p);
                        }
                    }

                    if (pz <= depth) {
                        PQueuePosition p = new PQueuePosition(x, y, pz, outputGrid[x][y][pz].size());
                        if(!collapsedSet.contains(p.pos)) {
                            outputGrid[x][y][pz].retainAll(forward);
                            toCollapseQueue.add(p);
                        }
                    }
                //endregion

                toCollapseQueue = filterQueue(toCollapseQueue);

            }

        //endregion

        //region Convert outputGrid to integer grid
            int[][][] finalOutput = new int[width+1][height+1][depth+1];
            for(int x = 0; x <= width; x++){
                for(int y = 0; y <= height; y++){
                    for(int z = 0; z <= depth; z++){
                        if(outputGrid[x][y][z].size() != 1){
                            throw new IndexOutOfBoundsException("Collapsed position not valid");
                        } else {
                            Object[] tmp = outputGrid[x][y][z].toArray();
                            Integer tmp2 = (Integer) tmp[0];
                            if(tmp2 == -1) throw new IllegalArgumentException("Cannnot generate -1");
                            finalOutput[x][y][z] = tmp2.intValue();
                        }                        
                    }
                }
            }
        //endregion

        return finalOutput;
    } catch (Exception e){
        System.out.println(e.toString());
        return null;
    }
    
}

//Generate the world from the chunks
public int stageX(int[][][] outputGrid){
    
    int width = outputGrid.length - (chunkSize - 1);
    int height = outputGrid[0].length - (chunkSize - 1);
    int depth = outputGrid[0][0].length - (chunkSize - 1);

    BlockPos startCoords = new BlockPos(105, 105, 105);

    //Goes from Minimal Coordinates to Maxmial Coordaintes
    for(int x = 0; x <= width; x++){
        for(int y = 0; y <= height; y++){
            for(int z = 0; z <= depth; z++){

                try {

                    if(outputGrid[x][y][z] == -1){
                        throw new IndexOutOfBoundsException("Invalid edge");
                    }

                    GenerateChunk(idToChunks.get(outputGrid[x][y][z]), 
                                new BlockPos(startCoords.getX() + x, startCoords.getY() + y, startCoords.getZ() + z));
                } catch (Exception e) {
                    System.out.println(e.toString());
                    return -1;
                }

            }
        }
    }
    return 1;
}

//#endregion

//#region Utility

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

public void maximizeCoordinatesInput(){
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

public void maximizeCoordinatesOutput(){
    float xMax = Math.max(runPos1.getX(), runPos2.getX());
    float yMax = Math.max(runPos1.getY(), runPos2.getY());
    float zMax = Math.max(runPos1.getZ(), runPos2.getZ());
    float xMin = Math.min(runPos1.getX(), runPos2.getX());
    float yMin = Math.min(runPos1.getY(), runPos2.getY());
    float zMin = Math.min(runPos1.getZ(), runPos2.getZ());

    BlockPos maxVector = new BlockPos(xMax, yMax, zMax);
    BlockPos minVector = new BlockPos(xMin, yMin, zMin);
    runPos1 = minVector;
    runPos2 = maxVector;
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

public void GenerateChunk(WFCChunk chunk, BlockPos startCoor){
    
    for(int x = 0; x < chunkSize; x++){
        for(int y = 0; y < chunkSize; y++){
            for(int z = 0; z < chunkSize; z++){
                BlockPos coor = new BlockPos(startCoor.getX()  + x, startCoor.getY() + y, startCoor.getZ() + z);
                world.setBlockState(coor, idToBlockState.get(chunk._chunkBlockValues[x][y][z]));
            }
        }
    }
}

public PriorityQueue<PQueuePosition> filterQueue(PriorityQueue<PQueuePosition> pQ){

    PriorityQueue<PQueuePosition> newQ = new PriorityQueue<PQueuePosition>(1, Comparator.comparingInt(PQueuePosition::Size));

    while(!pQ.isEmpty()){
        if(!newQ.contains(pQ.peek())){
            newQ.add(pQ.poll());
        } else {
            pQ.poll();
        }
    }

    return newQ;
}
//#endregion

}