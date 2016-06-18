package es.quirk.bladereminder.fragments;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.view.ActionMode;
import android.support.v7.view.ActionMode.Callback;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.google.common.base.Optional;

import com.github.gist.ssins.EndlessRecyclerOnScrollListener;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import timber.log.Timber;

import es.quirk.bladereminder.EditShaveMenu;
import es.quirk.bladereminder.FabIconUpdater;
import es.quirk.bladereminder.NewShaveMenu;
import es.quirk.bladereminder.R;
import es.quirk.bladereminder.ShaveEntry;
import es.quirk.bladereminder.SoundHelper;
import es.quirk.bladereminder.Utils;
import es.quirk.bladereminder.activities.BladeReminderActivity;
import es.quirk.bladereminder.adapter.ShaveEntryAdapter;
import es.quirk.bladereminder.contentprovider.ShaveEntryContentProvider;
import es.quirk.bladereminder.database.Contract;
import es.quirk.bladereminder.database.Contract.Shaves;
import es.quirk.bladereminder.database.DataSource;
import es.quirk.bladereminder.tasks.BackFiller;
import es.quirk.bladereminder.widgets.DividerItemDecoration;
import es.quirk.bladereminder.widgets.TextDrawable;
import es.quirk.bladereminder.widgets.TextDrawableFactory;

/**
 * Fragment that holds the main list and floating action button.
 */
