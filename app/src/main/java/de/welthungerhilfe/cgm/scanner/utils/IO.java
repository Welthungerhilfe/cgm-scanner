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

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class IO {

    private static final String TAG = IO.class.getName();

    private static final int BUFFER = 80000;

    public static void copyFile(String inputPath, String outputPath) {
        try {
            //create output directory if it doesn't exist
            File dir = new File(outputPath).getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }

            InputStream in = new FileInputStream(inputPath);
            OutputStream out = new FileOutputStream(outputPath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();

            // write the output file (You have now copied the file)
            out.flush();
            out.close();
            Log.d(TAG, inputPath + " copied to " + outputPath);

        } catch (Exception e) {
            Log.e(TAG, inputPath + " copying to " + outputPath + " failed");
            Log.e(TAG, e.getMessage());
        }
    }

    public static void delete(File f) {
        if (f.exists()) {
            if (f.isDirectory()) {
                File[] files = f.listFiles();
                if ((files != null) && (files.length > 0)) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            delete(f);
                        }
                        if (file.delete()) {
                            Log.d(TAG, file.getAbsolutePath() + " deleted");
                        } else {
                            Log.e(TAG, file.getAbsolutePath() + " deleting failed");
                        }
                    }
                }
            }
            if (f.delete()) {
                Log.d(TAG, f.getAbsolutePath() + " deleted");
            } else {
                Log.e(TAG, f.getAbsolutePath() + " deleting failed");
            }
        }
    }

    public static void move(File source, File destination) {
        if (source.exists()) {

            //ensure the parent directory exists
            File dir = destination.getParentFile();
            if (!dir.exists()) {
                if (dir.mkdirs()) {
                    Log.d(TAG, dir.getAbsolutePath() + " created");
                } else {
                    Log.e(TAG, dir.getAbsolutePath() + " creation failed");
                }
            }

            //move directory
            if (source.isDirectory()) {
                if (!source.renameTo(destination)) {
                    File[] files = source.listFiles();
                    if ((files != null) && (files.length > 0)) {
                        for (File f : files) {
                            move(f, new File(destination, f.getName()));
                        }
                    }
                    delete(source);
                } else {
                    Log.d(TAG, source.getAbsolutePath() + " moved to " + destination.getAbsolutePath());
                }
            }

            //move file
            else if (!source.renameTo(destination)) {
                copyFile(source.getAbsolutePath(), destination.getAbsolutePath());
                delete(source);
            } else {
                Log.d(TAG, source.getAbsolutePath() + " moved to " + destination.getAbsolutePath());
            }
        }
    }

    public static String getMD5(String filePath) {
        String base64Digest = "";
        try {
            InputStream input = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            MessageDigest md5Hash = MessageDigest.getInstance("MD5");
            int numRead = 0;
            while (numRead != -1) {
                numRead = input.read(buffer);
                if (numRead > 0) {
                    md5Hash.update(buffer, 0, numRead);
                }
            }
            input.close();
            byte[] md5Bytes = md5Hash.digest();
            base64Digest = Base64.encodeToString(md5Bytes, Base64.DEFAULT);

        } catch (Throwable t) {
            t.printStackTrace();
        }
        return base64Digest;
    }


    public static void takeScreenshot(Activity context, File pngFile) {
        try {
            // create bitmap screen capture
            View v1 = context.getWindow().getDecorView().getRootView();
            v1.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);

            FileOutputStream outputStream = new FileOutputStream(pngFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void unzip(String _zipFile, String _targetLocation) {

        //create target location folder if not exist
        dirChecker(_targetLocation);

        try {
            FileInputStream fin = new FileInputStream(_zipFile);
            ZipInputStream zin = new ZipInputStream(fin);
            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {

                //create dir if required while unzipping
                if (ze.isDirectory()) {
                    dirChecker(ze.getName());
                } else {
                    FileOutputStream fout = new FileOutputStream(_targetLocation + ze.getName());
                    for (int c = zin.read(); c != -1; c = zin.read()) {
                        fout.write(c);
                    }

                    zin.closeEntry();
                    fout.close();
                }

            }
            zin.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void zip(String[] _files, String zipFileName) {
        try {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(zipFileName);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));
            byte[] data = new byte[BUFFER];

            for (String file : _files) {
                Log.v("Compress", "Adding: " + file);
                FileInputStream fi = new FileInputStream(file);
                origin = new BufferedInputStream(fi, BUFFER);

                ZipEntry entry = new ZipEntry(file.substring(file.lastIndexOf("/") + 1));
                out.putNextEntry(entry);
                int count;

                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }

            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void dirChecker(String dir) {
        File f = new File(dir);
        if (!f.isDirectory()) {
            f.mkdirs();
        }
    }
}
