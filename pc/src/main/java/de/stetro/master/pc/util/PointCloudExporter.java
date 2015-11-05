package de.stetro.master.pc.util;


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

import de.stetro.master.pc.R;
import de.stetro.master.pc.rendering.PointCollection;

public class PointCloudExporter {
    private final Context context;
    private final PointCollection pointCollection;
    private MaterialDialog dialog;

    public PointCloudExporter(Context context, PointCollection pointCollection) {
        this.context = context;
        this.pointCollection = pointCollection;
        dialog = new MaterialDialog.Builder(context)
                .title(R.string.exporting_pointcloud)
                .content(R.string.please_wait)
                .progress(false, 100, true)
                .widgetColor(context.getResources().getColor(R.color.colorPrimary))
                .build();
        dialog.setProgress(0);
    }


    public void export() {
        new ExportAsyncTask().execute(pointCollection);
    }

    private class ExportAsyncTask extends AsyncTask<PointCollection, Integer, Void> {

        @Override
        protected Void doInBackground(PointCollection... params) {
            if (params.length == 0) {
                return null;
            }
            PointCollection pointCollection = params[0];
            Format formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.GERMAN);
            final String fileName = "pointcloud-" + formatter.format(new Date()) + ".pts";
            final File file = new File(context.getExternalFilesDir(null), fileName);
            try {
                OutputStream os = new FileOutputStream(file);
                int size = pointCollection.getCount();
                dialog.setMaxProgress(size);
                FloatBuffer floatBuffer = pointCollection.getBuffer();
                floatBuffer.rewind();
                int progressCounter = 0;
                for (int i = 0; i < size; i++) {
                    String row = String.valueOf(floatBuffer.get()) + " " + String.valueOf(floatBuffer.get()) + " " + String.valueOf(floatBuffer.get()) + "\n";
                    os.write(row.getBytes());
                    progressCounter++;
                    if (progressCounter % (int) ((double) size / 200.0) == 0) {
                        publishProgress(progressCounter);
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
