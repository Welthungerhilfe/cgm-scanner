package de.welthungerhilfe.cgm.scanner.ui.adapters;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import de.welthungerhilfe.cgm.scanner.R;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.ViewHolder> {

    private ArrayList<String> locations;

    public LocationAdapter(ArrayList<String> locations) {
        this.locations = locations;
    }

    @Override
    public LocationAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location, parent, false);
        return new LocationAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(LocationAdapter.ViewHolder holder, int position) {
        holder.tvLocation.setText(locations.get(position));
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvLocation;

        public ViewHolder(View itemView) {
            super(itemView);

            tvLocation = itemView.findViewById(R.id.tv_location);
        }
    }
}

