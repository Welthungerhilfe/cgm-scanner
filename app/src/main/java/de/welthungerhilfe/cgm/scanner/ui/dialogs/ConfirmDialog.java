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
package de.welthungerhilfe.cgm.scanner.ui.dialogs;

import android.app.Dialog;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.DialogConfirmBinding;

public class ConfirmDialog extends Dialog implements View.OnClickListener {

    void onConfirm() {
        dismiss();
        if (confirmListener != null) {
            confirmListener.onConfirm(true);
        }
    }

    void onCancel() {
        dismiss();
        if (confirmListener != null) {
            confirmListener.onConfirm(false);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.txtOK) {
            onConfirm();
        } else if (v.getId() == R.id.txtCancel) {
            onCancel();
        }
    }

    public interface OnConfirmListener {
        void onConfirm(boolean result);
    }

    private OnConfirmListener confirmListener;
    private DialogConfirmBinding dialogConfirmBinding;

    public ConfirmDialog(@NonNull Context context) {
        super(context);

        this.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogConfirmBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.dialog_confirm, null, false);
        this.setContentView(dialogConfirmBinding.getRoot());
        this.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.getWindow().getAttributes().windowAnimations = R.style.DialogAnimationScale;
        this.setCancelable(false);
        dialogConfirmBinding.txtCancel.setOnClickListener(this);
        dialogConfirmBinding.txtOK.setOnClickListener(this);

    }

    public void setConfirmListener(OnConfirmListener confirmListener) {
        this.confirmListener = confirmListener;
    }

    public void setMessage(String message) {
        dialogConfirmBinding.txtMessage.setText(message);
    }

    public void setMessage(int message) {
        dialogConfirmBinding.txtMessage.setText(message);
    }

    public void setCancleButtonInvisible(){
        dialogConfirmBinding.txtCancel.setVisibility(View.GONE);
    }

    public void show() {
        super.show();
    }

    public void dismiss() {
        super.dismiss();
    }
}
