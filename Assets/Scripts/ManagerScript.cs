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
    private Dictionary<int, Info> adjacencies; //Mapping of ID to Info Object (which contains the adjacency matrix)
    private List<MapCoordinate> worldMap; //Contains a List of Map Coordinates (which have an x, a y, and a list of possible features to spawn)
    private Dictionary<int, GameObject> objectMap;

    private int DIRECTION = 6;

    public class Info
    {
        public Dictionary<int, List<int>> directionMapping;
        public int internalID;

        public Info()
        {
            internalID = -1;
        }

        public Info(int inc)
        {
            internalID = inc;
        }

        public void addPossibility(int direction, int inc)
        {
            directionMapping[direction].Add(inc);
        }

        public List<int> getPossibilitiesFromDirection(int direction)
        {
            return directionMapping[direction];
        }

    }

    public class MapCoordinate
    {
        private Vector2 coords;
        private List<int> possibilites;

        public MapCoordinate()
        {
            coords = new Vector2(-1, -1);
        }

        public MapCoordinate(int incX, int incY)
        {
            coords = new Vector2(incX, incY);
        }

        public MapCoordinate(int incX, int incY, List<int> possible)
        {
            coords = new Vector2(incX, incY);
            possibilites = possible;
        }

        public void updatePossibilites(List<int> incPossibilites)
        {
            possibilites = incPossibilites;
        }

        public Vector2 getCoords()
        {
            return coords;
        }
    }

    // Start is called before the first frame update
    void Start()
    {

        //Setup the default adjacencies
        int newObjID = 0;
        objectMap[newObjID] = tile; //Brute force add the object to the tile mapping
        Info tmp = new Info(01);
        for(int i = 0; i < DIRECTION; i++)
        {
            tmp.addPossibility(i, 0); //Sets the adjacencies to all be zero for now, should eventually go and find the adjacencies
        }

        adjacencies[newObjID] = tmp; //Adds the tmp object to the adjacencies
        doOnce = true;

        //Setup the world map
        for(int i = 0; i < width; i++)
        {
            for(int j = 0; j < height; j++)
            {
                worldMap.Add(new MapCoordinate(i, j, adjacencies.Keys.ToList())); //Sets every world point to every possible adjacency
            }
        }
    }

    void StartWFC()
    {
        bool running = true;
        Vector2 randStart = new Vector2(Random.Range(0, width), Random.Range(0, height));

        int itr = 0;
        while (running)
        {
            Debug.Log("Running through WFC @ iteration " + itr.ToString());
            itr++;



        }
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
