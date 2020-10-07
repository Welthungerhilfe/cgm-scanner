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

import androidx.lifecycle.LifecycleOwner;

import androidx.recyclerview.widget.RecyclerView;

import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.PersonListViewModel;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.ui.activities.BaseActivity;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ConfirmDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ContactSupportDialog;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class RecyclerPersonAdapter extends RecyclerView.Adapter<RecyclerPersonAdapter.ViewHolder> {
    private BaseActivity context;
    private List<Person> personList = new ArrayList<>();

    private OnPersonDetail personDetailListener;

    private RecyclerView recyclerData;
    private MeasureRepository repository;
    private SessionManager session;
    private PersonListViewModel viewModel;

    public RecyclerPersonAdapter(BaseActivity ctx, RecyclerView recycler, SessionManager manager, PersonListViewModel model) {
        context = ctx;
        recyclerData = recycler;
        session = manager;
        viewModel = model;

        repository = MeasureRepository.getInstance(ctx);
    }

    @Override
    public RecyclerPersonAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_data, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerPersonAdapter.ViewHolder holder, int position) {
        Person person = getItem(position);
        String name = person.getName() + " " + person.getSurname();
        holder.txtName.setText(name);

        repository.getPersonLastMeasureLiveData(person.getId()).observe((LifecycleOwner) context, measure -> {
            if (measure != null) {
                holder.txtWeight.setText(String.format(Locale.getDefault(), "%.3f", measure.getWeight()));
                holder.txtHeight.setText(String.format(Locale.getDefault(),"%.2f", measure.getHeight()));
            } else {
                holder.txtWeight.setText("0.0");
                holder.txtHeight.setText("0.0");
            }
        });

        if (personDetailListener != null) {
            holder.bindPersonDetail(position);
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
                            person.setTimestamp(Utils.getUniversalTimestamp());

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

        ViewHolder(View itemView) {
            super(itemView);

            rytItem = itemView.findViewById(R.id.rytItem);

            txtName = itemView.findViewById(R.id.txtName);
            txtWeight = itemView.findViewById(R.id.txtWeight);
            txtHeight = itemView.findViewById(R.id.txtHeight);
            contextMenu = itemView.findViewById(R.id.contextMenuButton);
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
        CharSequence[] items = {
                context.getString(R.string.show_details),
                context.getString(R.string.delete_data),
                context.getString(R.string.contact_support)
        };

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
        builder.setTitle(R.string.select_action);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setItems(items, (dialog, which) -> {
            dialog.dismiss();
            switch (which) {
                case 0:
                    personDetailListener.onPersonDetail(getItem(position));
                    break;
                case 1:
                    deletePerson(position);
                    break;
                case 2:
                    ContactSupportDialog.show(context, "main screen feedback");
                    break;
            }
        });
        builder.show();
    }
}
