package es.quirk.bladereminder.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.Unbinder;
import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;

import java.util.ArrayList;
import java.util.List;

import es.quirk.bladereminder.R;

public class AddRazorDialog extends DialogFragment {

    @BindView(R.id.txt_razor_name) EditText mEditText;
    @BindView(R.id.add_razor_ok) Button mOkButton;
    @BindView(R.id.message) TextView mDialogMessage;
    private int mRazorIndex;
    private String mRazorName;
    private ArrayList<String> mExistingEntries;
    private Unbinder mUnbinder;

    @NonNull
    public static DialogFragment newInstance(int position, String name, List<String> razors) {
        AddRazorDialog d = new AddRazorDialog();
        Bundle bundle = new Bundle();
        bundle.putInt("position", position);
        bundle.putString("name", name);
        bundle.putStringArrayList("razors", new ArrayList<String>(razors));
        d.setArguments(bundle);
        return d;
    }

    /**
     * @returns true if currently editing an existing razor name, false if the
     * name is for a brand new entry.
     */
    private boolean isEditing() {
        return mRazorIndex != -1;
    }

    private void configureEditFields() {
        mEditText.setText(mRazorName);
        mDialogMessage.setText(getResources().getString(R.string.edit_razor_dlg_message));
        mOkButton.setText(getResources().getString(R.string.edit_razor_dlg_ok));
    }

    private void configureNewEntryFields() {
        mOkButton.setEnabled(false);
        mDialogMessage.setText(getResources().getString(R.string.add_razor_dlg_message));
        mOkButton.setText(getResources().getString(R.string.add_razor_dlg_ok));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_razor, container);
        mUnbinder = ButterKnife.bind(this, view);

        Bundle bundle = savedInstanceState;
        if (bundle == null) {
            bundle = getArguments();
        }

        mRazorIndex = bundle.getInt("position");
        mRazorName = bundle.getString("name");
        mExistingEntries = bundle.getStringArrayList("razors");
        getDialog().setTitle(isEditing() ? R.string.edit_razor_dlg_title : R.string.add_razor_dlg_title);

        // if editing, show the curr name
        if (isEditing())
            configureEditFields();
        else
            configureNewEntryFields();

        // Show soft keyboard automatically
        mEditText.requestFocus();
        getDialog().getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    private boolean isValidName(String name) {
        String trimmedName = name.trim();
        // no empty names
        if (trimmedName.isEmpty())
            return false;

        // editing and no changes, that's allowed
        if (isEditing() && trimmedName.equals(mRazorName)) {
            return true;
        }

        // otherwise can't be the same as an existing razor
        return !mExistingEntries.contains(trimmedName);
    }

    @OnTextChanged(R.id.txt_razor_name)
    void onTextChanged(CharSequence text) {
        mOkButton.setEnabled(isValidName(text.toString()));
    }

    private void doEnd() {
        // Return input text to activity
        String trimmedName = mEditText.getText().toString().trim();
        // no need to do anything if the name is unchanged
        if (isEditing() && trimmedName.equals(mRazorName)) {
            dismiss();
            return;
        }

        IAddRazorListener activity = (IAddRazorListener) getTargetFragment();
        if (isEditing())
            activity.onEditRazor(mRazorIndex, trimmedName);
        else
            activity.onAddRazor(trimmedName);
        dismiss();
    }

    @OnEditorAction(R.id.txt_razor_name)
    boolean onEditorAction(int actionId) {
        if (EditorInfo.IME_ACTION_DONE == actionId &&
                isValidName(mEditText.getText().toString())) {
            doEnd();
            return true;
        }
        return false;
    }

    @OnClick(R.id.add_razor_ok)
    void noteOK(Button button) {
        doEnd();
    }

}
