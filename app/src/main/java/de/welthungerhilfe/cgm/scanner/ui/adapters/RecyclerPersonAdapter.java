/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com> for Welthungerhilfe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.welthungerhilfe.cgm.scanner.ui.adapters;

import android.annotation.SuppressLint;
import androidx.lifecycle.LifecycleOwner;
import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;

public class RecyclerPersonAdapter extends RecyclerView.Adapter<RecyclerPersonAdapter.ViewHolder> {
    private Context context;
    private List<Person> personList = new ArrayList<>();

    private OnPersonDetail personDetailListener;

    private MeasureRepository repository;

    public RecyclerPersonAdapter(Context ctx) {
        context = ctx;

        repository = MeasureRepository.getInstance(ctx);
    }

    @Override
    public RecyclerPersonAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_data, parent, false);

        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(RecyclerPersonAdapter.ViewHolder holder, int position) {
        Person person = getItem(position);

        holder.txtName.setText(person.getName() + " " + person.getSurname());

        repository.getPersonLastMeasureLiveData(person.getId()).observe((LifecycleOwner) context, measure -> {
            if (measure != null) {
                holder.txtWeight.setText(String.format("%.3f", measure.getWeight()));
                holder.txtHeight.setText(String.format("%.2f", measure.getHeight()));
            } else {
                holder.txtWeight.setText("0.0");
                holder.txtHeight.setText("0.0");
            }
        });

        if (personDetailListener != null) {
            holder.bindPersonDetail(person);
        }
    }

    @Override
    public int getItemCount() {
        return personList.size();
    }

    public Person getItem(int position) {
        return personList.get(position);
    }

    public void setPersonDetailListener(OnPersonDetail listener) {
        personDetailListener = listener;
    }

    public void clear() {
        personList.clear();
        notifyDataSetChanged();
    }

    public void addPersons(List<Person> pList) {
        int oIndex = personList.size();
        personList.addAll(pList);
        notifyItemRangeInserted(oIndex, pList.size());
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public RelativeLayout rytItem;

        public TextView txtName;
        public TextView txtWeight;
        public TextView txtHeight;

        ViewHolder(View itemView) {
            super(itemView);

            rytItem = itemView.findViewById(R.id.rytItem);

            txtName = itemView.findViewById(R.id.txtName);
            txtWeight = itemView.findViewById(R.id.txtWeight);
            txtHeight = itemView.findViewById(R.id.txtHeight);
        }

        void bindPersonDetail(final Person person) {
            rytItem.setOnClickListener(view -> personDetailListener.onPersonDetail(person));
        }
    }

    public interface OnPersonDetail {
        void onPersonDetail(Person person);
    }
}
