using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System.Linq;

public class ManagerScript : MonoBehaviour
{
    public int width;
    public int height;
    public int length;
    public LayerMask world;
    public GameObject tile;
    public GameObject[] worldBlocks;

    private bool doOnce = true;

    //Possibilites are worldMap ints that Might be a tile
    //Adjacencies are the objectID's that are allowed to be next to the current object
    private Dictionary<int, Tiles> adjacencies = new Dictionary<int, Tiles>(); //objectID to Tile Object
    private MapCoordinate[,,] worldMap; //Contains an Arry of Map Coordinates (which have an x, a y, and a list of possible features to spawn) // Will eventually also contain a Z
    private Dictionary<int, GameObject> objectMap = new Dictionary<int, GameObject>(); //maps object ID to gameObject
    private Dictionary<string, int> tagMap = new Dictionary<string, int>(); //Maps tag to object ID
    
    private BoxCollider box;

    enum DIRECTION //reference enum
    {
        UP = 0,
        DOWN = 1,
        LEFT = 2,
        RIGHT = 3,
        FORWARD = 4,
        BACK = 5
    }

    public class Tiles
    {
        public Dictionary<int, List<int>> directionMapping = new Dictionary<int, List<int>>();

        //Keep track of occurances and the prefabtype of the object
        public int occurances = 0;
        public int prefabType; //Prefab type is the keyword of the prefab

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

    }

