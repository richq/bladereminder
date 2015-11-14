package es.quirk.bladereminder.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import butterknife.ButterKnife;
import butterknife.Bind;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import es.quirk.bladereminder.R;

public class EnterNoteDialog extends DialogFragment {

    @Bind(R.id.txt_your_note) EditText mEditText;
    @Bind(R.id.do_not_show_checkbox) CheckBox mDontShow;
    private int mPosition;
    private static final String NO_COUNT = "nah_count";

    @NonNull
    public static DialogFragment newInstance(int position) {
        EnterNoteDialog d = new EnterNoteDialog();
        Bundle bundle = new Bundle();
        bundle.putInt("position", position);
        d.setArguments(bundle);
        return d;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_insert_note, container);
        ButterKnife.bind(this, view);
        getDialog().setTitle(R.string.add_comment_dlg_title);
        if (savedInstanceState != null)
            mPosition = savedInstanceState.getInt("position");
        else
            mPosition = getArguments().getInt("position");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        boolean userHatesPopups = prefs.getInt(NO_COUNT, 0) >= 2;
        mDontShow.setVisibility(userHatesPopups ? View.VISIBLE : View.GONE);

        // Show soft keyboard automatically
        mEditText.requestFocus();
        getDialog().getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @OnCheckedChanged(R.id.do_not_show_checkbox)
    void checkChanged(@NonNull CheckBox check) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        prefs.edit().putBoolean("show_comment_dialog", !check.isChecked()).apply();
    }

    private void doEnd() {
        // Return input text to activity
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        String entry = mEditText.getText().toString();
        if (entry.isEmpty()) {
            int nahCount = prefs.getInt(NO_COUNT, 0);
            prefs.edit().putInt(NO_COUNT, nahCount + 1).apply();
        } else {
            prefs.edit().putInt(NO_COUNT, 0).apply();
        }
        INotesEditorListener activity = (INotesEditorListener) getTargetFragment();
        activity.onNotesEdit(mPosition, entry);
        dismiss();
    }

    @OnEditorAction(R.id.txt_your_note)
    boolean onEditorAction(int actionId) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
            doEnd();
            return true;
        }
        return false;
    }

    @OnClick(R.id.enter_note_ok)
    void noteOK(Button button) {
        doEnd();
    }

}
