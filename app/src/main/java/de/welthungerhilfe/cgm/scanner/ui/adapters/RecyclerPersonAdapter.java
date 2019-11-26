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
import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnMeasureLoad;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class RecyclerPersonAdapter extends RecyclerView.Adapter<RecyclerPersonAdapter.ViewHolder> implements Filterable {
    private Context context;
    private List<Person> personList = new ArrayList<>();
    private List<Person> filteredList = new ArrayList<>();
    private int lastPosition = -1;

    private SessionManager session;

    private int sortType = 0; // 0 : All, 1 : date, 2 : location, 3 : wasting, 4 : stunting;
    private ArrayList<Integer> filters = new ArrayList<>();
    private long startDate, endDate;
    private Loc currentLoc;
    private int radius;
    private CharSequence query;

    private PersonFilter personFilter = new PersonFilter();

    private OnPersonDetail personDetailListener;

    private MeasureRepository repository;

    public RecyclerPersonAdapter(Context ctx) {
        context = ctx;

        repository = MeasureRepository.getInstance(ctx);
        session = new SessionManager(ctx);
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
                holder.txtWeight.setText(Double.toString(measure.getWeight()));
                holder.txtHeight.setText(Double.toString(measure.getHeight()));
            } else {
                holder.txtWeight.setText("0.0");
                holder.txtHeight.setText("0.0");
            }
        });

        if (personDetailListener != null) {
            holder.bindPersonDetail(person);
        }

        //setAnimation(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return personList.size();
    }

    public Person getItem(int position) {
        return personList.get(position);
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    public void setPersonDetailListener(OnPersonDetail listener) {
        personDetailListener = listener;
    }

    public void resetData(ArrayList<Person> personList) {
        this.personList = personList;
        getFilter().filter("");
    }

    public void clear() {
        personList.clear();
        notifyDataSetChanged();
    }

    public void resetData(List<Person> list) {
        /*
        if (personList.size() > 0) {
            if (list.get(0).getCreated() > personList.get(0).getCreated()) {
                personList.add(0, list.get(0));
                notifyItemInserted(0);
            }
        } else {
            this.personList = list;
            notifyDataSetChanged();
        }
        */

        this.personList = list;
        getFilter().filter("");

    }

    public void addPerson(Person person) {
        personList.add(person);
        getFilter().filter("");
    }

    public void setDateFilter(long start, long end) {
        startDate = start;
        endDate = end;
    }

    public void setLocationFilter(Loc loc, int r) {
        currentLoc = loc;
        radius = r;
    }

    public void setSearchQuery(CharSequence query) {
        this.query = query;
    }

    @Override
    public Filter getFilter() {
        return personFilter;
    }

    public void updatePerson(Person person) {
        int index = filteredList.indexOf(person);
        notifyItemChanged(index);

        for (int i = 0; i < personList.size(); i++) {
            if (person.getId().equals(personList.get(i).getId())) {
                personList.remove(i);
                personList.add(i, person);
                break;
            }
        }
    }

    public void removePerson(Person person) {
        int index = filteredList.indexOf(person);
        filteredList.remove(index);
        personList.remove(person);
        notifyItemRemoved(index);
    }

    public void doSort(int sortType) {
        this.sortType = sortType;
        getFilter().filter("");
    }

    public void doFilter(ArrayList<Integer> filters) {
        this.filters = filters;
        getFilter().filter("");
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

    public class PersonFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            FilterResults results = new FilterResults();

            ArrayList<Person> tempList = new ArrayList<>();
            for (int i = 0; i < personList.size(); i++) {
                boolean passed = true;

                if (filters.size() > 0) {
                    label:
                    for (int j = 0; j < filters.size(); j++) {
                        switch (filters.get(j)) {
                            case 1:  // own data filter
                                if (!personList.get(i).getCreatedBy().equals(session.getUserEmail())) {
                                    passed = false;
                                    break label;
                                }
                                break;
                            case 2:  // date filter
                                if (!(personList.get(i).getCreated() <= endDate && personList.get(i).getCreated() >= startDate)) {
                                    passed = false;
                                    break label;
                                }
                                break;
                            case 3:  // location filter
                                if (!(Utils.distanceBetweenLocs(currentLoc, personList.get(i).getLastLocation()) < radius)) {
                                    passed = false;
                                    break label;
                                }
                                break;
                            case 4:  // search filter with query
                                if (!(personList.get(i).getName().contains(query) || personList.get(i).getSurname().contains(query))) {
                                    passed = false;
                                    break label;
                                }
                                break;
                        }
                    }
                }

                if (passed)
                    tempList.add(personList.get(i));
            }

            Collections.sort(tempList, (person, t1) -> {
                switch (sortType) {
                    case 1:    // Sort by created date
                        return Long.compare(t1.getCreated(), person.getCreated());
                    case 2:    // Sort by distance from me
                        if (currentLoc == null || person.getLastLocation() == null)
                            return 0;

                        return Double.compare(Utils.distanceBetweenLocs(currentLoc, person.getLastLocation()), Utils.distanceBetweenLocs(currentLoc, t1.getLastLocation()));
                    case 3:    // Sort by wasting
                        if (person.getLastMeasure() == null)
                            return 0;
                        return Double.compare(t1.getLastMeasure().getWeight() / t1.getLastMeasure().getHeight(), person.getLastMeasure().getWeight() / person.getLastMeasure().getHeight());
                    case 4:    // sort by stunting
                        if (person.getLastMeasure() == null)
                            return 0;

                        return Double.compare(t1.getLastMeasure().getHeight() / t1.getLastMeasure().getAge(), person.getLastMeasure().getHeight() / person.getLastMeasure().getAge());
                }
                return 0;
            });

            results.values = tempList;
            results.count = tempList.size();

            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults results) {
            filteredList = (ArrayList<Person>) results.values;
            notifyDataSetChanged();
        }
    }

    class PersonDiffCallback extends DiffUtil.Callback {

        private final List<Person> oldPosts, newPosts;

        public PersonDiffCallback(List<Person> oldPosts, List<Person> newPosts) {
            this.oldPosts = oldPosts;
            this.newPosts = newPosts;
        }

        @Override
        public int getOldListSize() {
            return oldPosts.size();
        }

        @Override
        public int getNewListSize() {
            return newPosts.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldPosts.get(oldItemPosition).getId() == newPosts.get(newItemPosition).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldPosts.get(oldItemPosition).equals(newPosts.get(newItemPosition));
        }
    }
}
