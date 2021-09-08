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
    private Dictionary<int, Info> adjacencies; //Mapping of ID to Info Object (which contains the adjacency matrix)
    private MapCoordinate[,] worldMap = new MapCoordinate[400,400]; //Contains an Arry of Map Coordinates (which have an x, a y, and a list of possible features to spawn) // Will eventually also contain a Z
    private Dictionary<int, GameObject> objectMap;

    private int DIRECTION = 4; //4 for now while I work in 2 dimensions

    public class Info
    {
        public Dictionary<int, List<int>> directionMapping;
        public int internalID;

        public Info(int inc) { internalID = inc; }

        public void addAdjacencies(int direction, int inc) { directionMapping[direction].Add(inc); }

        public List<int> getAdjacenciesByDirection(int direction) { return directionMapping[direction]; }

    }

    public class MapCoordinate
    {
        private Vector2 coords;
        private List<int> possibilites;
        private bool hasFixed = false;

        public MapCoordinate(int incX, int incY) { coords = new Vector2(incX, incY); }

        public MapCoordinate(int incX, int incY, List<int> possible)
        {
            coords = new Vector2(incX, incY);
            possibilites = possible;
        }

        public void updatePossibilites(List<int> incPossibilites) { possibilites = incPossibilites; }

        public List<int> getPossibilities() { return possibilites; }

        public Vector2 getCoords() { return coords; }

        public void Fix() { hasFixed = true; }

        public bool isFixed() { return hasFixed; }

    }

    // Start is called before the first frame update
    void Start()
    {

        //Setup the default adjacencies
        int newObjID = 0;
        objectMap[newObjID] = tile; //Brute force add the object to the tile mapping
        Info tmp = new Info(newObjID);
        for(int i = 0; i < DIRECTION; i++)
        {
            tmp.addAdjacencies(i, 0); //Sets the adjacencies to all be zero for now, should eventually go and find the adjacencies
        }

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
        Vector2 randStart = new Vector2(Random.Range(0, width), Random.Range(0, height));
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

            foreach(MapCoordinate curr in worldMap)
            {
                if (curr.isFixed()) //If we have fixed this node, percolate those changes to surrounding nodes
                {
                    //Test and update all directions
                    if (inBounds(curr.getCoords() + Vector2.down))
                    {
                        MapCoordinate tmp = worldMap[(int)curr.getCoords().x, (int)curr.getCoords().y - 1];

                    }
                    if (inBounds(curr.getCoords() + Vector2.up))
                    {

                    }
                    if (inBounds(curr.getCoords() + Vector2.left))
                    {

                    }
                    if (inBounds(curr.getCoords() + Vector2.right))
                    {

                    }

                }
            }

        }
    }

    bool inBounds(Vector2 incVec)
    {
        if(incVec.x > width || incVec.x < 0)
        {
            return false;
        }
        if(incVec.y > height || incVec.y < 0)
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
