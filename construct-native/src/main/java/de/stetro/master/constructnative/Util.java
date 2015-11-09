package de.stetro.master.constructnative;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class Util {

    public static boolean hasPermission(Context context, String permissionType) {
        Uri uri = Uri.parse("content://com.google.atap.tango.PermissionStatusProvider/" + permissionType);
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        boolean hasPermission = cursor != null;
        if (hasPermission) {
            cursor.close();
        }
        return hasPermission;
    }
}
