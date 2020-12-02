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
