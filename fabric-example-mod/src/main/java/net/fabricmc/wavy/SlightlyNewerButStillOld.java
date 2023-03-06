package net.fabricmc.wavy;

/* 
   
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
        // figure out how to add edge adjacencies
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
        //figure out how to load the edge adjacencies
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
              "null instanceof [type]" also returns false 
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

    void Print3DArray(Object[][][] array){
        // String ret = "";

        // for(int x = 0; x < array.length; x++){
        //     for(int y = 0; y < array[x].length; y++){
        //         for(int z = 0; z < array[x][y].length; z++){
        //             ret += array[x][y][z].toString() + ",";
        //         }
        //         ret = ret.substring(0, ret.length()-1);
        //         ret += "],";
        //     }
        //     ret = ret.substring(0, ret.length()-1);
        //     ret += "] , ";
        // }
        // ret = ret.substring(0, ret.length()-3);

        // System.out.println(ret);

        int xSize = array.length;
        int ySize = array[0].length;
        int zSize = array[0][0].length;
        
        // Print array
        for (int x = 0; x < xSize; x++) {
            System.out.println("x = " + x);
            for (int y = 0; y < ySize; y++) {
                for (int z = 0; z < zSize; z++) {
                    System.out.print(array[x][y][z]);
                    if (z < zSize - 1) {
                        System.out.print(z + ", ");
                    }
                }
                System.out.println();
            }
            System.out.println();
        }

    }

    void Print3DArray(String label, Object[][][] array){
        String ret = "";

        for(int x = 0; x < array.length; x++){
            ret += "[";
            for(int y = 0; y < array[x].length; y++){
                ret += "[";
                for(int z = 0; z < array[x][y].length; z++){
                    ret += array[x][y][z].toString() + ",";
                }
                ret = ret.substring(0, ret.length()-1);
                ret += "],";
            }
            ret = ret.substring(0, ret.length()-1);
            ret += "] , ";
        }
        ret = ret.substring(0, ret.length()-3);

        System.out.println(label + ret);
    }

    void Print3DVector(Vec3d vec){
        int x = (int)vec.x;
        int y = (int)vec.y;
        int z = (int)vec.z;

        System.out.println("X:" + x + " Y:" + y + " Z:" + z);
    }

    void Print3DVector(String label, Vec3d vec){
        int x = (int)vec.x;
        int y = (int)vec.y;
        int z = (int)vec.z;

        System.out.println(label + " X:" + x + " Y:" + y + " Z:" + z);
    }

    void Print3DVector(String label, int x, int y, int z){
        System.out.println(label + " X:" + x + " Y:" + y + " Z:" + z);
    }

    void Print3DVector(int x, int y, int z){
        System.out.println(" X:" + x + " Y:" + y + " Z:" + z);
    }

    void PrintArray(Object[] array){
        String ret = "";
        for(int x = 0; x < array.length; x++){
            ret += array[x].toString() + ",";
        }
        System.out.println("[" + ret.substring(0, ret.length()-1) + "]");
    }

    void PrintArray(String label, Object[] array){
        String ret = "";
        for(int x = 0; x < array.length; x++){
            ret += array[x].toString() + ",";
        }
        System.out.println(label + " [" + ret.substring(0, ret.length()-1) + "]");
    }

    Integer[] convertObjToInt(Object[] array){
        Integer[] ret = new Integer[array.length];
        for(int i = 0; i < array.length; i++){
            Object r = array[i];
            if(r instanceof Integer){
                int k = (Integer)r;
                ret[i] = k;
            }
        }
        return ret;
    }

    Integer[] deepCopyPseudo(Integer[] array){
        Integer[] ret = new Integer[array.length];
        for(int i = 0; i < array.length; i++){
            int x = array[i];
            ret[i] = x;
        }

        return ret;
    }

    void Print4DArray(Object[][][][] array){
        String ret = "";

        for(int x = 0; x < array.length; x++){
            ret += "[";
            for(int y = 0; y < array[x].length; y++){
                ret += "[";
                for(int z = 0; z < array[x][y].length; z++){
                    ret += "[";
                    for(int i = 0; i < array[x][y][z].length; i++){
                        ret += array[x][y][z][i] + ",";
                    }
                    ret = ret.substring(0, ret.length()-1);    
                    ret += "],";
                }
                ret = ret.substring(0, ret.length()-1);
                ret += "],";
            }
            ret = ret.substring(0, ret.length()-1);
            ret += "] , ";
        }
        ret = ret.substring(0, ret.length()-3);

        System.out.println(ret);
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

        //pos1 = new BlockPos(110, 7, 104);
        //pos2 = new BlockPos(122, 23, 92);

        // pos1 = new BlockPos(116, 18, 93);
        // pos2 = new BlockPos(111, 16, 98);

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

        
        //Goes through the integers inside of the inputIntegerGrid and creates associations for them
        createIntegerGridAssocs();

        //System.out.println(adj);

        //Read in Input Data with chunk size
        //int j = buildAdjacenciesChunks();
        hasRunFirstStep = true;
        //Print3DArray(inputIntegersGrid);
        //System.out.println(integerToChunkMap);
        //System.out.println(seenIntToBlock);
        System.out.println("Adjs: " + adj);

        return 1;
    }

    BlockPos min;
    BlockPos max;

    BlockPos runMin;
    BlockPos runMax;

    //Grid of the input as integers
    Integer[][][] inputIntegersGrid;
    static int chunkSize = 1;

    //Iterates through the entire input sample and generates chunk of specified chunkSize starting from the maximal coordinate
    private void createChunks() {

        int xLength = max.getX() - min.getX() - chunkSize + 2;
        int yLength = max.getY() - min.getY() - chunkSize + 2;
        int zLength = max.getZ() - min.getZ() - chunkSize + 2;

        inputIntegersGrid = new Integer[xLength][yLength][zLength];
        //Go from max to (min + chunkSize)
        //Each block position is treated as 'maximal' position of the chunk
        //Read in the values for that specific block and make sure that we don't already have it, then add it
        //Read that as an integer, and then put that integer into the (x, y, z) coordinates
        for(int x = 0; x < xLength; x++){
            for(int y = 0; y < yLength; y++){
                for(int z = 0; z < zLength; z++){
                    WFCChunk ret = readChunkRevamped(max.getX() - x, max.getY() - y, max.getZ() - z);//addChunk(x, y, z, true); //Validated with single chunk size
                    inputIntegersGrid[xLength-x-1][yLength-y-1][zLength-z-1] = ret._id;
                }
            }
        }
    }

    //Simple way to get some information and test chunk orientation
    public void ReadChunk(){
        System.out.println("1");
        copyBlocksToRight(new BlockPos(100, 100, 100), new BlockPos(101, 101, 100));
    }

    public void copyBlocksToRight(BlockPos startPos, BlockPos endPos) {
        // Calculate dimensions of the area to copy
        int width = endPos.getX() - startPos.getX() + 1;
        int height = endPos.getY() - startPos.getY() + 1;
        int depth = endPos.getZ() - startPos.getZ() + 1;
        System.out.println("2");
        // Initialize 3D array to store block data
        BlockState[][][] blocks = new BlockState[width][height][depth];
        
        // Read block data from world into array
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    BlockPos pos = new BlockPos(startPos.getX() + x, startPos.getY() + y, startPos.getZ() + z);
                    blocks[x][y][z] = world.getBlockState(pos);
                }
            }
        }
        System.out.println("3");
        // Generate new blocks to the right
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    BlockPos pos = new BlockPos(startPos.getX() + x + 5, startPos.getY() + y, startPos.getZ() + z);
                    world.setBlockState(pos, blocks[x][y][z]);
                }
            }
        }
        System.out.println("4");
    }

    //Reads in a chunk from maximal coordiante (x, y, z)
    private WFCChunk readChunkRevamped(int x, int y, int z) {
        WFCChunk chunk = new WFCChunk();

        //Read in the chunk
        for(int q = 0; q < chunkSize; q++){
            for(int w = 0; w < chunkSize; w++){
                for(int r = 0; r < chunkSize; r++){
                    BlockPos bp = new BlockPos(x - q, y - w, z - r);
                    chunk._chunkBlockValues[chunkSize-q-1][w][chunkSize-r-1] = seenBlocksToInt.get(world.getBlockState(bp));
                }
            }
        }

        //Determine if it's a new chunk, if it is add it to the places, if not increase it's amt
        int flag = checkChunkSeenBefore(chunk);
        if(flag == -2){ //This is a new chunk
            chunk._id = currentIndex;
            currentIndex++;
            listOfSeenChunks.put(chunk._id, 1);
            integerToChunkMap.put(chunk._id, chunk);
            chunkToIntegerMap.put(chunk, chunk._id);
            ////System.out.println("New Ret: " + ret);
        } else { //This is a seen chunk, so increase amt by 1
            chunk = integerToChunkMap.get(flag);
            listOfSeenChunks.put(flag, listOfSeenChunks.get(flag) + 1);
        }

        return chunk;
    }

    /*
    Up - 0 (+), Down - 1 (-) - y direction
    West - 2 (-), East - 3 (+) - x direction
    North - 4 (-), South - 5 (+) - z direction
    
    //Generates adjacencies for integers within the inputIntegersGrid
    private void createIntegerGridAssocs() {
        
        adj.put(-1, generateNew2DVector());

        for(int x = inputIntegersGrid.length-1; x >=0 ; x--){
            for(int y = inputIntegersGrid[0].length-1; y >= 0 ; y--){
                for(int z = inputIntegersGrid[0][0].length-1; z >= 0 ; z--){
                    //System.out.println("X: " + x + " Y: " + y + " Z: " + z);
                    int id = inputIntegersGrid[x][y][z];
                    if(adj.get(id) == null){
                        adj.put(id, generateNew2DVector());
                    }

                    //Up and Down
                    Vec3d up = new Vec3d(x, y + 1, z);
                    integerGridAssocHelper(up, 0, id);

                    Vec3d down = new Vec3d(x, y - 1, z);
                    integerGridAssocHelper(down, 1, id);

                    //East and West
                    Vec3d east = new Vec3d(x + 1, y, z);
                    integerGridAssocHelper(east, 3, id);
                    
                    Vec3d west = new Vec3d(x - 1, y, z);
                    integerGridAssocHelper(west, 2, id);

                    //North and South
                    Vec3d north = new Vec3d(x, y, z - 1);
                    integerGridAssocHelper(north, 4, id);

                    Vec3d south = new Vec3d(x, y, z + 1);
                    integerGridAssocHelper(south, 5, id);
                }
            }
        }
        //adj.get(-1).get(0).add(0);
    }

    //Associates the integer at position curr with the adjacencies for the integer id within the adjacency matrix
    private void integerGridAssocHelper(Vec3d coor, int direction, int id) {

        //Print3DVector("Curr Coordinate: ", coor);

        if(withinRange(coor)){
            //Put the value at the coordinate of up into the adjacencies of the int
            Vector<Integer> originalAdjs = adj.get(id).get(direction);
            int target = inputIntegersGrid[(int)coor.x][(int)coor.y][(int)coor.z];

            if(!originalAdjs.contains(target)){
                originalAdjs.add(target);
                adj.get(id).set(direction, originalAdjs);
            }
        } else { 
            //System.out.println("Coordinate is outside of range");
            //Put -1 into the adjacencies of the int, and put this id into -1
            Vector<Integer> originalAdjs = adj.get(id).get(direction);
            int target = -1;

            if(!originalAdjs.contains(target)){
                originalAdjs.add(target);
                adj.get(id).set(direction, originalAdjs);
            }

            int x = 0;
            switch(direction){
                case 0:
                    x = 1;
                    break;
                case 1:
                    x = 0;
                    break;
                case 2:
                    x = 3;
                    break;
                case 3:
                    x = 2;
                    break;
                case 4:
                    x = 5;
                    break;
                case 5:
                    x = 4;
                    break;
            }

            Vector<Integer> edgeAdjs = adj.get(-1).get(x);
            if(!edgeAdjs.contains(id)){
                edgeAdjs.add(id);
                adj.get(-1).set(x, edgeAdjs);
            }
        }
    }

    //Determines if an x, y, z truple are within the bounds of the inputIntegersGrid
    private boolean withinRange(Vec3d coor) {
        int x = (int)coor.x;
        int y = (int)coor.y;
        int z = (int)coor.z;

        if(x < 0 || y < 0 || z < 0){
            return false;
        } else if (x > inputIntegersGrid.length-1 || y > inputIntegersGrid[0].length-1 || z > inputIntegersGrid[0][0].length-1) {
            return false;
        } else {
            return true;
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

        System.out.println("Inside validate all blocks: " + seenBlocksToInt);
    }

    //Given a starting coordinate, returns the blocks inside that chunk

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

    private Vector<Vector<Integer>> generateNew2DVector(){
        Vector<Vector<Integer>> a = new Vector<Vector<Integer>>();
        for(int i = 0; i < 6; i++){
            Vector<Integer> b = new Vector<Integer>();
            a.add(b);
        }

        return a;
    }

    /*




























    
    HashSet<Vec3d> collapsedOutputIntegerCoordinates;

    //X, Y, Z, list of possibilities
    Integer[][][][] outputInteger;
    public int secondStepRevamped() {

        //runPos1 = new BlockPos(102, 6, 92);
        //runPos2 = new BlockPos(85, 22, 112);

        // runPos1 = new BlockPos(116, 28, 93);
        // runPos2 = new BlockPos(111, 26, 98);

        //runPos1 = new BlockPos(100, 5, 100);
        //runPos2 = new BlockPos(105, 10, 105);

        //102 - 85, 22-6, 112-92

        //Decrease the size by the chunk amount to convert to integers
        runMin = new BlockPos((runPos1.getX() < runPos2.getX() ? runPos1.getX() : runPos2.getX())
                             ,(runPos1.getY() < runPos2.getY() ? runPos1.getY() : runPos2.getY())
                             ,(runPos1.getZ() < runPos2.getZ() ? runPos1.getZ() : runPos2.getZ())
        );

        runMax = new BlockPos((runPos1.getX() > runPos2.getX() ? runPos1.getX() : runPos2.getX())
                             ,(runPos1.getY() > runPos2.getY() ? runPos1.getY() : runPos2.getY())
                             ,(runPos1.getZ() > runPos2.getZ() ? runPos1.getZ() : runPos2.getZ())
        );

        int xLength = runMax.getX() - runMin.getX() - chunkSize + 4;
        int yLength = runMax.getY() - runMin.getY() - chunkSize + 4;
        int zLength = runMax.getZ() - runMin.getZ() - chunkSize + 4;

        outputInteger = new Integer[xLength][yLength][zLength][listOfSeenChunks.size()];

        collapsedOutputIntegerCoordinates = new HashSet<Vec3d>();

        //Go through each position in output integers and add all the possibilities for chunks
        for(int x = 0; x < xLength; x++){
            for(int y = 0; y < yLength; y++){
                for(int z = 0; z < zLength; z++){

                    //If we are on an edge, then we make it -1 and collapse it because it has to be an edge
                    if(x == 0 || y == 0 || z == 0 || x == xLength-1 || y == yLength-1 || z == zLength-1){
                        //Print3DVector("Found edge at", x, y, z);
                        outputInteger[x][y][z] = new Integer[]{-1};
                        collapsedOutputIntegerCoordinates.add(new Vec3d(x, y, z));
                    } else {
                        //Print3DVector("Found not edge at" , x, y, z);
                        Integer[] temp = convertObjToInt(listOfSeenChunks.keySet().toArray());
                        outputInteger[x][y][z] = temp;
                    }
                }
            }
        }

        //Go through all of our edges (i.e. every value in collapsed nodes) and reflect their adjcencies inwards
        Object[] edges = collapsedOutputIntegerCoordinates.toArray();
        for (Object oedge : edges) {
            Vec3d edge;
            if(oedge instanceof Vec3d){
                edge = (Vec3d) oedge;
            } else {
                System.out.println("Shouldn't get here");
                break;
            }

            Vec3d up = new Vec3d(edge.x, edge.y+1, edge.z);
            if(withinRangeOutput(up) && !collapsedOutputIntegerCoordinates.contains(up)){
                Vector<Vector<Integer>> picker = adj.get(-1);
                Vector<Integer> possibilities = picker.get(0);
                Integer[] converted = convertObjToInt(possibilities.toArray());
                Integer[] current = outputInteger[(int)up.x][(int)up.y][(int)up.z];
                outputInteger[(int)up.x][(int)up.y][(int)up.z] = intersectIntegerArrays(current, converted);
            }

            Vec3d down = new Vec3d(edge.x, edge.y-1, edge.z);
            if(withinRangeOutput(down) && !collapsedOutputIntegerCoordinates.contains(down)){
                Vector<Vector<Integer>> picker = adj.get(-1);
                Vector<Integer> possibilities = picker.get(1);
                Integer[] converted = convertObjToInt(possibilities.toArray());
                Integer[] current = outputInteger[(int)down.x][(int)down.y][(int)down.z];

                outputInteger[(int)down.x][(int)down.y][(int)down.z] = intersectIntegerArrays(current, converted);
            }

            Vec3d east = new Vec3d(edge.x+1, edge.y, edge.z);
            if(withinRangeOutput(east) && !collapsedOutputIntegerCoordinates.contains(east)){
                Vector<Vector<Integer>> picker = adj.get(-1);
                Vector<Integer> possibilities = picker.get(3);
                Integer[] converted = convertObjToInt(possibilities.toArray());
                Integer[] current = outputInteger[(int)east.x][(int)east.y][(int)east.z];

                outputInteger[(int)east.x][(int)east.y][(int)east.z] = intersectIntegerArrays(current, converted);
            }

            Vec3d west = new Vec3d(edge.x-1, edge.y, edge.z);
            if(withinRangeOutput(west) && !collapsedOutputIntegerCoordinates.contains(west)){
                Vector<Vector<Integer>> picker = adj.get(-1);
                Vector<Integer> possibilities = picker.get(2);
                Integer[] converted = convertObjToInt(possibilities.toArray());
                Integer[] current = outputInteger[(int)west.x][(int)west.y][(int)west.z];

                outputInteger[(int)west.x][(int)west.y][(int)west.z] = intersectIntegerArrays(current, converted);
            }

            Vec3d north  = new Vec3d(edge.x, edge.y, edge.z-1);
            if(withinRangeOutput(north) && !collapsedOutputIntegerCoordinates.contains(north)){
                Vector<Vector<Integer>> picker = adj.get(-1);
                Vector<Integer> possibilities = picker.get(4);
                Integer[] converted = convertObjToInt(possibilities.toArray());
                Integer[] current = outputInteger[(int)north.x][(int)north.y][(int)north.z];

                outputInteger[(int)north.x][(int)north.y][(int)north.z] = intersectIntegerArrays(current, converted);
            }

            Vec3d south = new Vec3d(edge.x, edge.y, edge.z+1);
            if(withinRangeOutput(south) && !collapsedOutputIntegerCoordinates.contains(south)){
                Vector<Vector<Integer>> picker = adj.get(-1);
                Vector<Integer> possibilities = picker.get(5);
                Integer[] converted = convertObjToInt(possibilities.toArray());
                Integer[] current = outputInteger[(int)south.x][(int)south.y][(int)south.z];

                outputInteger[(int)south.x][(int)south.y][(int)south.z] = intersectIntegerArrays(current, converted);
            }

        }
        
        Print4DArray(outputInteger);

        // //Go through the the outputIntegers and find the one with the least entropy
        int itr = 6000;
        boolean done = false;
        
        while(!done && itr > 0){
            //Check if we have collapsed everything we can
            itr--;

            //

            if(collapsedOutputIntegerCoordinates.size() == (xLength * yLength * zLength)) {
                done = true;
                System.out.println("Collapsed every coordinate");
                break;
            }


            //If we are not done, find the least Entropic position within the array
            //Go through all elements and find the min val of the length
            int minVal = Integer.MAX_VALUE;
            Vec3d minVec = new Vec3d(0,0,0);

            for(int x = 1; x < xLength-1; x++) {
                for(int y = 1; y < yLength-1; y++) {
                    for(int z = 1; z < zLength-1; z++) {
                        if(outputInteger[x][y][z].length < minVal && !collapsedOutputIntegerCoordinates.contains(new Vec3d(x, y, z))) {
                            minVal = outputInteger[x][y][z].length;
                            minVec = new Vec3d(x, y, z);
                            if(outputInteger.length == 0){
                                System.out.println("IM SCREAMING");
                                return -2;
                            }
                        }
                    }
                }
            }

            //System.out.println("test1");
            //Check to see if the outputs are 0 at the end of the first iteration
            //Print things to file to help with readability.
            //Print the individual chunks and the block id's to the file
            //Dump everything I can to a file when I get through the iteration

            //Collapse that position
            //Pick on from the possibilites based on their percent chances
            //Get a list of all of the potential chunks this could be

            //Print3DVector("MinVec: ", minVec);
            //System.out.println("Size of output integer is " + outputInteger.length + " by " + outputInteger[0].length + " by " + outputInteger[0][0].length);


            if(outputInteger[(int)minVec.x][(int)minVec.y][(int)minVec.z].length == 0){ //No possibilites
                System.out.println("Crashed");
                return -1;
            }
            
            //System.out.println("Size of output: " + outputInteger[(int)minVec.x][(int)minVec.y][(int)minVec.z].length);
            //PrintArray("Print output: " , outputInteger[(int)minVec.x][(int)minVec.y][(int)minVec.z]);

            Integer[] potential = outputInteger[(int)minVec.x][(int)minVec.y][(int)minVec.z];

            Vector<Integer> pickVec = new Vector<Integer>();

            //For each int inside of potential, add a number of those int's to the pic vec. ex. [1, 1, 1, 1, 1, 2, 2, 3, 3, 3, 4]
            for(int i : potential) {
                for(int j = 0; j < listOfSeenChunks.get(i); j++){
                    pickVec.add(i);
                }
            }

            //System.out.println("test1.1");
            
            int picked = pickVec.get((int) (Math.random() * pickVec.size()));

            // PrintArray("PickVec:", pickVec.toArray());
            // System.out.println("PickVec Size: " + pickVec.size());
            // System.out.println("Picked Index: " + picked);

            //outputInteger[(int)minVec.x][(int)minVec.y][(int)minVec.z] = new Integer[]{pickVec.get(picked)};
            outputInteger[(int)minVec.x][(int)minVec.y][(int)minVec.z] = new Integer[]{picked};

            //System.out.println("test2");

            //Update adjacent spots
            //Check if any adjacencies are empty
            /*
            Up - 0 (+), Down - 1 (-) - y direction
            West - 2 (-), East - 3 (+) - x direction
            North - 4 (-), South - 5 (+) - z direction
            

            Vec3d up = new Vec3d(minVec.x, minVec.y+1, minVec.z);
            if(withinRangeOutput(up)){
                Vector<Vector<Integer>> picker = adj.get(picked);
                Vector<Integer> possibilities = picker.get(0);
                Integer[] converted = convertObjToInt(possibilities.toArray());
                Integer[] current = outputInteger[(int)up.x][(int)up.y][(int)up.z];

                outputInteger[(int)up.x][(int)up.y][(int)up.z] = intersectIntegerArrays(current, converted);
            }

            Vec3d down = new Vec3d(minVec.x, minVec.y-1, minVec.z);
            if(withinRangeOutput(down)){
                Vector<Vector<Integer>> picker = adj.get(picked);
                Vector<Integer> possibilities = picker.get(1);
                Integer[] converted = convertObjToInt(possibilities.toArray());
                Integer[] current = outputInteger[(int)down.x][(int)down.y][(int)down.z];

                outputInteger[(int)down.x][(int)down.y][(int)down.z] = intersectIntegerArrays(current, converted);
            }

            Vec3d east = new Vec3d(minVec.x+1, minVec.y, minVec.z);
            if(withinRangeOutput(east)){
                Vector<Vector<Integer>> picker = adj.get(picked);
                Vector<Integer> possibilities = picker.get(3);
                Integer[] converted = convertObjToInt(possibilities.toArray());
                Integer[] current = outputInteger[(int)east.x][(int)east.y][(int)east.z];

                outputInteger[(int)east.x][(int)east.y][(int)east.z] = intersectIntegerArrays(current, converted);
            }

            Vec3d west = new Vec3d(minVec.x-1, minVec.y, minVec.z);
            if(withinRangeOutput(west)){
                Vector<Vector<Integer>> picker = adj.get(picked);
                Vector<Integer> possibilities = picker.get(2);
                Integer[] converted = convertObjToInt(possibilities.toArray());
                Integer[] current = outputInteger[(int)west.x][(int)west.y][(int)west.z];

                outputInteger[(int)west.x][(int)west.y][(int)west.z] = intersectIntegerArrays(current, converted);
            }

            Vec3d north  = new Vec3d(minVec.x, minVec.y, minVec.z-1);
            if(withinRangeOutput(north)){
                Vector<Vector<Integer>> picker = adj.get(picked);
                Vector<Integer> possibilities = picker.get(4);
                Integer[] converted = convertObjToInt(possibilities.toArray());
                Integer[] current = outputInteger[(int)north.x][(int)north.y][(int)north.z];

                outputInteger[(int)north.x][(int)north.y][(int)north.z] = intersectIntegerArrays(current, converted);
            }

            Vec3d south = new Vec3d(minVec.x, minVec.y, minVec.z+1);
            if(withinRangeOutput(south)){
                Vector<Vector<Integer>> picker = adj.get(picked);
                Vector<Integer> possibilities = picker.get(5);
                Integer[] converted = convertObjToInt(possibilities.toArray());
                Integer[] current = outputInteger[(int)south.x][(int)south.y][(int)south.z];

                outputInteger[(int)south.x][(int)south.y][(int)south.z] = intersectIntegerArrays(current, converted);
            }

            collapsedOutputIntegerCoordinates.add(minVec);
        }
        //If a block is intersected and has one possibility, collapse it and then propogate that change
        //Print4DArray(outputInteger);

        //Shrink OutputIntegers to get rid of edges
        Integer[][][] outputIntegerRefined = new Integer[xLength-2][yLength-2][zLength-2];
        for(int x = 1; x < xLength-1; x++){
            for(int y = 1; y < yLength-1; y++){
                for(int z = 1; z < zLength-1; z++){
                    if(outputInteger[x][y][z].length == 0){
                        return -1;
                    }
                    outputIntegerRefined[x-1][y-1][z-1] = outputInteger[x][y][z][0];
                } 
            }
        }
        //SignBlock(Settings.of())
        Print3DArray(outputIntegerRefined);
        System.out.println("Not Crashed?");


        //Go through each integer, and generate the chunks - think about if this is going the correct way, or if I need to rethink my coordinate pairs
        for(int x = 0; x < outputIntegerRefined.length; x++){
            for(int y = 0; y < outputIntegerRefined[x].length; y++){
                for(int z = 0; z < outputIntegerRefined[x][y].length; z++){
                    WFCChunk c = integerToChunkMap.get(outputIntegerRefined[x][y][z]);
                    Vec3d coor = new Vec3d(runMax.getX()-x, runMax.getY() - y, runMax.getZ() - z);
                    if(x == outputIntegerRefined.length-1 && y == outputIntegerRefined[x].length-1){ //X and Y
                        GenerateChunkXsAndYs(c, coor);
                        //System.out.println("Generate Found X and Y chunk edge");
                    } else 
                    if(x == outputIntegerRefined.length-1 && z == outputIntegerRefined[x][y].length-1){ //X and Z
                        GenerateChunkXsAndZs(c, coor);
                        //System.out.println("Generate Found X and Z chunk edge");
                    } else
                    if(z == outputIntegerRefined[x][y].length-1 && y == outputIntegerRefined[x].length-1) { // Z and y
                        GenerateChunkZsAndYs(c, coor);
                        //System.out.println("Generate Found Z and Y chunk edge");
                    } else 
                    if(x == outputIntegerRefined.length-1 && z == outputIntegerRefined[x][y].length-1 && y == outputIntegerRefined[x].length-1) { //X y and Z
                        GenerateChunkXYAndZs(c, coor);
                        //System.out.println("Generate Found X and Y and Z chunk edge");
                    } else 
                    if(x == outputIntegerRefined.length-1){
                        GenerateChunkXs(c, coor);
                        //System.out.println("Generate Found X chunk edge");
                    } else
                    if(y == outputIntegerRefined[x].length-1){
                        GenerateChunkYs(c, coor);
                        //System.out.println("Generate Found Y chunk edge");
                    } else
                    if(z == outputIntegerRefined[x][y].length-1){
                        GenerateChunkZs(c, coor);
                        //System.out.println("Generate Found Z chunk edge");
                    } else {
                        GenerateChunkPartial(c, coor);
                        //System.out.println("Generate partial chunk");
                    }
                    
                }
            }
        }

        return 1;
    }

    private void GenerateChunkXsAndYs(WFCChunk c, Vec3d coor) {
        System.out.println("XY");
        Integer[][][] r = c._chunkBlockValues;
        for(int x = 0; x < r.length; x++){
            for(int y = 0; y < r[x].length; y++){
                world.setBlockState(new BlockPos(coor.x-x, coor.y - y, coor.z), seenIntToBlock.get(r[x][r[x].length - y - 1][0]));
            }
        }
    }

    private void GenerateChunkXsAndZs(WFCChunk c, Vec3d coor) {
        System.out.println("XZ");
        Integer[][][] r = c._chunkBlockValues;
        for(int x = 0; x < r.length; x++){
            for(int z = 0; z < r[x][0].length; z++){
                world.setBlockState(new BlockPos(coor.x-x, coor.y, coor.z-z), seenIntToBlock.get(r[x][0][z]));
            }
        }
    }

    private void GenerateChunkZsAndYs(WFCChunk c, Vec3d coor) {
        System.out.println("YZ");
        Integer[][][] r = c._chunkBlockValues;
        for(int y = 0; y < r[0].length; y++){
            for(int z = 0; z < r[0][y].length; z++){
                world.setBlockState(new BlockPos(coor.x, coor.y-y, coor.z-z), seenIntToBlock.get(r[0][y][z]));
            }
        }
    }

    private void GenerateChunkXYAndZs(WFCChunk c, Vec3d coor) {
        System.out.println("XYZ");
        Integer[][][] r = c._chunkBlockValues;
        for(int x = 0; x < r.length; x++) {
            for(int y = 0; y < r[0].length; y++){
                for(int z = 0; z < r[0][y].length; z++){
                    world.setBlockState(new BlockPos(coor.x-x, coor.y-y, coor.z-z), seenIntToBlock.get(r[x][y][z]));
                }
            }
        }
    }

    private void GenerateChunkXs(WFCChunk c, Vec3d coor) {
        System.out.println("X");
        Integer[][][] r = c._chunkBlockValues;
        for(int x = 0; x < r.length; x++){
            world.setBlockState(new BlockPos(coor.x-x, coor.y, coor.z), seenIntToBlock.get(r[x][0][0]));
        }
    }

    private void GenerateChunkYs(WFCChunk c, Vec3d coor) {
        System.out.println("Y");
        Integer[][][] r = c._chunkBlockValues;
        for(int y = 0; y < r[0].length; y++){
            world.setBlockState(new BlockPos(coor.x, coor.y - y, coor.z), seenIntToBlock.get(r[0][y][0]));
        }
    }
    
    private void GenerateChunkZs(WFCChunk c, Vec3d coor) {
        System.out.println("Z");
        Integer[][][] r = c._chunkBlockValues;
        for(int z = 0; z < r[0][0].length; z++){
            world.setBlockState(new BlockPos(coor.x, coor.y, coor.z-z), seenIntToBlock.get(r[0][0][z]));
        }
    }

    private void GenerateChunkPartial(WFCChunk c, Vec3d coor){
        System.out.println("Partial");
        Integer[][][] r = c._chunkBlockValues;
        world.setBlockState(new BlockPos(coor.x, coor.y, coor.z), seenIntToBlock.get(r[0][0][0]));
    }


    private boolean withinRangeOutput(Vec3d coor){
        int x = (int)coor.x;
        int y = (int)coor.y;
        int z = (int)coor.z;

        if(x < 0 || y < 0 || z < 0){
            return false;
        } else if (x > outputInteger.length-1 || y > outputInteger[0].length-1 || z > outputInteger[0][0].length-1) {
            return false;
        } else {
            return true;
        }
    }

    private Integer[] intersectIntegerArrays(Object[] a, Object[] b){
        Integer[] alpha = (Integer[]) a;
        Integer[] beta = (Integer[]) b;

        HashSet<Integer> set = new HashSet<>(); 
     
        set.addAll(Arrays.asList(alpha));
        
        set.retainAll(Arrays.asList(beta));
        
        //convert to array
        Integer[] intersection = {};
        intersection = set.toArray(intersection);

        return intersection;
    }

    public int secondStepWrapper(int runs){
        print("Running WFC with " + runs + " runs.");
        int ret = 0;
        for(int i = 0; i < runs; i++){
            ret = secondStepRevamped();
            if(ret == 1){
                break;
            }
        }

        print("We got out!");

        if(ret == -1){
            print("Failed to find valid configuration in " + runs + " runs.");
        }

        return ret;
    }

    /*
    0 - Up
    1 - Down
    2 - Left
    3 - Right
    4 - Forward
    5 - Back
    

    //Spawns the read chunks into the world for debug purposes
    public int debugChunks(Vec3d playerPos) {

        BlockPos startPos = new BlockPos(playerPos.getX(), playerPos.getY(), playerPos.getZ());
        int offset = 0; 
        int i = 0;
        for(int c : listOfSeenChunks.keySet()){
            WFCChunk chunk = integerToChunkMap.get(c);
            //System.out.println(integerToChunkMap.get(x)._chunkAdjs);
            //is read in by z, y, x, figure out what that means

            for(int q = 0; q < chunkSize; q++){
                for(int w = 0; w < chunkSize; w++){
                    for(int r = 0; r < chunkSize; r++){
                        BlockPos point = new BlockPos(startPos.getX() + offset + q, startPos.getY() - w, startPos.getZ() + r);
                        BlockState toSpawn = seenIntToBlock.get(chunk._chunkBlockValues[chunkSize-q-1][chunkSize-w-1][chunkSize-r-1]);
                        world.setBlockState(point, toSpawn);
                    }
                }
            }

            i++;
            offset = i * (chunkSize + 1);
        }   

        return 1;
    }


*/ 

