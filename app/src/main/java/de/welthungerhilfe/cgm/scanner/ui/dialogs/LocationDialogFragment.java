package de.welthungerhilfe.cgm.scanner.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.DialogFragmentLocationBinding;
import de.welthungerhilfe.cgm.scanner.datasource.location.india.IndiaLocation;
import de.welthungerhilfe.cgm.scanner.datasource.repository.IndiaLocationRepository;
import de.welthungerhilfe.cgm.scanner.ui.adapters.LocationAdapter;
import de.welthungerhilfe.cgm.scanner.ui.fragments.PersonalDataFragment;

public class LocationDialogFragment extends DialogFragment implements LocationAdapter.OnItemClickListener {


    private ArrayList<String> locations;

    DialogFragmentLocationBinding dialogFragmentLocationBinding;

    String area;

    IndiaLocationRepository indiaLocationRepository;

    LocationAdapter.OnItemClickListener onItemClickListener;

    PassDataToPersonDataFragment passDataToPersonDataFragment;

    public static LocationDialogFragment newInstance(String area, PassDataToPersonDataFragment passDataToPersonDataFragment) {
        LocationDialogFragment frag = new LocationDialogFragment();

        frag.setIndiaLocation(area,passDataToPersonDataFragment);
        return frag;
    }

    @Override
    public void onAttachFragment(@NonNull Fragment childFragment) {
        super.onAttachFragment(childFragment);
        onItemClickListener = (LocationAdapter.OnItemClickListener) childFragment;

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        passDataToPersonDataFragment = (LocationDialogFragment.PassDataToPersonDataFragment) getTargetFragment();

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        return dialog;
    }

    public void setIndiaLocation(String area,PassDataToPersonDataFragment passDataToPersonDataFragment){
        this.area = area;
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);

        /*locations = new ArrayList<>();
        locations.add("MITADI, BLOCK1, JUNAGADH, GUJARAT");
        locations.add("MANAVADAR,BLOCK1, JUNAGADH, GUJARAT");*/
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        dialogFragmentLocationBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_fragment_location,container,false);
        indiaLocationRepository = IndiaLocationRepository.getInstance(getActivity());

        dialogFragmentLocationBinding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        //dialogFragmentLocationBinding.recyclerView.setAdapter(new LocationAdapter(locations));

        if(area == null){
            locations = (ArrayList<String>) indiaLocationRepository.getVillage();
            dialogFragmentLocationBinding.searchEditText.setHint("Search village");
        }else{
            locations = (ArrayList<String>) indiaLocationRepository.getAganwadi(area);
            dialogFragmentLocationBinding.searchEditText.setHint("Select aaganwadi");

            filterLocations("");
        }

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

        dialogFragmentLocationBinding.recyclerView.setAdapter(new LocationAdapter(filteredLocations,new LocationAdapter.OnItemClickListener(){
            @Override
            public void onItemClick(String item) {
                Log.i("LocationDialog fragment","this is area "+item);
                passDataToPersonDataFragment.onPassDate(area,item);
                dismiss();
            }
        }));

    }


    @Override
    public void onItemClick(String item) {

    }

    public interface PassDataToPersonDataFragment{
        void onPassDate(String area,String data);
    }
}