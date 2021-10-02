using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System.Linq;

public class ManagerScript : MonoBehaviour
{
    public int width; //x
    public int height; //y
    public int length; //z
    public LayerMask world;
    public GameObject tile;
    public GameObject[] worldBlocks;
    public static ManagerScript instance;

    //Possibilites are worldMap ints that Might be a tile
    //Adjacencies are the objectID's that are allowed to be next to the current object
    public Dictionary<int, Tiles> adjacencies = new Dictionary<int, Tiles>(); //objectID to Tile Object
    private MapCoordinate[,,] worldMap; //Contains an Arry of Map Coordinates (which have an x, a y, and a list of possible features to spawn) // Will eventually also contain a Z
    private Dictionary<int, GameObject> objectMap = new Dictionary<int, GameObject>(); //maps object ID to gameObject
    private Dictionary<string, int> tagMap = new Dictionary<string, int>(); //Maps tag to object ID
    
    private BoxCollider box;

    public class Tiles
    {
        public Dictionary<int, List<int>> directionMapping = new Dictionary<int, List<int>>();

        //Keep track of occurances and the prefabtype of the object
        public int occurances = 0;
        public int prefabType; //Prefab type is the keyword of the prefab
        public string name;

        public Tiles(int inc) { 
            prefabType = inc;
            directionMapping.Add(0, new List<int>()); //Dirty way of adding the directions to the dictionary
            directionMapping.Add(1, new List<int>());
            directionMapping.Add(2, new List<int>());
            directionMapping.Add(3, new List<int>());
            directionMapping.Add(4, new List<int>());
            directionMapping.Add(5, new List<int>());
        }

        public void addAdjacencies(int direction, int inc) { directionMapping[direction].Add(inc); } //This will be built initially

        public void convertToSet()
        {
            for(int i = 0; i < directionMapping.Keys.Count; i++)
            {
                directionMapping[i] = directionMapping[i].Distinct().ToList();
            }
        }

        public List<int> getAdjacenciesByDirection(int direction) { return directionMapping[direction]; }

        public bool equals(Tiles other) { if (prefabType == other.prefabType) { return true; } else { return false; } }

        public override string ToString()
        {
            string ret = "";
            ret += " Printing Tile Adjacencies for tile type: " + name + "\n";

            for(int i = 0; i < directionMapping.Keys.Count; i++)
            {
                string tmp = "[";
                foreach(var x in directionMapping[i])
                {
                    tmp += x.ToString() + ", ";
                }
                tmp += "]";
                ret += "For direction " + i + " the adjacencies are " + tmp + "\n";
            }

            return ret;
        }

    }

    public class MapCoordinate
    {
        private Vector3Int coords;
        private List<int> possibilites;
        private bool hasFixed = false;
        public int thisTile;

        public MapCoordinate() { coords = new Vector3Int(0, 0, 0); }
        public MapCoordinate(int incX, int incY, int incZ) { coords = new Vector3Int(incX, incY, incZ); }

        public MapCoordinate(int incX, int incY, int incZ, List<int> possible)
        {
            coords = new Vector3Int(incX, incY, incZ);
            possibilites = possible;
        }

        public void updatePossibilites(List<int> incPossibilites) { possibilites = incPossibilites; }

        public List<int> getPossibilities() { return possibilites; }

        public Vector3Int getCoords() { return coords; }

        /// <summary>
        /// "Fixes" a node in place and selects one of the possibilites based on occurances
        /// </summary>
        public void Fix() { 
            hasFixed = true;

            List<(float, int)> weighted = new List<(float,int)>(); //List of Weight and Type
            int totalBlocks = 0;
            foreach(int i in possibilites) //Count total blocks and add initial occurances
            {
                int occurances = ManagerScript.instance.adjacencies[i].occurances;
                weighted.Add((occurances, i));
                totalBlocks += occurances;

            }

            float totalWeight = 0;
            for(int i = 0; i < weighted.Count; i++) 
            {
                weighted[i] = (weighted[i].Item1 / totalBlocks, weighted[i].Item2);
                totalWeight += weighted[i].Item1;
            }

            float randWeight = Random.Range(0, totalWeight);

            weighted.Sort((x, y) => x.Item1.CompareTo(y.Item1));
            
            foreach((float x, int y) in weighted)
            {
                Debug.Log("Weight: " + x + " Type: " + y);
            }


            int selected = 0;
            for(int i = 1; i < weighted.Count; i++)
            {
                if(weighted[i].Item1 > randWeight)
                {
                    selected = weighted[i].Item2;
                    break;
                }
            }

            foreach((float weight, int tile) in weighted)
            {
                //Debug.Log("Weight of " + tile + " is " + weight);
            }
            thisTile = selected;
            possibilites = new List<int> { selected };
        }

