package de.stetro.master.chisel.util;


import android.content.Context;
import android.os.AsyncTask;

import com.afollestad.materialdialogs.MaterialDialog;

import org.rajawali3d.math.vector.Vector3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.stetro.master.chisel.R;


public class PLYExporter {
    private final Context context;
    private List<Vector3> mesh;

    private MaterialDialog dialog;

    public PLYExporter(Context context, List<Vector3> mesh) {
        this.context = context;
        this.mesh = mesh;

        dialog = new MaterialDialog.Builder(context)
                .title(R.string.calculating_mesh)
                .content(R.string.please_wait)
                .progress(false, 100, true)
                .widgetColor(context.getResources().getColor(R.color.colorPrimary))
                .build();
        dialog.setProgress(0);
    }


    public void export() {
        new ExportAsyncTask().execute(mesh);
    }

    private class ExportAsyncTask extends AsyncTask<List<Vector3>, Integer, Void> {

        @Override
        protected Void doInBackground(List<Vector3>... params) {
            if (params.length == 0) {
                return null;
            }
            List<Vector3> pointCollection = params[0];
            Format formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.GERMAN);
            final String fileName = "mesh-" + formatter.format(new Date()) + ".ply";
            final File file = new File(context.getExternalFilesDir(null), fileName);
            try {
                FileOutputStream os = new FileOutputStream(file);

                int size = pointCollection.size();

                os.write("ply\n".getBytes());
                os.write("format ascii 1.0 \n".getBytes());
                os.write(("element vertex " + mesh.size() + "\n").getBytes());
                os.write("property float32 x\n".getBytes());
                os.write("property float32 y\n".getBytes());
                os.write("property float32 z\n".getBytes());
                os.write(("element face " + (mesh.size() / 3) + "\n").getBytes());
                os.write("property list uint8 int32 vertex_index\n".getBytes());
                os.write("end_header\n".getBytes());

                dialog.setMaxProgress(size);

                for (int i = 0; i < mesh.size(); i++) {
                    Vector3 vertex = mesh.get(i);
                    os.write((String.valueOf(vertex.x) + " " + String.valueOf(vertex.y) + " " + String.valueOf(vertex.z) + "\n").getBytes());
                    if (i % 200 == 0) {
                        dialog.setProgress(i / 2);
                    }
                }
                for (int i = 0; i < mesh.size() / 3; i++) {
                    os.write(("3 " + i * 3 + " " + (i * 3 + 1) + " " + (i * 3 + 2) + "\n").getBytes());
                    if (i % 100 == 0) {
                        dialog.setProgress(size / 2 + i * 3);
                    }
                }

                os.close();
            } catch (IOException e) {
                dialog.setCancelable(true);
            } finally {
                dialog.dismiss();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            dialog.dismiss();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            dialog.setProgress(values[0]);
        }
    }
}