    public class MapCoordinate
    {
        private Vector3Int coords;
        private List<int> possibilites;
        private bool hasFixed = false;
        private int thisTile;

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
            return "";
        }

    }

    // Start is called before the first frame update
    void Start()
    {
        //New code for generating adjacency matrix
        box = GetComponent<BoxCollider>();
        //Setup the default adjacencies
        worldMap = new MapCoordinate[width, height, length];

        for(int i = 0; i < transform.childCount; i++) //"Building Blocks" are children of the manager
        {
            objectMap.Add(i, transform.GetChild(i).gameObject); //Add all possible tiles to the objectMap with their ID
            tagMap.Add(transform.GetChild(i).tag, i); //Map the correct tag to the object ID for use later.
            adjacencies.Add(i, new Tiles(i)); //Add a 0 occurance version of the tile to the map for later use
        }

        buildAdjacencyMatrix();
        //End new code

        //Old Code for generating dummy tiles
        /*worldMap = new MapCoordinate[width, height, length];
        Debug.Log(worldMap);
        int newObjID = 0;
        objectMap[0] = Instantiate(tile, new Vector3(), Quaternion.identity); //Brute force add the object to the tile mapping
        Tiles tmp = new Tiles(newObjID);
        for (int i = 0; i < 6; i++)
        {
            tmp.addAdjacencies(i, 0); //Sets the adjacencies to all be zero for now, should eventually go and find the adjacencies
        }

        tmp.convertToSet();

        adjacencies[newObjID] = tmp; //Adds the tmp object to the adjacencies
        doOnce = true;*/
        //End old code


        //Setup the world map
        for (int x = 0; x < width; x++)
        {
            for(int y = 0; y < height; y++)
            {
                for (int z = 0; z < length; z++)
                {
                    worldMap[x, y, z] = (new MapCoordinate(x, y, z, adjacencies.Keys.ToList())); //Sets every world point to every possible adjacency
                }
            }
        }
    }

    void StartWFC()
    {
        Vector3Int randStart = new Vector3Int(Random.Range(0, width), Random.Range(0, height), Random.Range(0, length));
        foreach (MapCoordinate curr in worldMap)
        {
            if (curr.getCoords() == randStart)
            {
                curr.Fix(); //Fix it in place
                List<int> thesePossibilies = curr.getPossibilities();

                List<int> selected = new List<int>();
                selected.Add(thesePossibilies[Random.Range(0, thesePossibilies.Count())]);

                curr.updatePossibilites(selected);
            }
        }

        bool running = true;

        int itr = 0;
        while (running)
        {
            Debug.Log("Running through WFC @ iteration " + itr.ToString());
            itr++;
            running = false;
            if(itr > 1000)
            {
                break;
            }

            foreach(MapCoordinate curr in worldMap)
            {//Adjust this to pick from lowest entropy
                //Take this out of the while loop so I can control things
                //Add counter to see if something has 0 elements possible and do something
                // Add wrapping
                if (curr.isFixed()) //If we have fixed this node, percolate those changes to surrounding nodes
                {
                    //Test and update all directions
                    //Transforms the current position with the correct direction and then asks if that is in bounds
                    Vector3Int transformed = curr.getCoords() + Vector3Int.down;
                    if (inBounds(transformed))
                    { 
                        updateSuperposition(transformed); //if it is, update that superposition
                    }

                    transformed = curr.getCoords() + Vector3Int.up;
                    if (inBounds(transformed))
                    {
                        updateSuperposition(transformed);
                    }

                    transformed = curr.getCoords() + Vector3Int.left;
                    if (inBounds(transformed))
                    {
                        updateSuperposition(transformed);
                    }

                    transformed = curr.getCoords() + Vector3Int.right;
                    if (inBounds(transformed))
                    {
                        updateSuperposition(transformed);
                    }

                    transformed = curr.getCoords() + Vector3Int.forward;
                    if (inBounds(transformed))
                    {
                        updateSuperposition(transformed);
                    }

                    transformed = curr.getCoords() + Vector3Int.back;
                    if (inBounds(transformed))
                    {
                        updateSuperposition(transformed);
                    }

                } else { //If any node isn't fixed, we're not done running yet
                    running = true;
                }

                if (curr.getPossibilities().Count == 1)
                {
                    curr.Fix();
                }
            }

        }

        foreach (MapCoordinate curr in worldMap)
        {
            Object.Instantiate(objectMap[curr.getPossibilities()[0]], new Vector3(curr.getCoords().x, curr.getCoords().y, curr.getCoords().z), Quaternion.identity);
        }
    }

    void updateSuperposition(Vector3Int inc) //Takes a map coordinate and compiles the intersection of all fixed nodes around it
    {
        List<int> myPossible = objectMap.Keys.ToList(); //Get's every possible tile

        Vector3Int transformed = inc + Vector3Int.down;
        if (inBounds(transformed))
        {
            if (worldMap[transformed.x, transformed.y, transformed.z].isFixed()) { 
                myPossible.Intersect(worldMap[transformed.x, transformed.y, transformed.z].getPossibilities()); 
            }
        }

        transformed = inc + Vector3Int.up;
        if (inBounds(transformed))
        {
            if (worldMap[transformed.x, transformed.y, transformed.z].isFixed())
            {
                myPossible.Intersect(worldMap[transformed.x, transformed.y, transformed.z].getPossibilities());
            }
        }

        transformed = inc + Vector3Int.left;
        if (inBounds(transformed))
        {
            if (worldMap[transformed.x, transformed.y, transformed.z].isFixed())
            {
                myPossible.Intersect(worldMap[transformed.x, transformed.y, transformed.z].getPossibilities());
            }
        }

        transformed = inc + Vector3Int.right;
        if (inBounds(transformed))
        {
            if (worldMap[transformed.x, transformed.y, transformed.z].isFixed())
            {
                myPossible.Intersect(worldMap[transformed.x, transformed.y, transformed.z].getPossibilities());
            }
        }

        transformed = inc + Vector3Int.forward;
        if (inBounds(transformed))
        {
            if (worldMap[transformed.x, transformed.y, transformed.z].isFixed())
            {
                myPossible.Intersect(worldMap[transformed.x, transformed.y, transformed.z].getPossibilities());
            }
        }

        transformed = inc + Vector3Int.back;
        if (inBounds(transformed))
        {
            if (worldMap[transformed.x, transformed.y, transformed.z].isFixed())
            {
                myPossible.Intersect(worldMap[transformed.x, transformed.y, transformed.z].getPossibilities());
            }
        }
    }

    bool inBounds(Vector3Int incVec)
    {
        if(incVec.x >= width || incVec.x < 0)
        {
            return false;
        }
        if(incVec.y >= height || incVec.y < 0)
        {
            return false;
        }
        if(incVec.z >= length || incVec.z < 0)
        {
            return false;
        }
        return true;
    }

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
            int thisId = tagMap[obj.tag];
            adjacencies[thisId].occurances++; //We have seen an instances of the object
            Vector3 testPos;
            Collider[] hit;

            //Now go find the correct adjacenceies

            //Up
            testPos = obj.transform.position + Vector3.up; //Would need to be multiplied by the block size
            hit = Physics.OverlapSphere(testPos, .1f, world);
            if(hit.Length > 0)
            {
                adjacencies[thisId].addAdjacencies(0, tagMap[hit[0].gameObject.tag]); //Adds the objectID of the hit block
            } else
            {
                adjacencies[thisId].addAdjacencies(0, -1); //Adds an air block
            }

            //Down
            testPos = obj.transform.position + Vector3.down; //Would need to be multiplied by the block size
            hit = Physics.OverlapSphere(testPos, .1f, world);
            if (hit.Length > 0)
            {
                adjacencies[thisId].addAdjacencies(1, tagMap[hit[0].gameObject.tag]); //Adds the objectID of the hit block
            }
            else
            {
                adjacencies[thisId].addAdjacencies(1, -1); //Adds an air block
            }

            //Left
            testPos = obj.transform.position + Vector3.left; //Would need to be multiplied by the block size
            hit = Physics.OverlapSphere(testPos, .1f, world);
            if (hit.Length > 0)
            {
                Debug.Log(adjacencies[thisId]);
                Debug.Log(hit[0].gameObject.tag);
                adjacencies[thisId].addAdjacencies(2, tagMap[hit[0].gameObject.tag]); //Adds the objectID of the hit block
            }
            else
            {
                adjacencies[thisId].addAdjacencies(2, -1); //Adds an air block
            }

            //Right
            testPos = obj.transform.position + Vector3.right; //Would need to be multiplied by the block size
            hit = Physics.OverlapSphere(testPos, .1f, world);
            if (hit.Length > 0)
            {
                adjacencies[thisId].addAdjacencies(3, tagMap[hit[0].gameObject.tag]); //Adds the objectID of the hit block
            }
            else
            {
                adjacencies[thisId].addAdjacencies(3, -1); //Adds an air block
            }

            //Forward
            testPos = obj.transform.position + Vector3.forward; //Would need to be multiplied by the block size
            hit = Physics.OverlapSphere(testPos, .1f, world);
            if (hit.Length > 0)
            {
                adjacencies[thisId].addAdjacencies(4, tagMap[hit[0].gameObject.tag]); //Adds the objectID of the hit block
            }
            else
            {
                adjacencies[thisId].addAdjacencies(4, -1); //Adds an air block
            }

            //Back
            testPos = obj.transform.position + Vector3.back; //Would need to be multiplied by the block size
            hit = Physics.OverlapSphere(testPos, .1f, world);
            if (hit.Length > 0)
            {
                adjacencies[thisId].addAdjacencies(5, tagMap[hit[0].gameObject.tag]); //Adds the objectID of the hit block
            }
            else
            {
                adjacencies[thisId].addAdjacencies(5, -1); //Adds an air block
            }
        }

        Debug.Log(adjacencies);
    }

    // Update is called once per frame
    void Update()
    {

        if (Input.GetKeyDown(KeyCode.Space))
        {
            if (doOnce)
            {
                doOnce = false;
                StartWFC();
            }
        }
    }
}