        public bool isFixed() { return hasFixed; }

        override public string ToString()
        {
            string tmp = "[";
            foreach (var x in possibilites)
            {
                tmp += x.ToString() + ", ";
            }
            tmp += "]";
            return "For block Map BLock @: " + coords.ToString() + " the possibilites at this point are " + tmp;
        }

    }








    private void Awake()
    {

        instance = this;
        //New code for generating adjacency matrix
        box = GetComponent<BoxCollider>();
        //Setup the default adjacencies
        worldMap = new MapCoordinate[width, height, length];

        for (int i = 0; i < worldBlocks.Count(); i++) //"Building Blocks" are children of the manager
        {
            objectMap.Add(i, worldBlocks[i]); //Add all possible tiles to the objectMap with their ID
            tagMap.Add(worldBlocks[i].tag, i); //Map the correct tag to the object ID for use later.
            adjacencies.Add(i, new Tiles(i)); //Add a 0 occurance version of the tile to the map for later use
        }

        foreach(string e in tagMap.Keys.ToList())
        {
            //Debug.Log(e + "\n");
        }

        

        buildAdjacencyMatrix();
        printAdjacencyMatrix();


        //Setup the world map
        for (int x = 0; x < width; x++)
        {
            for (int y = 0; y < height; y++)
            {
                for (int z = 0; z < length; z++)
                {
                    worldMap[x, y, z] = (new MapCoordinate(x, y, z, adjacencies.Keys.ToList())); //Sets every world point to every possible adjacency
                }
            }
        }
    }

    // Start is called before the first frame update
    void Start()
    {
        
    }

    private void printAdjacencyMatrix()
    {
        for(int i = 0; i < adjacencies.Keys.Count; i++)
        {
            Debug.Log(adjacencies[i].ToString());
        }
    }

    void RunWFC()
    {
        //Debug.Log("Running WFC");
        MapCoordinate curr = null; //worldMap[(Random.Range(0, width)), Random.Range(0, height), Random.Range(0, length)]; //select random map coordinate
        //Debug.Log(curr.getPossibilities().Count);

        int min = int.MaxValue;

        foreach(MapCoordinate mc in worldMap)
        {
            //Debug.Log(mc.getCoords() + " " + mc.getPossibilities().Count);
        }

        foreach (MapCoordinate mc in worldMap) //Find map coordinate with the least possibilites
        {
            if (!mc.isFixed()) //if current map coordinate isn't fixed
            {
                //Debug.Log(mc.getPossibilities().Count);
                if (mc.getPossibilities().Count < min)//curr.getPossibilities().Count) //if it has less possibilites than the random one we selected
                {
                    
                    //Debug.Log("Found new low entropy unit @" + mc.getCoords() + " with count " + mc.getPossibilities().Count);
                    curr = mc; //asign it 
                    min = curr.getPossibilities().Count;
                }
            }
        }

       // Debug.Log("Found low entropy unit at " + curr.getCoords());

        if (curr.getPossibilities().Count == 0) //If anything has 0 possibilites, then we fail
        {
            
            Debug.LogError("Impossible Pattern @ " + curr.getCoords());
            return;
        }

        //At this point, curr should be the node with the least entropy, so we fix it in place
        curr.Fix();


        //After we Fix it, then update all the adjacent nodes
        
        Vector3Int trans = findBounds(curr.getCoords() + Vector3Int.up);
        updateFromAdjacent(trans, curr, 0);
        
        trans = findBounds(curr.getCoords() + Vector3Int.down);
        updateFromAdjacent(trans, curr, 1);

        trans = findBounds(curr.getCoords() + Vector3Int.left);
        updateFromAdjacent(trans, curr, 2);

        trans = findBounds(curr.getCoords() + Vector3Int.right);
        updateFromAdjacent(trans, curr, 3);

        trans = findBounds(curr.getCoords() + Vector3Int.forward);
        updateFromAdjacent(trans, curr, 4);

        trans = findBounds(curr.getCoords() + Vector3Int.back);
        updateFromAdjacent(trans, curr, 5);


        foreach (MapCoordinate mc in worldMap)
        {
            //Debug.Log(mc.getCoords() + " " + mc.getPossibilities().Count);
        }

        //Spawn the fixed map coordinate
        if (curr.thisTile != -1)
        {
            Object.Instantiate(objectMap[curr.thisTile], curr.getCoords(), Quaternion.identity); //curr.GetCoords would need to be multiplied by the size of the chunk eventually
        }
    }

