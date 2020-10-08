package de.welthungerhilfe.cgm.scanner.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import de.welthungerhilfe.cgm.scanner.R;

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

    public ContextMenuDialog(Context context, Item[] items, ContextMenuSelection callback) {
        ListAdapter adapter = new ArrayAdapter<Item>(context, R.layout.lv_item_with_icon,
                android.R.id.text1, items) {

            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater vi = LayoutInflater.from(context);
                    v = vi.inflate(R.layout.lv_item_with_icon, null);
                }

                TextView textView = (TextView)v.findViewById(R.id.text);
                textView.setText(items[position].text);
                View iconView = v.findViewById(R.id.icon);
                iconView.setBackgroundResource(items[position].icon);
                return v;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setAdapter(adapter, (dialog, which) -> {
            dialog.dismiss();
            callback.onSelected(which);
        });
        builder.show();
    }
}
