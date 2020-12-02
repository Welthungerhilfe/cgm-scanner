/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.welthungerhilfe.cgm.scanner.utils;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;

public class InternalStorageContentProvider extends ContentProvider {
    public static final Uri CONTENT_URI = Uri.parse("content://de.welthungerhilfe.cgm.scanner.qrconsent/");
    private static final HashMap<String, String> MIME_TYPES = new HashMap<String, String>();

    private static final String TEMP_PHOTO_FILE_NAME = "temp_photo.jpg";

    static {
        MIME_TYPES.put(".jpg", "image/jpeg");
        MIME_TYPES.put(".jpeg", "image/jpeg");
    }

    @Override
    public boolean onCreate() {
        try {
            File mFile = new File(getContext().getFilesDir(), TEMP_PHOTO_FILE_NAME);
            if(!mFile.exists()) {
                mFile.createNewFile();
                getContext().getContentResolver().notifyChange(CONTENT_URI, null);
            }
            return (true);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        String path = uri.toString();
        for (String extension : MIME_TYPES.keySet()) {
            if (path.endsWith(extension)) {
                return (MIME_TYPES.get(extension));
            }
        }
        return (null);
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File f = new File(getContext().getFilesDir(), TEMP_PHOTO_FILE_NAME);
        if (f.exists()) {
            return (ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_WRITE));
        }
        throw new FileNotFoundException(uri.getPath());
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