public class ShaveListFragment extends Fragment
        implements
        ShaveEntryAdapter.IClickListener,
        INotesEditorListener,
        IAddRazorListener,
        LoaderCallbacks<Cursor> {

        public final static String ARG_RAZOR = "razor";

        private final static int LIST_THRESHOLD = 2;
        private final static String[] PROJECTION = { Shaves._ID, Shaves.DATE, Shaves.COUNT, Shaves.COMMENT, Shaves.RAZOR };
        private static final int DIALOG_FRAGMENT = 1;
        @BindView(R.id.shaveList) RecyclerView mRecyclerView;
        @BindView(R.id.action_button) FloatingActionButton mFAB;
        @BindView(R.id.coordinator_layout) CoordinatorLayout mCoordLayout;
        @Nullable
        private BladeReminderActivity mMainActivity;
        private ShaveEntryAdapter mAdapter;
        private DataSource mDataSource;
        private final Callback mEditModeCallback = new NewShaveMenu(this);
        private final Callback mEditEntryCallback = new EditShaveMenu(this);
        private Optional<ActionMode> mActionMode = Optional.absent();
        private int mClickedItem = -1;
        private int mCurrentPage;
        private SoundHelper mSoundHelper;
        private SharedPreferences mPrefs;
        private View mRootView;
        private int mRazorId;
        private Unbinder mUnbinder;
        FragmentStatePagerAdapter mPagerAdapter;

        private void backfill() {
                new BackFiller(getActivity().getContentResolver()).execute();
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                        Bundle savedInstanceState) {
                View rootView = inflater.inflate(R.layout.fragment_shave_list, container, false);
                mUnbinder = ButterKnife.bind(this, rootView);
                mRootView = rootView;
                mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                updateFabIcon();
                FabIconUpdater.register(this);
                Bundle args = getArguments();
                mDataSource = new DataSource(rootView.getContext());
                mRazorId = mDataSource.getRazorId(args.getInt(ARG_RAZOR));

                mFAB.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                                heroaction();
                        }
                });

                getLoaderManager().initLoader(0, null, this);
                backfill();
                mCurrentPage = 0;
                mSoundHelper = new SoundHelper(rootView.getContext());
                return rootView;
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
                super.onViewCreated(view, savedInstanceState);
                LinearLayoutManager llm = new LinearLayoutManager(getActivity());
                mRecyclerView.setLayoutManager(llm);
                mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity()));
                mAdapter = new ShaveEntryAdapter(getActivity(), this);
                mRecyclerView.setAdapter(mAdapter);
                mRecyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener(llm) {
                        @Override
                        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                                super.onScrolled(recyclerView, dx, dy);
                                if (dy > 0)
                                        mFAB.setVisibility(View.INVISIBLE);
                                else
                                        mFAB.setVisibility(View.VISIBLE);
                        }
                        @Override
                        public void onLoadMore(int currentPage) {
                                mCurrentPage = currentPage - 1;
                                getLoaderManager().restartLoader(0, null, ShaveListFragment.this);
                        }
                });
        }

        @Override
        public void onDestroyView() {
                super.onDestroyView();
                mUnbinder.unbind();
                FabIconUpdater.unregister();
        }

        @Override
        public void onResume() {
                super.onResume();
                // in case it was paused for a day, fill out today
                backfill();
        }

        private class FabIconUpdateTask extends AsyncTask<Void, Void, String> {

                @Override
                protected String doInBackground(Void... params) {
                        String iconType = TextDrawableFactory.BLADE;
                        Cursor cursor = getActivity().getContentResolver().query(
                                        ShaveEntryContentProvider.CONTENT_URI,
                                        PROJECTION, null, null, Shaves.DATE_DESC);
                        Optional<ShaveEntry> shaveEntry = Optional.fromNullable(cursorToEntry(cursor));
                        if (shaveEntry.isPresent()) {
                                ShaveEntry selected = shaveEntry.get();
                                mClickedItem = (int) selected.getID();
                                int prev = getPreviousCount(selected.getDate());
                                if (selected.getCount() > 0)
                                        iconType = "gmd-edit";
                                else if (prev > 0)
                                        iconType = TextDrawableFactory.RAZOR;
                        }
                        return iconType;
                }

                @Override
                protected void onPostExecute(final String iconType) {
                        getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                        TextDrawable icon = TextDrawableFactory.createIcon(mRootView.getContext(), iconType);
                                        icon.setTextColor(Utils.LIGHT_TEXT);
                                        mFAB.setImageDrawable(icon);
                                }
                        });
                }
        }

        public void updateFabIcon() {
                new FabIconUpdateTask().execute();
        }

        private class NewEntryChecker extends AsyncTask<Void, Void, Integer> {
                private final ShaveEntry mShaveEntry;

                public NewEntryChecker(ShaveEntry shaveEntry) {
                        super();
                        mShaveEntry = shaveEntry;
                }

                @Override
                protected Integer doInBackground(Void... params) {
                        return getPreviousCount(mShaveEntry.getDate());
                }

                @Override
                protected void onPostExecute(Integer result) {
                        if (result == 0) {
                                doNew();
                        } else {
                                showActionModeForEntry(mShaveEntry);
                        }
                }
        }

        private void showActionModeForEntry(@NonNull ShaveEntry selected) {
                Callback touse = (selected.getCount() == 0) ? mEditModeCallback : mEditEntryCallback;
                if (mMainActivity != null)
                        mActionMode = Optional.fromNullable(mMainActivity.startSupportActionMode(touse));
                else
                        mActionMode = Optional.absent();
        }

        public static @Nullable ShaveEntry cursorToEntry(@NonNull Cursor cursor) {
                cursor.moveToFirst();
                ShaveEntry entry = cursor.isAfterLast() ? null :
                        ShaveEntry.fromCursor(cursor);
                cursor.close();
                return entry;
        }

        private class ItemClickTask extends AsyncTask<Void, Void, Optional<ShaveEntry>> {
                @Override
                protected Optional<ShaveEntry> doInBackground(Void... params) {
                        return getItemAtPosition(mClickedItem);
                }

                @Override
                protected void onPostExecute(Optional<ShaveEntry> maybeSelected) {
                        if (!maybeSelected.isPresent())
                                return;

                        ShaveEntry selected = maybeSelected.get();
                        Timber.d(" ___ selected.getCount() == %s", selected.getCount());
                        Timber.d(" ___ selected.getDate() == %s", selected.getDate());
                        // make sure we don't show 2 confusing icons when nothing previously added
                        if (selected.getCount() == 0) {
                                // maybe call doNew, or call the showActionModeForEntry
                                new NewEntryChecker(selected).execute();
                        } else {
                                showActionModeForEntry(selected);
                        }
                }
        }

        @Override
        public void onItemClick(long id) {
                if (mActionMode.isPresent()) {
                        mActionMode.get().finish();
                }
                mClickedItem = (int) id;
                new ItemClickTask().execute();
        }

        @Override
        public int getRazorId() {
                return mRazorId;
        }

        private void heroaction() {
                // find newest entry in database
                // "select id from shaves order by date DESC LIMIT 1";
                Cursor cursor = getActivity().getContentResolver().query(
                                ShaveEntryContentProvider.CONTENT_URI,
                                PROJECTION, null, null, Shaves.DATE_DESC);
                Optional<ShaveEntry> shaveEntry = Optional.fromNullable(cursorToEntry(cursor));
                if (shaveEntry.isPresent()) {
                        ShaveEntry selected = shaveEntry.get();
                        mClickedItem = (int) selected.getID();
                        if (selected.getCount() == 0)
                                doOneMore();
                        else
                                doEdit();
                }
        }

        public void onDestroyActionMode() {
                mActionMode = Optional.absent();
                mClickedItem = -1;
        }

        private void showEditDialog(final int position) {
                if (!mPrefs.getBoolean("show_comment_dialog", true))
                        return;
                DialogFragment enterNoteDialog = EnterNoteDialog.newInstance(position);
                enterNoteDialog.setTargetFragment(this, DIALOG_FRAGMENT);
                enterNoteDialog.show(getFragmentManager().beginTransaction(), "fragment_enter_note");
        }

        private int getPreviousCount(@NonNull final String date) {
                // "select count from shaves where date < ? and razor = [current] order by date DESC LIMIT 1";
                String[] selectionArgs = new String[] { date };
                String selection = Shaves.DATE + " < ? AND " +
                        Shaves.COUNT + " > 0 and (" +
                        Shaves.RAZOR + " = " + mRazorId + " OR " +
                        Shaves.RAZOR + " IS NULL)";
                String[] countProjection = new String[] { Shaves.COUNT };
                Cursor cursor = getActivity().getContentResolver().query(ShaveEntryContentProvider.CONTENT_URI,
                                countProjection, selection, selectionArgs, Shaves.DATE_DESC);

                cursor.moveToFirst();
                int result = cursor.isAfterLast() ? 0 : cursor.getInt(0);
                cursor.close();
                return result;
        }


        private class OneMoreSetter extends AsyncTask<Void, Void, Void> {

                @NonNull
                private final ShaveEntry mSelected;
                private final int mPosition;

                public OneMoreSetter(@NonNull ShaveEntry selected, int position) {
                        super();
                        mSelected = selected;
                        mPosition = position;
                        if (mMainActivity != null)
                                mMainActivity.start();
                }

                @Nullable
                @Override
                protected Void doInBackground(Void... params) {
                        final String date = mSelected.getDate();
                        int lastCount = getPreviousCount(date);
                        Uri shaveId = Uri.parse(ShaveEntryContentProvider.CONTENT_URI + "/" + mPosition);
                        ContentValues values = new ContentValues();
                        values.put(Shaves.COUNT, lastCount + 1);
                        values.put(Shaves.RAZOR, mRazorId);
                        getActivity().getContentResolver().update(shaveId, values, null, null);

                        // this next bit takes a while...
                        mDataSource.bubbleForwards(mSelected.getDate());
                        getActivity().getContentResolver().notifyChange(ShaveEntryContentProvider.CONTENT_URI, null);
                        return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                        if (mMainActivity != null)
                                mMainActivity.stop();
                }

        }

        public static Cursor getCursorForId(final ContentResolver resolver, final Uri id) {
                return resolver.query(id, PROJECTION, null, null, null);
        }

        private @NonNull Optional<ShaveEntry> getItemAtPosition(int pos) {
                Uri shaveId = Uri.parse(ShaveEntryContentProvider.CONTENT_URI + "/" + pos);
                Cursor cursor = getCursorForId(getActivity().getContentResolver(), shaveId);
                if (cursor == null)
                        return Optional.absent();
                return Optional.fromNullable(cursorToEntry(cursor));
        }

        private static class SaveNew extends AsyncTask<ShaveEntry, Void, Void> {

                final ContentResolver mContentResolver;
                final int mIndex;

                public SaveNew(int index, ContentResolver contentResolver) {
                        super();
                        mIndex = index;
                        mContentResolver = contentResolver;
                }

                @Nullable
                @Override
                protected Void doInBackground(ShaveEntry... params) {
                        ShaveEntry entry = params[0];
                        ContentValues values = new ContentValues();
                        values.put(Shaves.DATE, entry.getDate());
                        values.put(Shaves.COUNT, entry.getCount());
                        values.put(Shaves.COMMENT, entry.getComment());
                        values.put(Shaves.RAZOR, entry.getRazor());
                        Uri uri = Uri.parse(ShaveEntryContentProvider.CONTENT_URI + "/" + mIndex);
                        mContentResolver.update(uri, values, null, null);
                        return null;
                }

        }

        private class OneMoreOrNewTask extends AsyncTask<Void, Void, Optional<ShaveEntry>> {

                private final int mSoundResource;
                private final int mIndex;

                public OneMoreOrNewTask(int soundResource, int index) {
                        mSoundResource = soundResource;
                        mIndex = index;
                }

                @Override
                protected Optional<ShaveEntry> doInBackground(Void... params) {
                        return getItemAtPosition(mIndex);
                }

                @Override
                protected void onPostExecute(Optional<ShaveEntry> maybeSelected) {
                        if (!maybeSelected.isPresent())
                                return;

                        ShaveEntry selected = maybeSelected.get();
                        if (selected.getCount() == 0) {
                                mSoundHelper.playRawSound(mSoundResource);
                                if (mSoundResource == R.raw.newping) {
                                        ShaveEntry newentry = new ShaveEntry(selected.getID(), selected.getDate(),
                                                        1, selected.getComment(), Integer.toString(mRazorId));
                                        new SaveNew(mIndex, getActivity().getContentResolver()).execute(newentry);
                                } else if (mSoundResource == R.raw.plusone) {
                                        new OneMoreSetter(selected, mIndex).execute();
                                }
                                showEditDialog(mIndex);
                        }
                }
        }

        private void doOneMoreOrNew(@RawRes int soundResource) {
                if (mClickedItem == -1)
                        return;
                new OneMoreOrNewTask(soundResource, mClickedItem).execute();
        }

        public void doOneMore() {
                doOneMoreOrNew(R.raw.plusone);
        }

        public void doNew() {
                doOneMoreOrNew(R.raw.newping);
        }

        private class Deleter extends AsyncTask<ShaveEntry, Void, Void> {

                @Nullable
                @Override
                protected Void doInBackground(ShaveEntry... params) {
                        ContentValues values = new ContentValues();
                        values.put(Shaves.COMMENT, "");
                        values.put(Shaves.COUNT, "0");
                        Uri uri = Uri.parse(ShaveEntryContentProvider.CONTENT_URI + "/" + params[0].getID());
                        getActivity().getContentResolver().update(uri, values, null, null);
                        return null;
                }

        }

        private class DoDeleterTask extends AsyncTask<Void, Void, Optional<ShaveEntry>> {
                private final int mIndex;
                public DoDeleterTask(int index) {
                        mIndex = index;
                }
                @Override
                protected Optional<ShaveEntry> doInBackground(Void... params) {
                        return getItemAtPosition(mIndex);
                }

                @Override
                protected void onPostExecute(Optional<ShaveEntry> maybeSelected) {
                        if (!maybeSelected.isPresent())
                                return;

                        final ShaveEntry selected = maybeSelected.get();
                        final int lastDeletedPosition = mIndex;
                        Timber.d("mCurrentPage = %s", mCoordLayout.toString());
                        Snackbar.make(mCoordLayout, R.string.deleted, Snackbar.LENGTH_LONG)
                                .setAction(R.string.undo, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                                doUndo(selected, lastDeletedPosition);
                                        }
                                })
                        .show();

                        mSoundHelper.playRawSound(R.raw.delete);
                        new Deleter().execute(selected);
                }
        }

        public void doDelete() {
                if (mClickedItem == -1)
                        return;
                new DoDeleterTask(mClickedItem).execute();
        }

        public void doEdit() {
                if (mClickedItem == -1)
                        return;
                Intent intent = new Intent(Intent.ACTION_EDIT,
                                ShaveEntryContentProvider.CONTENT_URI);
                intent.setType(Contract.Shaves.CONTENT_ITEM_TYPE);
                intent.putExtra(Contract.Shaves.CONTENT_ITEM_TYPE, ShaveEntryContentProvider.CONTENT_URI + "/" + mClickedItem);
                startActivityForResult(intent, 1);
        }

        @Override
        public void onAttach(Context activity) {
                super.onAttach(activity);
                // This makes sure that the container activity has implemented
                // the callback interface. If not, it throws an exception
                mMainActivity = (BladeReminderActivity) activity;
        }

        @Override
        public void onDetach() {
                super.onDetach();
                mMainActivity = null;
        }

        private void doUndo(@NonNull ShaveEntry lastDeleted, int lastDeletedPosition) {
                // somehow undo
                new SaveNew(lastDeletedPosition, getActivity().getContentResolver()).execute(lastDeleted);
        }


        public void setPagerAdapter(FragmentStatePagerAdapter adapter) {
                mPagerAdapter = adapter;
        }

        private class AddRazorTask extends AsyncTask<String, Void, Void> {
                @Override
                protected Void doInBackground(String... params) {
                        mDataSource.addRazor(params[0]);
                        return null;
                }
                @Override
                protected void onPostExecute(Void result) {
                        if (mPagerAdapter != null) {
                                mPagerAdapter.notifyDataSetChanged();
                        }
                }
        }

        private class EditRazorTask extends AsyncTask<String, Void, Boolean> {
                @Override
                protected Boolean doInBackground(String... params) {
                        mDataSource.editRazor(params[0], params[1]);
                        return Boolean.TRUE;
                }
                @Override
                protected void onPostExecute(Boolean result) {
                        if (result && mPagerAdapter != null) {
                                mPagerAdapter.notifyDataSetChanged();
                        }
                }
        }

        @Override
	public void onAddRazor(final String name) {
                if (name.isEmpty()) {
                        return;
                }
                new AddRazorTask().execute(name);
        }

        @Override
	public void onEditRazor(int position, final String name) {
                if (name.isEmpty()) {
                        return;
                }
                new EditRazorTask().execute(Integer.toString(position), name);
        }

        @Override
        public void onNotesEdit(int position, String newComment) {
                // no longer has race condition #1
                final ContentValues values = new ContentValues();
                values.put(Shaves.COMMENT, newComment);
                final Uri uri = Uri.parse(ShaveEntryContentProvider.CONTENT_URI + "/" + position);
		(new Thread() {
			@Override
			public void run() {
                                getActivity().getContentResolver().update(uri, values, null, null);
			}
		}).start();
        }

        // creates a new loader after the initLoader() call
        @NonNull
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                Timber.d("______________ onCreateLoader");
                String selection = Shaves.DATE + " > ? ";
                Date range = Calendar.getInstance().getTime();
                long daysago = (long)((mCurrentPage + 1) * 28) * Utils.ONE_DAY_MS;
                long newTime = range.getTime() - daysago;
                range.setTime(newTime);
                DateFormat format = Utils.createDateFormatYYYYMMDD();
                String []selectionArgs = new String[] {
                        format.format(range)
                };
                if (mMainActivity != null)
                        mMainActivity.start();
                return new CursorLoader(getActivity().getApplicationContext(),
                                ShaveEntryContentProvider.CONTENT_URI, PROJECTION, selection,
                                selectionArgs, Shaves.DATE_DESC);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
                mAdapter.changeCursor(cursor);
                if (mMainActivity != null)
                        mMainActivity.stop();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
                // data is not available anymore, delete reference
                mAdapter.changeCursor(null);
        }
}