    void updateFromAdjacent(Vector3Int trans, MapCoordinate curr, int dir) //Curr is the just fixed node, trans is the node to be updating
    {
        if (!worldMap[trans.x, trans.y, trans.z].isFixed()) //If the node is not fixed, maybe fix for AIR
        {
            List<int> fixedTileAdjacencies = adjacencies[curr.thisTile].getAdjacenciesByDirection(dir); //Gets adjacency for that tile type in that direction
            MapCoordinate nodeToUpdate = worldMap[trans.x, trans.y, trans.z]; //Get the node to update
            List<int> tmp = nodeToUpdate.getPossibilities(); //Get it's possibilites
            tmp = tmp.Intersect(fixedTileAdjacencies).ToList(); //Intersect it
            nodeToUpdate.updatePossibilites(tmp); //Update the nodes to the intersection
            worldMap[trans.x, trans.y, trans.z] = nodeToUpdate;

            //Debug.Log("Updating " + trans + " with the appropriate adjacencies of " + PrintIntList(fixedTileAdjacencies) + "\n Combining that with the list " + PrintIntList(tmp) + " yielded " + PrintIntList(nodeToUpdate.getPossibilities()));
        }
    }


    string PrintIntList(List<int> list)
    {
        string ret = "";
        foreach(int elem in list)
        {
            ret += elem + ", ";
        }
        return ret;
    }

    /// <summary>
    /// Used for finding bounds around the output layer
    /// </summary>
    /// <param name="incVec"></param>
    /// <returns></returns>
    Vector3Int findBounds(Vector3Int incVec) //Proper usage of this function: call on a transformed vector and will return the correct map coordinate to modify
    {
        //Debug.Log("Finding bounds for " + incVec);
        if(incVec.x >= width)
        {
            return new Vector3Int(0, incVec.y, incVec.z);
        }
        if(incVec.x < 0)
        {
            return new Vector3Int(width-1, incVec.y, incVec.z);
        }

        if (incVec.y >= height)
        {
            return new Vector3Int(incVec.x, 0, incVec.z);
        }
        if(incVec.y < 0)
        {
            return new Vector3Int(incVec.x, height-1 , incVec.z);
        }

        if (incVec.z >= length)
        {
            return new Vector3Int(incVec.x, incVec.y, 0);
        }
        if(incVec.z < 0)
        {
            return new Vector3Int(incVec.x, incVec.y, length-1);
        }

        return incVec;
    }


    /// <summary>
    /// Used for finding bounds around the input layer
    /// </summary>
    /// <param name="minVec"></param>
    /// <param name="maxVec"></param>
    /// <param name="incVec"></param>
    /// <returns></returns>
    Vector3Int findBoundsInputLayer(Vector3Int minVec, Vector3Int maxVec, Vector3Int incVec)
    {
        if (incVec.x >= maxVec.x)
        {
            return new Vector3Int(minVec.x, incVec.y, incVec.z);
        }
        if (incVec.x < minVec.x)
        {
            return new Vector3Int(maxVec.x, incVec.y, incVec.z);
        }

        if (incVec.y >= maxVec.y)
        {
            return new Vector3Int(incVec.x, minVec.y, incVec.z);
        }
        if (incVec.y < minVec.y)
        {
            return new Vector3Int(incVec.x, maxVec.y, incVec.z);
        }

        if (incVec.z >= maxVec.z)
        {
            return new Vector3Int(incVec.x, incVec.y, minVec.z);
        }
        if (incVec.z < minVec.z)
        {
            return new Vector3Int(incVec.x, incVec.y, maxVec.y);
        }

        return incVec;
    }

