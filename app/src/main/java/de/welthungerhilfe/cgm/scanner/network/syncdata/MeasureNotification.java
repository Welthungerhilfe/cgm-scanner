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
package de.welthungerhilfe.cgm.scanner.network.syncdata;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;

import java.util.HashMap;
import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.ui.activities.MainActivity;
import de.welthungerhilfe.cgm.scanner.utils.DataFormat;

public class MeasureNotification {

    private static final String CHANNEL_ID = "CGM_Result_Generation_Notification";
    private static final int NOTIFICATION_ID = 2030;

    private static final HashMap<String, MeasureNotification> notifications = new HashMap<>();
    private static boolean updated = false;

    private Float height;
    private Float weight;

    public static MeasureNotification get(String qrCode) {
        if (qrCode == null) {
            return null;
        }
        if (!notifications.containsKey(qrCode)) {
            notifications.put(qrCode, new MeasureNotification());
        }
        return notifications.get(qrCode);
    }

    public boolean hasHeight() {
        return height != null;
    }

    public boolean hasWeight() {
        return weight != null;
    }

    public void setHeight(float value) {
        height = value;
        updated = true;
    }

    public void setWeight(float value) {
        weight = value;
        updated = true;
    }

    public static void dismissNotification(Context context) {
        try {
            notifications.clear();
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTIFICATION_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        updated = false;
    }

    public static void showNotification(Context context) {
        if (!updated) {
            return;
        }
        updated = false;

        Notification.Builder notificationBuilder;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "CGM Result Generation", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);

            notificationBuilder = new Notification.Builder(context, CHANNEL_ID);

        } else {
            notificationBuilder = new Notification.Builder(context);
        }

        notificationBuilder = notificationBuilder
                .setSmallIcon(R.drawable.icon_notif)
                .setContentIntent(pendingIntent)
                .setPriority(Notification.PRIORITY_MAX)
                .setVibrate(new long[]{0, 500, 1000})
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setOngoing(false);

        long timestamp = System.currentTimeMillis();
        String title = String.format(context.getString(R.string.result_generation_at) + " %s", DataFormat.timestamp(context, DataFormat.TimestampFormat.DATE, timestamp));
        StringBuilder text = new StringBuilder();

        boolean valid = false;
        for (String qrCode : notifications.keySet()) {
            MeasureNotification n = get(qrCode);
            if (n.hasHeight()) {
                if (valid) {
                    text.append("\n");
                }
                text.append(context.getString(R.string.label_height)).append(" ");
                text.append(context.getString(R.string.result_for)).append(" ");
                text.append(String.format(Locale.US, "%s : %.2f%s", qrCode, n.height, "cm"));
                valid = true;
            }
            if (n.hasWeight()) {
                if (valid) {
                    text.append("\n");
                }
                text.append(context.getString(R.string.label_weight)).append(" ");
                text.append(context.getString(R.string.result_for)).append(" ");
                text.append(String.format(Locale.US, "%s : %.2f%s", qrCode, n.weight, "kg"));
                valid = true;
            }
        }

        if (valid) {
            Notification.BigTextStyle style = new Notification.BigTextStyle();
            style.bigText(text.toString());

            notificationBuilder.setStyle(style);
            notificationBuilder.setContentTitle(title);
            notificationBuilder.setContentText(text.toString());
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
    }
}
