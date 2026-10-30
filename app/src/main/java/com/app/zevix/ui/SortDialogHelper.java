package com.app.zevix.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.app.zevix.R;
import com.app.zevix.data.MediaStoreRepository;

public class SortDialogHelper {

    public interface OnSortSelectedListener {
        void onSortSelected(int sortOrder, String toastLabel);
    }

    public static void showSortDialog(Context context, boolean isFolderSort, int currentSortOrder, OnSortSelectedListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_sort_custom);

        RadioGroup rgCriteria = dialog.findViewById(R.id.rg_sort_criteria);
        RadioButton rbName = dialog.findViewById(R.id.rb_sort_name);
        RadioButton rbDate = dialog.findViewById(R.id.rb_sort_date);
        RadioButton rbSize = dialog.findViewById(R.id.rb_sort_size);
        RadioButton rbLength = dialog.findViewById(R.id.rb_sort_length);

        RadioGroup rgOrder = dialog.findViewById(R.id.rg_sort_order);
        RadioButton rbOpt1 = dialog.findViewById(R.id.rb_sort_asc);
        RadioButton rbOpt2 = dialog.findViewById(R.id.rb_sort_desc);

        TextView btnCancel = dialog.findViewById(R.id.btn_sort_cancel);
        TextView btnOk = dialog.findViewById(R.id.btn_sort_ok);

        if (isFolderSort) {
            rbDate.setVisibility(View.GONE);
            rbLength.setVisibility(View.GONE);
        }

        // Set initial state
        if (currentSortOrder == MediaStoreRepository.SORT_NAME_ASC) {
            rbName.setChecked(true);
            updateOrderLabels(rbName, rbOpt1, rbOpt2);
            rbOpt1.setChecked(true);
        } else if (currentSortOrder == MediaStoreRepository.SORT_NAME_DESC) {
            rbName.setChecked(true);
            updateOrderLabels(rbName, rbOpt1, rbOpt2);
            rbOpt2.setChecked(true);
        } else if (currentSortOrder == MediaStoreRepository.SORT_SIZE_DESC) {
            rbSize.setChecked(true);
            updateOrderLabels(rbSize, rbOpt1, rbOpt2);
            rbOpt1.setChecked(true);
        } else if (currentSortOrder == MediaStoreRepository.SORT_SIZE_ASC) {
            rbSize.setChecked(true);
            updateOrderLabels(rbSize, rbOpt1, rbOpt2);
            rbOpt2.setChecked(true);
        } else if (currentSortOrder == MediaStoreRepository.SORT_LENGTH_DESC) {
            rbLength.setChecked(true);
            updateOrderLabels(rbLength, rbOpt1, rbOpt2);
            rbOpt1.setChecked(true);
        } else if (currentSortOrder == MediaStoreRepository.SORT_LENGTH_ASC) {
            rbLength.setChecked(true);
            updateOrderLabels(rbLength, rbOpt1, rbOpt2);
            rbOpt2.setChecked(true);
        } else if (currentSortOrder == MediaStoreRepository.SORT_DATE_ASC) {
            rbDate.setChecked(true);
            updateOrderLabels(rbDate, rbOpt1, rbOpt2);
            rbOpt2.setChecked(true);
        } else { // default SORT_DATE_DESC
            if (isFolderSort) {
                rbName.setChecked(true);
                updateOrderLabels(rbName, rbOpt1, rbOpt2);
                rbOpt1.setChecked(true);
            } else {
                rbDate.setChecked(true);
                updateOrderLabels(rbDate, rbOpt1, rbOpt2);
                rbOpt1.setChecked(true);
            }
        }

        rgCriteria.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selected = dialog.findViewById(checkedId);
            boolean wasOpt1 = rbOpt1.isChecked();
            updateOrderLabels(selected, rbOpt1, rbOpt2);
            if (wasOpt1) {
                rbOpt1.setChecked(true);
            } else {
                rbOpt2.setChecked(true);
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnOk.setOnClickListener(v -> {
            int checkedCriteriaId = rgCriteria.getCheckedRadioButtonId();
            boolean isOpt1 = rbOpt1.isChecked();
            int selectedOrder;
            String toastText = isOpt1 ? rbOpt1.getText().toString() : rbOpt2.getText().toString();

            if (checkedCriteriaId == R.id.rb_sort_name) {
                selectedOrder = isOpt1 ? MediaStoreRepository.SORT_NAME_ASC : MediaStoreRepository.SORT_NAME_DESC;
            } else if (checkedCriteriaId == R.id.rb_sort_size) {
                selectedOrder = isOpt1 ? MediaStoreRepository.SORT_SIZE_DESC : MediaStoreRepository.SORT_SIZE_ASC;
            } else if (checkedCriteriaId == R.id.rb_sort_length) {
                selectedOrder = isOpt1 ? MediaStoreRepository.SORT_LENGTH_DESC : MediaStoreRepository.SORT_LENGTH_ASC;
            } else { // Date
                selectedOrder = isOpt1 ? MediaStoreRepository.SORT_DATE_DESC : MediaStoreRepository.SORT_DATE_ASC;
            }

            dialog.dismiss();
            if (listener != null) {
                listener.onSortSelected(selectedOrder, toastText);
            }
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.86);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private static void updateOrderLabels(RadioButton selectedCriteria, RadioButton rbOpt1, RadioButton rbOpt2) {
        if (selectedCriteria == null) return;
        if (selectedCriteria.getId() == R.id.rb_sort_name) {
            rbOpt1.setText("From A to Z");
            rbOpt2.setText("From Z to A");
        } else if (selectedCriteria.getId() == R.id.rb_sort_size) {
            rbOpt1.setText("From big to small");
            rbOpt2.setText("From small to big");
        } else if (selectedCriteria.getId() == R.id.rb_sort_length) {
            rbOpt1.setText("From long to short");
            rbOpt2.setText("From short to long");
        } else { // Date
            rbOpt1.setText("From new to old");
            rbOpt2.setText("From old to new");
        }
    }
}

