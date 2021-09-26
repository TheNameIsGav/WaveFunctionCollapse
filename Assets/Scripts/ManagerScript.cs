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

    //Possibilites are worldMap ints that Might be a tile
    //Adjacencies are the objectID's that are allowed to be next to the current object
    private Dictionary<int, Tiles> adjacencies = new Dictionary<int, Tiles>(); //objectID to Tile Object
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

        public void Fix() { hasFixed = true; thisTile = possibilites[0]; }

        public bool isFixed() { return hasFixed; }

        public void setTile(int tile) { thisTile = tile; }

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
        //New code for generating adjacency matrix
        box = GetComponent<BoxCollider>();
        //Setup the default adjacencies
        worldMap = new MapCoordinate[width, height, length];

        for (int i = 0; i < transform.childCount; i++) //"Building Blocks" are children of the manager
        {
            objectMap.Add(i, transform.GetChild(i).gameObject); //Add all possible tiles to the objectMap with their ID
            tagMap.Add(transform.GetChild(i).tag, i); //Map the correct tag to the object ID for use later.
            adjacencies.Add(i, new Tiles(i)); //Add a 0 occurance version of the tile to the map for later use
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
        WFC();
    }

    private void printAdjacencyMatrix()
    {
        for(int i = 0; i < adjacencies.Keys.Count; i++)
        {
            Debug.Log(adjacencies[i].ToString());
        }
    }

    void WFC() //Selects a random start coordinate and picks one of the valid building blocks, as every map coordinate to begin with has every tile
    {
        MapCoordinate curr = worldMap[(Random.Range(0, width)), Random.Range(0, height), Random.Range(0, length)];
         
        List<int> thesePossibilies = curr.getPossibilities();

        List<int> selected = new List<int>();
        selected.Add(thesePossibilies[Random.Range(0, thesePossibilies.Count())]);

        curr.updatePossibilites(selected);

        curr.Fix();//Fix it in place

    }

    void RunWFC()
    {

        MapCoordinate curr = worldMap[(Random.Range(0, width)), Random.Range(0, height), Random.Range(0, length)]; //select random map coordinate
        while (curr.isFixed())
        {
            curr = worldMap[(Random.Range(0, width)), Random.Range(0, height), Random.Range(0, length)]; //select until we get a non-fixed node (i.e. a superposition)
        }

        foreach (MapCoordinate mc in worldMap) //Find map coordinate with the least possibilites
        {
            if (!mc.isFixed()) //if current map coordinate isn't fixed
            {
                if (mc.getPossibilities().Count < curr.getPossibilities().Count) //if it has less possibilites than the random one we selected
                {
                    curr = mc; //asign it 
                }
            }
        }

        // Add wrapping
        //If I go right, I snap to 0, if I go left I snap to width
        //If I go forward I snap to 0, if I go back I snap to length
        //Should I do vertical wrapping?
        //If we get here, then look at all our adjacencies and see if they are fixed, if they are, then we update ourselves
        Vector3Int trans = inBounds(curr.getCoords() + Vector3Int.up);
        updateFromAdjacent(trans, curr, 0);
        
        trans = inBounds(curr.getCoords() + Vector3Int.down);
        updateFromAdjacent(trans, curr, 1);

        trans = inBounds(curr.getCoords() + Vector3Int.left);
        updateFromAdjacent(trans, curr, 2);

        trans = inBounds(curr.getCoords() + Vector3Int.right);
        updateFromAdjacent(trans, curr, 3);

        trans = inBounds(curr.getCoords() + Vector3Int.forward);
        updateFromAdjacent(trans, curr, 4);

        trans = inBounds(curr.getCoords() + Vector3Int.back);
        updateFromAdjacent(trans, curr, 5);


        if (curr.getPossibilities().Count == 0) //If anything has 0 possibilites, then we fail
        {
            Debug.Log("Impossible Pattern");
            return;
        }

        if (curr.getPossibilities().Count == 1) //If we are at one possibility, then we fix (Is this correct???????????)
        {
            curr.Fix();
        }
    }

    void updateFromAdjacent(Vector3Int trans, MapCoordinate curr, int dir) //Updates mapcoordinates possibilites with intersection of adjacent fixed nodes
    {
        if (worldMap[trans.x, trans.y, trans.z].isFixed())
        {
            List<int> fixedTileAdjacencies = adjacencies[worldMap[trans.x, trans.y, trans.z].thisTile].getAdjacenciesByDirection(0); //Gets adjacency for that tile type in that direction
            List<int> tmp = curr.getPossibilities();
            tmp.Intersect(fixedTileAdjacencies);
            curr.updatePossibilites(tmp);
        }
    }

    Vector3Int inBounds(Vector3Int incVec) //Proper usage of this function: call on a transformed vector and will return the correct map coordinate to modify
    {
        if(incVec.x >= width)
        {
            return new Vector3Int(0, incVec.y, incVec.z);
        }
        if(incVec.x < 0)
        {
            return new Vector3Int(width, incVec.y, incVec.z);
        }

        if (incVec.y >= height)
        {
            return new Vector3Int(incVec.x, 0, incVec.z);
        }
        if(incVec.y < 0)
        {
            return new Vector3Int(incVec.x, height , incVec.z);
        }

        if (incVec.z >= length)
        {
            return new Vector3Int(incVec.x, incVec.y, 0);
        }
        if(incVec.z < 0)
        {
            return new Vector3Int(incVec.x, incVec.y, length);
        }

        return incVec;
    } //if this returns false then we wrap, maybe change to int to indicate which direction it returned false in;

    void buildAdjacencyMatrix()
    {
        //Blocks are 1x1 and are spaced 1 unit apart
        Collider[] tmp = Physics.OverlapBox(transform.position, box.bounds.extents);

        foreach (Collider c in tmp)
        {
            GameObject obj = c.gameObject;
            if (obj.CompareTag("Manager"))
            {
                break;
            }
            int thisID = tagMap[obj.tag];
            adjacencies[thisID].occurances++; //We have seen an instances of the object
            adjacencies[thisID].name = objectMap[thisID].name;
            Vector3 testPos;

            //Up
            testPos = obj.transform.position + Vector3.up; //Would need to be multiplied by the block size
            generateOverlapSpheres(testPos, 0, thisID);

            //Down
            testPos = obj.transform.position + Vector3.down; //Would need to be multiplied by the block size
            generateOverlapSpheres(testPos, 1, thisID);

            //Left
            testPos = obj.transform.position + Vector3.left; //Would need to be multiplied by the block size
            generateOverlapSpheres(testPos, 2, thisID);

            //Right
            testPos = obj.transform.position + Vector3.right; //Would need to be multiplied by the block size
            generateOverlapSpheres(testPos, 3, thisID);

            //Forward
            testPos = obj.transform.position + Vector3.forward; //Would need to be multiplied by the block size
            generateOverlapSpheres(testPos, 4, thisID);

            //Back
            testPos = obj.transform.position + Vector3.back; //Would need to be multiplied by the block size
            generateOverlapSpheres(testPos, 5, thisID);

            adjacencies[thisID].convertToSet();
        }

    }

    void generateOverlapSpheres(Vector3 incVec, int dir, int id) 
    {
        Collider[] hit = Physics.OverlapSphere(incVec, .1f); //By taking out world, then I can test for "air" vs. wrappiong
        if (hit.Length > 1)
        {
            adjacencies[id].addAdjacencies(dir, tagMap[hit[0].gameObject.tag]); //Adds the objectID of the hit block
        }
        else if (hit.Length == 1) //I've interesected with the manager and that's it
        {
            adjacencies[id].addAdjacencies(dir, -1);
        } else 
        {
            //Debug.Log(gameObject.GetComponent<BoxCollider>().size); //.size returns the size of the object in all directions, should get half of each direction for calulations (except y)

            switch (dir)
            {
                case 0: //Up
                    incVec = new Vector3(incVec.x, gameObject.GetComponent<BoxCollider>().size.y, incVec.z);
                    break;
                case 1: //Down
                    incVec = new Vector3(incVec.x, gameObject.GetComponent<BoxCollider>().size.y + incVec.y, incVec.z);
                    break;
                case 2: //Left
                    incVec = new Vector3(gameObject.GetComponent<BoxCollider>().size.x, incVec.y, incVec.z);
                    break;
                case 3: //Right
                    incVec = new Vector3(gameObject.GetComponent<BoxCollider>().size.x + incVec.x, incVec.y, incVec.z);
                    break;
                case 4: //Forward
                    incVec = new Vector3(incVec.x, incVec.y, gameObject.GetComponent<BoxCollider>().size.z);
                    break;
                case 5: //Back
                    incVec = new Vector3(incVec.x, incVec.y, gameObject.GetComponent<BoxCollider>().size.z + incVec.z);
                    break;
            }
            //Handle wrapping
            //build another function that gets the bounds of the collider and then moves "inwards" by one step?
        }

    }

