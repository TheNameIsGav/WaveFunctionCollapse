package net.fabricmc.wavy;

public class OldWaveDriverCode {
    /*
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
*/

/*//Builds Adjacency matrix and setups block to int conversion
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
    } */


    /* 

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
 */


 /*
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
    } */

    /*
    private Vector3d chunkIsBorderingEdgeGenerate(BlockPos curr){
        int xThreshold = runMin.getX() + chunkSize - 1;
        int yThreshold = runMin.getY() + chunkSize - 1;
        int zThreshold = runMin.getZ() + chunkSize - 1;

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
 */

 /*public int secondStepWFC(){
        collapseMap = new HashMap<BlockPos, Vector<Integer>>();
        collapsed = new HashSet<BlockPos>();
        listOfSeenBlocks.put(-1, 0);

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
        System.out.println(blockToIntegerMap);
        //Setup The Beginning Collapse Map

        //runPos1 = new BlockPos(0, 0, 0);
        //runPos2 = new BlockPos(0, 0, 0);

        //Verify integrity of Add edges
        //System.out.println("Before Change \n Run Pos 1: " + runPos1 + "\nRun Pos 2: " + runPos2);
        AddEdges();
        // System.out.println("After Change \n Run Pos 1: " + runPos1 + "\n Run Pos 2: " + runPos2);
        // System.out.println("\n Should match original runPositions \n Original Run Pos1: " + originalRunPos1 + "\n Original Run Pos2: " + originalRunPos2);

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

        System.out.println("Made it past Adding edges and adding to collapse map");

        // System.out.println("Size of CollapseMap before Manipulate Edges: " + collapseMap.size());
        // System.out.println("Size of Collapsed Nodes before Manipulate Edges: " + collapsed.size());
        // System.out.println("\nRun Pos1: " + runPos1 + "\nRun Pos2: " + runPos2);
        
        ManipulateEdges();

        System.out.println("Made it past manipulating edges");

        // System.out.println("Size of CollapseMap after Manipulate Edges: " + collapseMap.size());
        // System.out.println("Size of Collapsed Nodes after Manipulate Edges: " + collapsed.size());
        // System.out.println("\nRun Pos1: " + runPos1 + "\nRun Pos2: " + runPos2);

        // System.out.println(collapsed.contains(originalRunPos1));

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

            //System.out.println("Test 1");

            if(current == null){
                validConfig = false;
                break;
            }

            //Collapse it
            if(!collapseNode(current)){
                validConfig = false;
                break;
            }

            //System.out.println("Test 2");

            //Change the surrounding nodes
            changeSurrounding(current, SHOULDWRAP);

            //System.out.println("Test 3");

            //GenerateSingleBlock(current);
        }
            //if(validConfig){
            //    break;
            //}
        //}

        System.out.println("Made it past the waavy step");

        RemoveEdgeBlocks();

        //System.out.println(collapseMap);

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
    } */


    /*//Method to add an outer ring to the collapse map by updating the runPositions
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
        runMin = new BlockPos(minX, minY, minZ);

    } */

    /* private void CollapseCorners(){
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
    } */

    // private int buildAdjacenciesChunks(){

    //     //Add edge
    //     Vector<Vector<Integer>> a = new Vector<Vector<Integer>>();
    //     for(int i = 0; i < 6; i++){
    //         Vector<Integer> b = new Vector<Integer>();
    //         a.add(b);
    //     }
    //     adj.put(-1, a);
        
    //     //For each position, assume top left and build from there
    //     for(int x = max.getX(); x >= min.getX() + chunkSize-1; x--){
    //         for(int y = max.getY(); y >= min.getY() + chunkSize-1; y--){
    //             for(int z = max.getZ(); z >= min.getZ() + chunkSize-1; z--){
    //                 ////System.out.println("Chunk Start Pos: " + x + ", " + y + ", " + z);
    //                 WFCChunk newChunk = addChunk(x, y, z, true); //Validated with single chunk size

    //                 AddChunkAdjacencies(newChunk._id, new BlockPos(x, y, z));
    //             }
    //         }
    //     }

    //     return 1;
    // }

        // private Integer[][][] readChunkByChunkSize(int c, int u, int v){
    //     Integer[][][] a = new Integer[chunkSize][chunkSize][chunkSize];