    void buildAdjacencyMatrix()
    {
        //Blocks are 1x1 and are spaced 1 unit apart
        Collider[] tmp = Physics.OverlapBox(transform.position, box.bounds.extents);


        //Keep the size of the bounding box to an odd number so that the conversions work correctly
        Vector3Int minVec = new Vector3Int((int)transform.position.x - (int)(box.size.x / 2), (int)transform.position.y - (int)(box.size.y / 2), (int)transform.position.z - (int)(box.size.z / 2));
        Vector3Int maxVec = new Vector3Int((int)transform.position.x + (int)(box.size.x / 2), (int)transform.position.y + (int)(box.size.y / 2), (int)transform.position.z + (int)(box.size.z / 2));


        for(int x = minVec.x; x <= maxVec.x; x++)
        {
            for(int y = minVec.y; y <= maxVec.y; y++)
            {
                for(int z = minVec.z; z <= maxVec.z; z++)
                {
                    Vector3Int incVec = new Vector3Int(x, y, z);
                    //Debug.Log(incVec);
                    Collider[] hit = Physics.OverlapSphere(incVec, .1f, world);

                    //Debug.Log(hit.Length);
                    GameObject obj;

                    if (hit.Length == 1) //Hit a block
                    {
                        obj = hit[0].gameObject;
                    } else //Hit an air block
                    {
                        obj = worldBlocks[0];
                    }

                    int thisID = tagMap[obj.tag];
                    //Debug.Log(thisID);
                    adjacencies[thisID].occurances++; //We have seen an instances of the object
                    adjacencies[thisID].name = objectMap[thisID].name;
                    Vector3 testPos;

                    testPos = findBoundsInputLayer(minVec, maxVec, incVec + Vector3Int.up); //Would need to be multiplied by the block size
                    
                    generateOverlapSpheres(testPos, 0, thisID);

                    //Down
                    testPos = findBoundsInputLayer(minVec, maxVec, incVec + Vector3Int.down); //Would need to be multiplied by the block size
                    generateOverlapSpheres(testPos, 1, thisID);

                    //Left
                    testPos = findBoundsInputLayer(minVec, maxVec, incVec + Vector3Int.left); //Would need to be multiplied by the block size
                    generateOverlapSpheres(testPos, 2, thisID);

                    //Right
                    testPos = findBoundsInputLayer(minVec, maxVec, incVec + Vector3Int.right); //Would need to be multiplied by the block size
                    generateOverlapSpheres(testPos, 3, thisID);

                    //Forward
                    testPos = findBoundsInputLayer(minVec, maxVec, incVec + Vector3Int.forward); //Would need to be multiplied by the block size
                    generateOverlapSpheres(testPos, 4, thisID);

                    //Back
                    testPos = findBoundsInputLayer(minVec, maxVec, incVec + Vector3Int.back); //Would need to be multiplied by the block size
                    generateOverlapSpheres(testPos, 5, thisID);

                    adjacencies[thisID].convertToSet();

                }
            }
        }
    }

    void generateOverlapSpheres(Vector3 incVec, int dir, int id) 
    {
        Collider[] hit = Physics.OverlapSphere(incVec, .1f, world); //By taking out world, then I can test for "air" vs. wrappiong
        if (hit.Length >= 1)
        {
            adjacencies[id].addAdjacencies(dir, tagMap[hit[0].gameObject.tag]); //Adds the objectID of the hit block
        }
        else  //I've interesected with the manager and that's it
        {
            adjacencies[id].addAdjacencies(dir, tagMap["AirTag"]); //Adds an instance of "AIR" //TODO (Maybe) calculate adjacencies of air?
        }
    }

// Update is called once per frame
    void Update()
    {
        if (Input.GetKeyDown(KeyCode.Space))
        {
            RunWFC();
        }
    }

}
