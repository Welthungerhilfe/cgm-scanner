package de.welthungerhilfe.cgm.scanner.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.R;

public class QRScanConfirmDialog extends DialogFragment {

    View view;

    QRScanConfirmDialogInterface qrScanConfirmDialogInterface;

    boolean doNotShowAgain = false;

    SessionManager sessionManager;

    @OnClick(R.id.iv_back)
    void closeActivity() {
        qrScanConfirmDialogInterface.onConfirm(false);
    }

    @OnClick(R.id.txtCancel)
    void closeActivityByCancel() {
        qrScanConfirmDialogInterface.onConfirm(false);
        dismiss();
    }

    @OnClick(R.id.btnOK)
    void onConfirm() {
        sessionManager.setQrConfirmDialogDoNotshow(doNotShowAgain);
        qrScanConfirmDialogInterface.onConfirm(true);
        dismiss();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        qrScanConfirmDialogInterface = (QRScanConfirmDialogInterface) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.dialog_qrscan_confirm, container, false);
        ButterKnife.bind(this, view);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().setCancelable(false);
        sessionManager = new SessionManager(getActivity());

        return view;
    }


    public interface QRScanConfirmDialogInterface {
        void onConfirm(boolean value);
    }

    @OnCheckedChanged(R.id.checkDoNoShowAgain)
    void onAlert(CompoundButton buttonView, boolean isChecked) {
        doNotShowAgain = isChecked;
    }
}