// Update is called once per frame
    void Update()
    {

        if (Input.GetKey(KeyCode.Space))
        {
            RunWFC();
        }
    }












    //unused currently
    void updateSuperposition(Vector3Int inc) //Takes a map coordinate and compiles the intersection of all fixed nodes around it
    {
        List<int> myPossible = objectMap.Keys.ToList(); //Get's every possible tile

        Vector3Int transformed = inc + Vector3Int.up;
        testSupers(transformed, myPossible, 0);

        transformed = inc + Vector3Int.down;
        testSupers(transformed, myPossible, 1);

        transformed = inc + Vector3Int.left;
        testSupers(transformed, myPossible, 2);

        transformed = inc + Vector3Int.right;
        testSupers(transformed, myPossible, 3);

        transformed = inc + Vector3Int.forward;
        testSupers(transformed, myPossible, 4);

        transformed = inc + Vector3Int.back;
        testSupers(transformed, myPossible, 5);
    }

    //unused currently
    void testSupers(Vector3Int trans, List<int> poss, int dir) //Trans will ALWAYS be a world coord due to wrapping
    {
        if (worldMap[trans.x, trans.y, trans.z].isFixed())
        {
            List<int> fixedTileAdjacencies = adjacencies[worldMap[trans.x, trans.y, trans.z].thisTile].getAdjacenciesByDirection(dir); //Gets adjacency for that tile type in that direction
            poss.Intersect(fixedTileAdjacencies);
        }
    }
}