/* AI Generated Attempt to construct wave function collapse

 * import java.util.*;

public class WaveFunctionCollapse {
  private int[][][] inputMatrix;
  private int[][][] outputMatrix;
  private int M;
  private int N;

  public WaveFunctionCollapse(int[][][] input, int M, int N) {
    this.inputMatrix = input;
    this.M = M;
    this.N = N;
  }

  public int[][][] collapse() {
    outputMatrix = new int[N][N][N];
    for (int i = 0; i < N; i++) {
      for (int j = 0; j < N; j++) {
        for (int k = 0; k < N; k++) {
          List<Integer> possibleValues = getPossibleValues(i, j, k);
          if (possibleValues.size() == 1) {
            outputMatrix[i][j][k] = possibleValues.get(0);
          } else {
            Random random = new Random();
            int index = random.nextInt(possibleValues.size());
            outputMatrix[i][j][k] = possibleValues.get(index);
          }
        }
      }
    }
    return outputMatrix;
  }

    private List<Integer> getPossibleValues(int i, int j, int k) {
    List<Integer> possibleValues = new ArrayList<>();
    for (int v = 0; v < M; v++) {
    boolean isPossible = true;
    for (int ni = i - 1; ni <= i + 1; ni++) {
        for (int nj = j - 1; nj <= j + 1; nj++) {
        for (int nk = k - 1; nk <= k + 1; nk++) {
            if (ni >= 0 && ni < N && nj >= 0 && nj < N && nk >= 0 && nk < N) {
            if (outputMatrix[ni][nj][nk] == v) {
                isPossible = false;
                break;
            }
            } else {
            int mi = (int) (ni * (double) N / M);
            int mj = (int) (nj * (double) N / M);
            int mk = (int) (nk * (double) N / M);
            if (mi >= 0 && mi < M && mj >= 0 && mj < M && mk >= 0 && mk < M) {
                if (inputMatrix[mi][mj][mk] == v) {
                isPossible = false;
                break;
                }
            }
            }
        }
        if (!isPossible) {
            break;
        }
        }
        if (!isPossible) {
        break;
        }
    }
    if (isPossible) {
        possibleValues.add(v);
    }
    }
    return possibleValues;
    }
}

 */