    //     for(int x = 0; x < chunkSize; x++){
    //         for(int y = 0; y < chunkSize; y++){
    //             for(int z = 0; z < chunkSize; z++){
    //                 BlockPos curr = new BlockPos(c - x, u - y, v - z);
    //                 ////System.out.println("Printing current position inside readChunkByChunkSize: " + curr);

    //                 //If our position is inside the grid
    //                 if(evaulatePositionRework(curr)){
    //                     //Adds that int to our array
    //                     a[x][y][z] = seenBlocksToInt.get(world.getBlockState(curr));

    //                 } else {
    //                     //System.out.println("we should not get here if I have done my math properly");
    //                     //a.add(-1);
    //                 }
    //             }
    //         }
    //     }
    //     return a;
    // }

    //Given the top left corner of a block, extends to read in blocks in the size of the chunk
    // private WFCChunk addChunk(int x, int y, int z, boolean actual){
    //     WFCChunk ret = new WFCChunk();

    //     //Actually read in the values from the set
    //     Integer[][][] a = readChunkByChunkSize(x, y, z);
    //     ret._chunkBlockValues = a;

    //     Vector<Vector<Integer>> newVec = new Vector<Vector<Integer>>();
    //     for(int i = 0; i < 6; i++){
    //         newVec.add(new Vector<Integer>());
    //     }

    //     //Check to see if we have seen this particular arrangement of chunk before
    //     int flag = checkChunkSeenBefore(ret);

    //     if(flag == -2){ //This is a new chunk
    //         ret._id = currentIndex;
    //         currentIndex++;
    //         listOfSeenChunks.put(ret._id, 1);
    //         adj.put(ret._id, newVec);
    //         integerToChunkMap.put(ret._id, ret);
    //         chunkToIntegerMap.put(ret, ret._id);
    //         ////System.out.println("New Ret: " + ret);
    //     } else if (actual){ //This is a seen chunk, so increase amt by 1
    //         ret = integerToChunkMap.get(flag);
            
    //         listOfSeenChunks.put(flag, listOfSeenChunks.get(flag) + 1);
    //         ////System.out.println("Found old chunk with values " + ret);
    //     } else {
    //         ret = integerToChunkMap.get(flag);
    //     }
    //     return ret;
    // }

        //Modify these methods - I never get to a blockposition that cannot be read in it's entirety, so I just need to check if it's got an edge

    //Its not that specific directions don't work, it's that multiple elements in the adjs don't work
    // private void AddChunkAdjacencies(int id, BlockPos pos) {
    //     /*
    //         Up - 0, Down - 1
    //         West - 2, East - 3
    //         North - 4, South - 5
    //     */

    //     //Checking the 3 min-chunk borders
    //     Vector3d minBorders = chunkIsBorderingEdgeMin(pos);

    //     Vector3d maxBorders = chunkIsBorderingEdgeMax(pos);
        
    //     if(minBorders.x == 1){         
    //         addChunkEdgeAdjacency(id, 2, 3);
    //     } else { //Normal
    //         addChunkAdjacency(id, 2, pos.west());
    //     }

    //     if(minBorders.y == 1) { //We have a down edge (1) 
    //         addChunkEdgeAdjacency(id, 1, 0);
    //     } else { //Normal
    //         addChunkAdjacency(id, 1, pos.down());
    //     }

    //     if(minBorders.z == 1){ //We have a northward edge (4)
    //         addChunkEdgeAdjacency(id, 4, 5);
    //     } else { //Normal
    //         addChunkAdjacency(id, 4, pos.north());
    //     }

    //     //Checking the 3 max borders
        

    //     if(maxBorders.x == 1){ //We have a Eastward Edge (3)
    //         addChunkEdgeAdjacency(id, 3, 2);
    //     } else {
    //         addChunkAdjacency(id, 3, pos.east());
    //     }

    //     if(maxBorders.y == 1){ //We have an up edge (0)
    //         addChunkEdgeAdjacency(id, 0, 1);
    //     } else {
    //         addChunkAdjacency(id, 0, pos.up());
    //     }

