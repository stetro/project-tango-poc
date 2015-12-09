package de.stetro.master.constructnative.util;


import android.os.AsyncTask;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;

import org.rajawali3d.math.vector.Vector3;

import java.util.List;
import java.util.Stack;

import de.stetro.master.constructnative.JNIInterface;
import de.stetro.master.constructnative.R;
import de.stetro.master.constructnative.calc.OctTree;
import de.stetro.master.constructnative.rendering.PointCloudARRenderer;
import de.stetro.master.constructnative.rendering.PointCollection;
import de.stetro.master.constructnative.ui.MainActivity;

public class ReconstructionBuilder {
    private final static String tag = ReconstructionBuilder.class.getSimpleName();
    private final PointCollection collectedPoints;
    private final PointCloudARRenderer renderer;
    private MaterialDialog dialog;

    public ReconstructionBuilder(MainActivity context, PointCollection collectedPoints, PointCloudARRenderer renderer) {
        this.collectedPoints = collectedPoints;
        this.renderer = renderer;
        dialog = new MaterialDialog.Builder(context)
                .title(R.string.calculating_mesh)
                .content(R.string.please_wait)
                .progress(false, 100, true)
                .widgetColor(context.getResources().getColor(R.color.text_primary))
                .build();
        dialog.setProgress(0);
    }

    public void reconstruct() {
        new ReconstructionAsyncTask().execute(collectedPoints);
    }

    private class ReconstructionAsyncTask extends AsyncTask<PointCollection, Integer, Stack<Vector3>> {

        @SuppressWarnings("UnnecessaryLocalVariable")
        @Override
        protected Stack<Vector3> doInBackground(PointCollection... params) {
            if (params.length == 0) {
                return null;
            }
            PointCollection pointCollection = params[0];
            Stack<Vector3> stack = createJniReconstruction(pointCollection);
            return stack;
        }

        private Stack<Vector3> createJniReconstruction(PointCollection pointCollection) {
            Stack<Vector3> stack = new Stack<>();
            OctTree tree = pointCollection.getOctTree();
            int size = tree.getSize();
            float[] array = new float[size * 3];
            List<Vector3> pointList = tree.getPointList();
            for (int i = 0; i < size; i++) {
                array[i * 3] = (float) pointList.get(i).x;
                array[i * 3 + 1] = (float) pointList.get(i).y;
                array[i * 3 + 2] = (float) pointList.get(i).z;
            }
            float[] faces = JNIInterface.reconstructPiecewisePlanes(array);
            Log.e("Java", "got " + faces.length / 9 + " faces");
            for (int i = 0; i < (faces.length / 3); i++) {
                stack.add(new Vector3(faces[i * 3], faces[i * 3 + 1], faces[i * 3 + 2]));
            }

            Log.e("Java", "finished with " + (stack.size() / 3) + " faces");
            return stack;
        }


        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected void onPostExecute(Stack<Vector3> points) {
            dialog.dismiss();
            if (points != null) {
                renderer.setFaces(points);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            dialog.setProgress(values[0]);
        }

    }
}
