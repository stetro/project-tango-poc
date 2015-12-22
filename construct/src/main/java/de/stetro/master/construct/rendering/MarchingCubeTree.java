package de.stetro.master.construct.rendering;

import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.stetro.master.construct.calc.OctTree;
import de.stetro.master.construct.marchingcubes.Cube;
import de.stetro.master.construct.marchingcubes.HVector3;


public class MarchingCubeTree extends OctTree {
    private static int[][] vertexNeighbours = new int[][]{
            {1, 2, 5, 6},
            {1, 5},
            {2, 6},
            {5, 6},
            {5},
            {6},
            {1, 2},
            {1},
            {2},

            {0, 3, 4, 7},
            {0, 4},
            {3, 7},
            {4, 7},
            {4},
            {7},
            {0, 3},
            {0},
            {4},

            {0, 1, 4, 5},
            {2, 3, 6, 7},
            {4, 5, 6, 7},
            {4, 5},
            {6, 7},
            {0, 1, 2, 3},
            {0, 1},
            {2, 3},

    };

    public MarchingCubeTree(Vector3 position, double range, int depth) {
        super(position, range, depth);
    }


    public List<Cube> getCubes(int depthLimit) {
        HashMap<HVector3, Cube> cubes = new HashMap<>();
        getCubes(cubes, depthLimit, this);
        return new ArrayList<>(cubes.values());
    }


    private void getCubes(HashMap<HVector3, Cube> cubes, int depthLimit, OctTree root) {
        if (depth == depthLimit) {
            fillCubes(new HVector3(position.x, position.y, position.z), cubes, depthLimit, root, true);
        } else {
            for (OctTree child : children) {
                MarchingCubeTree mcChild = (MarchingCubeTree) child;
                if (mcChild != null) {
                    mcChild.getCubes(cubes, depthLimit, root);
                }
            }
        }
    }

    private void fillCubes(HVector3 pos, HashMap<HVector3, Cube> cubes, int depthLimit, OctTree root, boolean center) {
        if (!cubes.containsKey(pos)) {
            HVector3[] potentialNeighbours = new HVector3[]{
                    new HVector3(pos.x + range, pos.y, pos.z),
                    new HVector3(pos.x + range, pos.y - range, pos.z),
                    new HVector3(pos.x + range, pos.y + range, pos.z),
                    new HVector3(pos.x + range, pos.y, pos.z + range),
                    new HVector3(pos.x + range, pos.y - range, pos.z + range),
                    new HVector3(pos.x + range, pos.y + range, pos.z + range),
                    new HVector3(pos.x + range, pos.y, pos.z - range),
                    new HVector3(pos.x + range, pos.y - range, pos.z - range),
                    new HVector3(pos.x + range, pos.y + range, pos.z - range),

                    new HVector3(pos.x - range, pos.y, pos.z),
                    new HVector3(pos.x - range, pos.y - range, pos.z),
                    new HVector3(pos.x - range, pos.y + range, pos.z),
                    new HVector3(pos.x - range, pos.y, pos.z + range),
                    new HVector3(pos.x - range, pos.y - range, pos.z + range),
                    new HVector3(pos.x - range, pos.y + range, pos.z + range),
                    new HVector3(pos.x - range, pos.y, pos.z - range),
                    new HVector3(pos.x - range, pos.y - range, pos.z - range),
                    new HVector3(pos.x - range, pos.y + range, pos.z - range),

                    new HVector3(pos.x, pos.y - range, pos.z),
                    new HVector3(pos.x, pos.y + range, pos.z),
                    new HVector3(pos.x, pos.y, pos.z + range),
                    new HVector3(pos.x, pos.y - range, pos.z + range),
                    new HVector3(pos.x, pos.y + range, pos.z + range),
                    new HVector3(pos.x, pos.y, pos.z - range),
                    new HVector3(pos.x, pos.y - range, pos.z - range),
                    new HVector3(pos.x, pos.y + range, pos.z - range),
            };

            if (center) {
                for (HVector3 potentialNeighbour : potentialNeighbours) {
                    fillCubes(potentialNeighbour, cubes, depthLimit, root, false);
                }
            } else {
                boolean[] weightedVertices = new boolean[8];
                for (int i = 0; i < potentialNeighbours.length; i++) {
                    if (root.exists(potentialNeighbours[i], depthLimit)) {
                        for (int j = 0; j < vertexNeighbours[i].length; j++) {
                            weightedVertices[vertexNeighbours[i][j]] = true;
                        }
                    }
                }
                cubes.put(pos, new Cube(new Vector3(pos.x, pos.y, pos.z), range, weightedVertices));
            }
        }
    }


}
