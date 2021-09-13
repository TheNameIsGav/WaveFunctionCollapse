using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System.Linq;

public class ManagerScript : MonoBehaviour
{
    public int width;
    public int height;
    public GameObject tile;

    private bool doOnce;

    //Possibilites are worldMap ints that Might be a tile
    //Adjacencies are the objectID's that are allowed to be next to the current object
    private Dictionary<int, Tiles> adjacencies = new Dictionary<int, Tiles>(); //Mapping of ID to Info Object (which contains the adjacency matrix)
    private MapCoordinate[,] worldMap; //Contains an Arry of Map Coordinates (which have an x, a y, and a list of possible features to spawn) // Will eventually also contain a Z
    private Dictionary<int, GameObject> objectMap = new Dictionary<int, GameObject>(); //update this to work with converting prefabs to gameobjects

    private int DIRECTION = 4; //4 for now while I work in 2 dimensions

    public class Tiles
    {
        public Dictionary<int, List<int>> directionMapping = new Dictionary<int, List<int>>();

        //Keep track of occurances and the prefabtype of the object
        public int occurances;
        public int prefabType; //Prefab type is the keyword of the prefab

        public Tiles(int inc) { 
            prefabType = inc;
            directionMapping.Add(0, new List<int>()); //Dirty way of adding the directions to the dictionary
            directionMapping.Add(1, new List<int>());
            directionMapping.Add(2, new List<int>());
            directionMapping.Add(3, new List<int>());
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
        private Vector2Int coords;
        private List<int> possibilites;
        private bool hasFixed = false;

        public MapCoordinate() { coords = new Vector2Int(0, 0); }
        public MapCoordinate(int incX, int incY) { coords = new Vector2Int(incX, incY); }

        public MapCoordinate(int incX, int incY, List<int> possible)
        {
            coords = new Vector2Int(incX, incY);
            possibilites = possible;
        }

        public void updatePossibilites(List<int> incPossibilites) { possibilites = incPossibilites; }

        public List<int> getPossibilities() { return possibilites; }

        public Vector2Int getCoords() { return coords; }

        public void Fix() { hasFixed = true; }

        public bool isFixed() { return hasFixed; }

    }

    // Start is called before the first frame update
    void Start()
    {
        //Setup the default adjacencies
        worldMap = new MapCoordinate[width, height];
        Debug.Log(worldMap);
        int newObjID = 0;
        objectMap[0] = Instantiate(tile, new Vector3(), Quaternion.identity); //Brute force add the object to the tile mapping
        Tiles tmp = new Tiles(newObjID);
        for(int i = 0; i < DIRECTION; i++)
        {
            tmp.addAdjacencies(i, 0); //Sets the adjacencies to all be zero for now, should eventually go and find the adjacencies
        }

        tmp.convertToSet();

        adjacencies[newObjID] = tmp; //Adds the tmp object to the adjacencies
        doOnce = true;

        //Setup the world map
        for(int i = 0; i < width; i++)
        {
            for(int j = 0; j < height; j++)
            {
                worldMap[i,j] = (new MapCoordinate(i, j, adjacencies.Keys.ToList())); //Sets every world point to every possible adjacency
            }
        }
    }

    void StartWFC()
    {
        bool running = true;

        //Pick and set the start node to be one of the random possibilites that exists at that node
        Vector2Int randStart = new Vector2Int(Random.Range(0, width), Random.Range(0, height));
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

        int itr = 0;
        while (running)
        {
            Debug.Log("Running through WFC @ iteration " + itr.ToString());
            itr++;
            running = false;

            foreach(MapCoordinate curr in worldMap)
            {
                if (curr.isFixed()) //If we have fixed this node, percolate those changes to surrounding nodes
                {
                    //Test and update all directions
                    //Transforms the current position with the correct direction and then asks if that is in bounds
                    Vector2Int transformed = curr.getCoords() + Vector2Int.down;
                    if (inBounds(transformed))
                    { 
                        updateSuperposition(transformed); //if it is, update that superposition
                    }

                    transformed = curr.getCoords() + Vector2Int.up;
                    if (inBounds(transformed))
                    {
                        updateSuperposition(transformed);
                    }

                    transformed = curr.getCoords() + Vector2Int.left;
                    if (inBounds(transformed))
                    {
                        updateSuperposition(transformed);
                    }

                    transformed = curr.getCoords() + Vector2Int.right;
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
            Object.Instantiate(objectMap[curr.getPossibilities()[0]], new Vector3(curr.getCoords().x, 0, curr.getCoords().y), Quaternion.identity);
        }
    }

    void updateSuperposition(Vector2Int inc) //Takes a map coordinate and compiles the intersection of all fixed nodes around it
    {
        List<int> myPossible = objectMap.Keys.ToList(); //Get's every possible tile

        Vector2Int transformed = inc + Vector2Int.down;
        if (inBounds(transformed))
        {
            if (worldMap[transformed.x, transformed.y].isFixed()) { 
                myPossible.Intersect(worldMap[transformed.x, transformed.y].getPossibilities()); 
            }
        }

        transformed = inc + Vector2Int.up;
        if (inBounds(transformed))
        {
            if (worldMap[transformed.x, transformed.y].isFixed())
            {
                myPossible.Intersect(worldMap[transformed.x, transformed.y].getPossibilities());
            }
        }

        transformed = inc + Vector2Int.left;
        if (inBounds(transformed))
        {
            if (worldMap[transformed.x, transformed.y].isFixed())
            {
                myPossible.Intersect(worldMap[transformed.x, transformed.y].getPossibilities());
            }
        }

        transformed = inc + Vector2Int.right;
        if (inBounds(transformed))
        {
            if (worldMap[transformed.x, transformed.y].isFixed())
            {
                myPossible.Intersect(worldMap[transformed.x, transformed.y].getPossibilities());
            }
        }
    }


    bool inBounds(Vector2Int incVec)
    {
        if(incVec.x >= width || incVec.x < 0)
        {
            return false;
        }
        if(incVec.y >= height || incVec.y < 0)
        {
            return false;
        }
        return true;
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
