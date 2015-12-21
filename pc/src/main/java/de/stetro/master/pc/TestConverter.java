package de.stetro.master.pc;

import org.rajawali3d.math.vector.Vector3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import de.stetro.master.pc.rendering.MeshTree;

public class TestConverter {


    public static void main(String[] args) {
        File file = new File("/home/stetro/Downloads/pc.xyz");
        List<Vector3> points = new ArrayList<>();
        System.out.print("loading pointcloud ...");
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] floats = line.split(" ");
                points.add(new Vector3(Float.parseFloat(floats[0]), Float.parseFloat(floats[1]), Float.parseFloat(floats[2])));
            }
            System.out.print(" DONE - loaded " + points.size() + " vertices \n");
            MeshTree m = new MeshTree(new Vector3(-20, -20, -20), 40.0, 11, 4);
            System.out.print("Storing points into meshtree ...");
            m.putPoints(points);
            int storedPoints = m.getNewPointsCount();
            System.out.print(" DONE inserted " + storedPoints + " points \nDo polygon reconstruction ...");
            m.updateMesh();
            Stack<Vector3> polygons = new Stack<>();
            m.fillPolygons(polygons);
            System.out.print(" DONE got " + polygons.size() + " polygon vertices - \nGenerate PLY file ...");
            File result = new File("/home/stetro/Downloads/result.ply");
            FileOutputStream fileOutputStream = new FileOutputStream(result);
            fileOutputStream.write("ply\n".getBytes());
            fileOutputStream.write("format ascii 1.0 \n".getBytes());
            fileOutputStream.write(("element vertex " + polygons.size() + "\n").getBytes());
            fileOutputStream.write("property float32 x\n".getBytes());
            fileOutputStream.write("property float32 y\n".getBytes());
            fileOutputStream.write("property float32 z\n".getBytes());
            fileOutputStream.write(("element face " + (polygons.size() / 3) + "\n").getBytes());
            fileOutputStream.write("property list uint8 int32 vertex_index\n".getBytes());
            fileOutputStream.write("end_header\n".getBytes());

            for (Vector3 vertex : polygons) {
                fileOutputStream.write((String.valueOf(vertex.x) + " " + String.valueOf(vertex.y) + " " + String.valueOf(vertex.z) + "\n").getBytes());
            }
            for (int i = 0; i < polygons.size() / 3; i++) {
                fileOutputStream.write(("3 " + i * 3 + " " + (i * 3 + 1) + " " + (i * 3 + 2) + "\n").getBytes());
            }
            fileOutputStream.close();
            System.out.println(" DONE");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
