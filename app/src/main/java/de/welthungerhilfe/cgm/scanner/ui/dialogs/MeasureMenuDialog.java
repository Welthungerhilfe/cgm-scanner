package de.welthungerhilfe.cgm.scanner.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.DialogMeasureMenuBinding;

public class MeasureMenuDialog extends DialogFragment {

    DialogMeasureMenuBinding dialogMeasureMenuBinding;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        dialogMeasureMenuBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_measure_menu,container,false);

        return dialogMeasureMenuBinding.getRoot();
    }
}
