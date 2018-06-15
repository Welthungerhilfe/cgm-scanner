package de.welthungerhilfe.cgm.scanner.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.delegators.OnConfirmListener;

public class DocumentAskDialog extends Dialog {

    @OnClick(R.id.txtAskNo)
    void onNo(TextView txtAskNo) {
        dismiss();
        if (confirmListener != null) {
            confirmListener.onConfirm(false);
        }
    }

    @OnClick(R.id.txtAskYes)
    void onYes(TextView txtAskYes) {
        dismiss();
        if (confirmListener != null) {
            confirmListener.onConfirm(true);
        }
    }

    @BindView(R.id.imgQr)
    ImageView imgQr;

    private OnConfirmListener confirmListener;

    public DocumentAskDialog(@NonNull Context context) {
        super(context);

        this.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.dialog_ask);
        this.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.getWindow().getAttributes().windowAnimations = R.style.DialogAnimationScale;
        this.setCancelable(false);

        ButterKnife.bind(this);
    }

    public void setDocumentImage(File mFile) {
        Glide.with(getContext()).load(mFile).into(imgQr);
    }

    public void setDocumentImage(Uri mUri) {
        Glide.with(getContext()).load(mUri).into(imgQr);
    }

    public void setConfirmListener(OnConfirmListener listener) {
        confirmListener = listener;
    }

    public void onBackPressed() {
        dismiss();
    }
}
