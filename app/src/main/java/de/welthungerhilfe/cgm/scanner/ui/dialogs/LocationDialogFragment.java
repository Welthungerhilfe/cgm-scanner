package de.welthungerhilfe.cgm.scanner.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.DialogFragmentLocationBinding;
import de.welthungerhilfe.cgm.scanner.ui.adapters.LocationAdapter;

public class LocationDialogFragment extends DialogFragment {


    private ArrayList<String> locations;

    DialogFragmentLocationBinding dialogFragmentLocationBinding;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        return dialog;
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);

        locations = new ArrayList<>();
        locations.add("MITADI, BLOCK1, JUNAGADH, GUJARAT");
        locations.add("MANAVADAR,BLOCK1, JUNAGADH, GUJARAT");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        dialogFragmentLocationBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_fragment_location,container,false);

        dialogFragmentLocationBinding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        //dialogFragmentLocationBinding.recyclerView.setAdapter(new LocationAdapter(locations));

        dialogFragmentLocationBinding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    filterLocations(charSequence.toString());
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        return dialogFragmentLocationBinding.getRoot();
    }

    private void filterLocations(String text) {
        ArrayList<String> filteredLocations = new ArrayList<>();
        for (String location : locations) {
            if (location.toLowerCase().startsWith(text.toLowerCase())) {
                filteredLocations.add(location);
            }
        }

        dialogFragmentLocationBinding.recyclerView.setAdapter(new LocationAdapter(filteredLocations));
    }


}