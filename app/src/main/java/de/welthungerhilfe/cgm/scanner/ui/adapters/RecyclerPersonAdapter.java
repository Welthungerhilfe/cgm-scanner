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

import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.DataFormat;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.PersonListViewModel;
import de.welthungerhilfe.cgm.scanner.ui.activities.BaseActivity;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ConfirmDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ContactSupportDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ContextMenuDialog;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;

public class RecyclerPersonAdapter extends RecyclerView.Adapter<RecyclerPersonAdapter.ViewHolder> {
    private BaseActivity context;
    private List<Person> personList = new ArrayList<>();

    private OnPersonDetail personDetailListener;

    private RecyclerView recyclerData;
    private MeasureRepository repository;
    private SessionManager session;
    private PersonListViewModel viewModel;

    public RecyclerPersonAdapter(BaseActivity ctx, RecyclerView recycler, PersonListViewModel model) {
        context = ctx;
        recyclerData = recycler;
        viewModel = model;

        repository = MeasureRepository.getInstance(ctx);
        session = new SessionManager(context);
    }

    @Override
    public RecyclerPersonAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_data, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerPersonAdapter.ViewHolder holder, int position) {
        Person person = getItem(position);
        holder.txtName.setText(person.getFullName());


        if(person.getSex().equals("male")){
            holder.iv_child.setImageResource(R.drawable.ic_boy_black);
        }else {
            holder.iv_child.setImageResource(R.drawable.ic_girl_black);

        }
        if(person.isBelongs_to_rst()) {
            holder.tv_age.setVisibility(View.VISIBLE);
            holder.tv_age.setText(DataFormat.calculateAge(person.getBirthday()));
            holder.tv_label_weight.setVisibility(View.GONE);
            holder.tv_lable_height.setVisibility(View.GONE);
            holder.txtHeight.setVisibility(View.GONE);
            holder.txtWeight.setVisibility(View.GONE);
          //  holder.txtLastMeasure.setVisibility(View.VISIBLE);
        }else {
           // holder.txtLastMeasure.setVisibility(View.GONE);
            holder.tv_age.setVisibility(View.GONE);
            holder.tv_label_weight.setVisibility(View.VISIBLE);
            holder.tv_lable_height.setVisibility(View.VISIBLE);
            holder.txtHeight.setVisibility(View.VISIBLE);
            holder.txtWeight.setVisibility(View.VISIBLE);
            repository.getPersonLastMeasureLiveData(person.getId()).observe(context, measure -> {
                SessionManager sessionManager = new SessionManager(context);
                if (sessionManager.getStdTestQrCode() != null) {
                    holder.txtHeight.setText(R.string.field_concealed);
                    holder.txtWeight.setText(R.string.field_concealed);
                } else {
                    double height = 0;
                    double weight = 0;
                    if (measure != null) {
                        height = measure.getHeight();
                        weight = measure.getWeight();
                    }
                    holder.txtHeight.setText(String.format(Locale.getDefault(), "%.1f", height) + context.getString(R.string.unit_cm));
                    holder.txtWeight.setText(String.format(Locale.getDefault(), "%.3f", weight) + context.getString(R.string.unit_kg));

                }
            });
        }
        if (personDetailListener != null) {
            holder.bindPersonDetail(position);
        }
        if (person.isDenied()) {
            holder.ll_measure.setVisibility(View.GONE);
            holder.ll_denied.setVisibility(View.VISIBLE);
        } else {
            holder.ll_measure.setVisibility(View.VISIBLE);
            holder.ll_denied.setVisibility(View.GONE);
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

    public void deletePerson(int position) {
        if (!AppController.getInstance().isAdmin()) {
            notifyItemChanged(position);
            Snackbar.make(recyclerData, R.string.permission_delete, Snackbar.LENGTH_LONG).show();
        } else {
            Person person = getItem(position);

            ConfirmDialog dialog = new ConfirmDialog(context);
            dialog.setMessage(R.string.delete_person);
            dialog.setConfirmListener(result -> {
                if (result) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            person.setDeleted(true);
                            person.setDeletedBy(session.getUserEmail());
                            person.setTimestamp(AppController.getInstance().getUniversalTimestamp());

                            return null;
                        }

                        public void onPostExecute(Void result) {
                            viewModel.updatePerson(person);
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else {
                    notifyItemChanged(position);
                }
            });
            dialog.show();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public RelativeLayout rytItem;

        public TextView txtName;
        public TextView txtWeight;
        public TextView txtHeight;
        private View contextMenu;
        private LinearLayout ll_measure, ll_denied;
        ImageView iv_child;
        TextView tv_age, txtLastMeasure;
        TextView tv_lable_height, tv_label_weight;

        ViewHolder(View itemView) {
            super(itemView);

            rytItem = itemView.findViewById(R.id.rytItem);
            iv_child = itemView.findViewById(R.id.iv_child);
            txtName = itemView.findViewById(R.id.txtName);
            txtWeight = itemView.findViewById(R.id.txtWeight);
            txtHeight = itemView.findViewById(R.id.txtHeight);
            contextMenu = itemView.findViewById(R.id.contextMenuButton);
            ll_denied = itemView.findViewById(R.id.ll_denied);
            ll_measure = itemView.findViewById(R.id.ll_measure);
            tv_age = itemView.findViewById(R.id.tv_age);
            txtLastMeasure = itemView.findViewById(R.id.txtLastMeasure);
            tv_lable_height = itemView.findViewById(R.id.tv_lable_height);
            tv_label_weight = itemView.findViewById(R.id.tv_lable_weight);
        }

        void bindPersonDetail(int position) {
            rytItem.setOnClickListener(view -> personDetailListener.onPersonDetail(getItem(position)));
            rytItem.setOnLongClickListener(view -> {
                showContextMenu(position);
                return true;
            });
            contextMenu.setOnClickListener(view -> showContextMenu(position));
        }
    }

    public interface OnPersonDetail {
        void onPersonDetail(Person person);
    }

    private void showContextMenu(int position) {
        Person person = getItem(position);
        new ContextMenuDialog(context, new ContextMenuDialog.Item[]{
                new ContextMenuDialog.Item(R.string.show_details, R.drawable.ic_details),
                new ContextMenuDialog.Item(R.string.delete_data, R.drawable.ic_delete),
                new ContextMenuDialog.Item(R.string.contact_support, R.drawable.ic_contact_support),
        }, which -> {
            switch (which) {
                case 0:
                    personDetailListener.onPersonDetail(getItem(position));
                    break;
                case 1:
                    deletePerson(position);
                    break;
                case 2:
                    ContactSupportDialog.show(context, "person " + person.getQrcode(), "personID:" + person.getId());
                    break;
            }
        });
    }
}
