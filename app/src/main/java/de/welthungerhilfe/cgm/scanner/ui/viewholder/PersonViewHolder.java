package de.welthungerhilfe.cgm.scanner.ui.viewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import de.welthungerhilfe.cgm.scanner.R;

public class PersonViewHolder extends RecyclerView.ViewHolder {
    public TextView txtName;
    public TextView txtWeight;
    public TextView txtHeight;

    public PersonViewHolder(View itemView) {
        super(itemView);

        txtName = itemView.findViewById(R.id.txtName);
        txtWeight = itemView.findViewById(R.id.txtWeight);
        txtHeight = itemView.findViewById(R.id.txtHeight);
    }
}
