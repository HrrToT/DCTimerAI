package com.dctimer.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.dctimer.R;
import com.dctimer.activity.MainActivity;
import com.dctimer.util.ExternalTimerImportManager;

public class OtherTimerImportDialog extends DialogFragment {
    public static OtherTimerImportDialog newInstance() {
        return new OtherTimerImportDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_other_timer_import, null);

        View itemCstimer = view.findViewById(R.id.item_cstimer);
        View itemTwistyTimer = view.findViewById(R.id.item_twisty_timer);
        itemCstimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).requestOtherTimerImport(ExternalTimerImportManager.SOURCE_CSTIMER);
                } else {
                    Toast.makeText(getActivity(), R.string.other_timer_import_pending, Toast.LENGTH_SHORT).show();
                }
                dismiss();
            }
        });
        itemTwistyTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).requestOtherTimerImport(ExternalTimerImportManager.SOURCE_TWISTY_TIMER);
                } else {
                    Toast.makeText(getActivity(), R.string.other_timer_import_pending, Toast.LENGTH_SHORT).show();
                }
                dismiss();
            }
        });

        builder.setTitle(R.string.other_timer_import_title)
                .setView(view)
                .setNegativeButton(R.string.btn_close, null);
        return builder.create();
    }
}
