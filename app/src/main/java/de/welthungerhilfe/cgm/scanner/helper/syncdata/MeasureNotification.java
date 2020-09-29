package de.welthungerhilfe.cgm.scanner.helper.syncdata;

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

    private static String lastNotification = "";

    private Float height;
    private Float weight;

    public boolean hasHeight() {
        return height != null;
    }

    public boolean hasWeight() {
        return weight != null;
    }

    public void setHeight(float value) {
        height = value;
    }

    public void setWeight(float value) {
        weight = value;
    }

    public static void dismissNotification(Context context) {
        try {
            lastNotification = "";
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTIFICATION_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showNotification(Context context, HashMap<String, MeasureNotification> notifications) {
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

        for (String qrCode : notifications.keySet()) {
            MeasureNotification n = notifications.get(qrCode);
            if (n.hasHeight()) {
                if (text.length() > 0) {
                    text.append("\n");
                }
                text.append(context.getString(R.string.label_height).replace(":", "")).append(" ");
                text.append(context.getString(R.string.result_for)).append(" ");
                text.append(String.format(Locale.US, "%s : %.2f%s", qrCode, n.height, "cm"));
            }
            if (n.hasWeight()) {
                if (text.length() > 0) {
                    text.append("\n");
                }
                text.append(context.getString(R.string.label_weight).replace(":", "")).append(" ");
                text.append(context.getString(R.string.result_for)).append(" ");
                text.append(String.format(Locale.US, "%s : %.2f%s", qrCode, n.weight, "kg"));
            }
        }

        if (text.length() > 0) {
            if (lastNotification.length() > 0) {
                lastNotification += "\n";
            }
            String finalText = lastNotification + text.toString();
            notificationBuilder.setContentTitle(title);
            notificationBuilder.setStyle(new Notification.BigTextStyle().bigText(finalText));
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
            lastNotification = finalText;
        }
    }
}
