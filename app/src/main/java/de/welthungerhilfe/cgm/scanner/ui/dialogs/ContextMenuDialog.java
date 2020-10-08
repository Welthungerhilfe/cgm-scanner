package de.welthungerhilfe.cgm.scanner.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class ContextMenuDialog {

    public static class Item {
        public int text;
        public int icon;

        public Item(int text, int icon) {
            this.text = text;
            this.icon = icon;
        }
    }

    public interface ContextMenuSelection {
        void onSelected(int which);
    }

    public ContextMenuDialog(Context context, String title, Item[] items, ContextMenuSelection callback) {
        ArrayAdapter adapter = new ArrayAdapter<Item>(context, android.R.layout.select_dialog_item,
                android.R.id.text1, items) {

            public View getView(int position, View convertView, ViewGroup parent) {
                int size = Utils.dpToPx(20, context);
                Drawable dr = context.getDrawable(items[position].icon);
                Bitmap bitmap = ((BitmapDrawable) dr).getBitmap();
                bitmap = Bitmap.createScaledBitmap(bitmap, size, size, true);
                Drawable icon = new BitmapDrawable(context.getResources(), bitmap);

                View v = super.getView(position, convertView, parent);
                TextView tv = (TextView)v.findViewById(android.R.id.text1);
                tv.setText(items[position].text);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                tv.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
                tv.setCompoundDrawablePadding(Utils.dpToPx(10, context));
                return v;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title + "\n");
        builder.setAdapter(adapter, (dialog, which) -> {
            dialog.dismiss();
            callback.onSelected(which);
        });
        builder.show();
    }
}