    //     if(maxBorders.z == 1) {//We have a Southern Edge (5)
    //         addChunkEdgeAdjacency(id, 5, 4);
    //     } else {
    //         addChunkAdjacency(id, 5, pos.south());
    //     }
    // }

    // private Vector3d chunkIsBorderingEdgeMax(BlockPos curr) {
    //     int xThreshold = max.getX();
    //     int yThreshold = max.getY();
    //     int zThreshold = max.getZ();

    //     Vector3d ret = new Vector3d(0,0, 0);

    //     if(curr.getX() == xThreshold){
    //         ret.x = 1;
    //     }

    //     if(curr.getY() == yThreshold){
    //         ret.y = 1;
    //     }

    //     if(curr.getZ() == zThreshold){
    //         ret.z = 1;
    //     }
    
    //     return ret;
    // }

    // //If the block position that I am currently on is equal to x, y, or z chunk thresholds, return the axis to add edge adjacency
    // //returns a vector of axis that border

    // private Vector3d chunkIsBorderingEdgeMin(BlockPos curr){
    //     int xThreshold = min.getX() + chunkSize - 1;
    //     int yThreshold = min.getY() + chunkSize - 1;
    //     int zThreshold = min.getZ() + chunkSize - 1;

    //     Vector3d ret = new Vector3d(0,0,0);

    //     if(curr.getX() == xThreshold){
    //         ret.x = 1;
    //     }

    //     if(curr.getY() == yThreshold){
    //         ret.y = 1;
    //     }

    //     if(curr.getZ() == zThreshold){
    //         ret.z = 1;
    //     }
    
    //     return ret;
    // }

    // //Add chunk adjacency and then edge adjacency in the opposite direction
    // private void addChunkEdgeAdjacency(int id, int direction, int opposite){
    //     Vector<Integer> prevAdj = adj.get(id).get(direction);
    //     if(!prevAdj.contains(-1)){
    //         prevAdj.add(-1);
    //         adj.get(id).set(direction, prevAdj);
    //     }
        
    //     Vector<Integer> testPrev = adj.get(-1).get(opposite); 
    //     if(!testPrev.contains(id)){
    //         testPrev.add(id);
    //         adj.get(-1).set(opposite, testPrev);
    //     }
        
    // }

    // private void addChunkAdjacency(int id, int direction, BlockPos newPos){
    //     WFCChunk adjChunk = addChunk(newPos.getX(), newPos.getY(), newPos.getZ(), false);
    //     ////System.out.println("Adjacent Chunk: " + adjChunk);
    //     Vector<Integer> originalAdjs = adj.get(id).get(direction);

    //     if(!originalAdjs.contains(adjChunk._id)){
    //         originalAdjs.add(adjChunk._id);
    //         adj.get(id).set(direction, originalAdjs);
    //     }
    // }

    // private boolean evaulatePositionRework(BlockPos test){

    //     if(test.getX() > max.getX() || test.getX() < min.getX()){
    //         return false;
    //     }

    //     if(test.getY() > max.getY() || test.getY() < min.getY()){
    //         return false;
    //     }

    //     if(test.getZ() > max.getZ() || test.getZ() < min.getZ()){
    //         return false;
    //     }

    //     return true;
    // }

    // private int buildAdjacenciesChunks(){

    //     //Add edge
    //     Vector<Vector<Integer>> a = new Vector<Vector<Integer>>();
    //     for(int i = 0; i < 6; i++){
    //         Vector<Integer> b = new Vector<Integer>();
    //         a.add(b);
    //     }
    //     adj.put(-1, a);
        
    //     //For each position, assume top left and build from there
    //     for(int x = max.getX(); x >= min.getX() + chunkSize-1; x--){
    //         for(int y = max.getY(); y >= min.getY() + chunkSize-1; y--){
    //             for(int z = max.getZ(); z >= min.getZ() + chunkSize-1; z--){
    //                 ////System.out.println("Chunk Start Pos: " + x + ", " + y + ", " + z);
    //                 WFCChunk newChunk = addChunk(x, y, z, true); //Validated with single chunk size

    //                 AddChunkAdjacencies(newChunk._id, new BlockPos(x, y, z));
    //             }
    //         }
    //     }

    //     return 1;
    // }
}
