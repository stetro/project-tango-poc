package de.stetro.master.masterprototype.util;


import android.content.Context;
import android.os.AsyncTask;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.stetro.master.masterprototype.R;
import de.stetro.master.masterprototype.calc.OctTree;

public class PointCloudExporter {
    private final Context context;
    private final OctTree octTree;
    private MaterialDialog dialog;

    public PointCloudExporter(Context context, OctTree octTree) {
        this.context = context;
        this.octTree = octTree;
        dialog = new MaterialDialog.Builder(context)
                .title(R.string.exporting_pointcloud)
                .content(R.string.please_wait)
                .progress(false, 100, true)
                .widgetColor(context.getResources().getColor(R.color.colorPrimary))
                .build();
        dialog.setProgress(0);
    }


    public void export() {
        new ExportAsyncTask().execute(octTree);
    }

    private class ExportAsyncTask extends AsyncTask<OctTree, Integer, Void> {

        @Override
        protected Void doInBackground(OctTree... params) {
            if (params.length == 0) {
                return null;
            }
            OctTree octTree = params[0];
            Format formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.GERMAN);
            final String fileName = "pointcloud-" + formatter.format(new Date()) + ".pts";
            final File file = new File(context.getExternalFilesDir(null), fileName);
            try {
                OutputStream os = new FileOutputStream(file);
                int size = octTree.getSize();
                FloatBuffer floatBuffer = FloatBuffer.allocate(size * 3);
                octTree.fill(floatBuffer);
                floatBuffer.rewind();
                int progressCounter = 0;
                while (floatBuffer.hasRemaining()) {
                    String row = String.valueOf(floatBuffer.get()) + " " + String.valueOf(floatBuffer.get()) + " " + String.valueOf(floatBuffer.get()) + "\n";
                    os.write(row.getBytes());
                    progressCounter++;
                    if (progressCounter % (int) ((double) size / 100.0) == 0) {
                        publishProgress((int) (((double) progressCounter / (double) size) * 100.0));
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
