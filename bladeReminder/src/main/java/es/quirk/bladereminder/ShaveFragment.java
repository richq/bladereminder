package es.quirk.bladereminder;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.view.ActionMode;
import android.support.v7.view.ActionMode.Callback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.github.clans.fab.FloatingActionButton;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnItemClick;
import es.quirk.bladereminder.contentprovider.ShaveEntryContentProvider;
import es.quirk.bladereminder.database.Contract;
import es.quirk.bladereminder.database.Contract.Shaves;
import es.quirk.bladereminder.database.DataSource;
import es.quirk.bladereminder.tasks.BackFiller;
import es.quirk.bladereminder.widgets.TextDrawable;
import es.quirk.bladereminder.widgets.TextDrawableFactory;
import timber.log.Timber;

/**
 * A placeholder fragment containing a simple view.
 */
public class ShaveFragment extends Fragment
        implements OnScrollListener,
                   INotesEditorListener,
                   LoaderCallbacks<Cursor> {

        private final static int LIST_THRESHOLD = 2;
        final static String[] PROJECTION = { Shaves._ID, Shaves.DATE, Shaves.COUNT, Shaves.COMMENT };
        private static final int DIALOG_FRAGMENT = 1;
        private int mListViewLastCount;
        private View mHeaderView;
        @InjectView(R.id.shaveList) ListView mListView;
        @InjectView(R.id.action_button) FloatingActionButton mFAB;
        private int mPreviousVisibleItem;
        private final List<View> mColumnHeaders = Lists.newArrayList();
        private BladeReminderActivity mMainActivity;
        private SimpleCursorAdapter mAdapter;
        private Optional<ShaveEntry> mLastDeleted = Optional.absent();
        private int mLastDeletedPosition;
        private DataSource mDataSource;
        private final Callback mEditModeCallback = new NewShaveMenu(this);
        private final Callback mEditEntryCallback = new EditShaveMenu(this);
        private Optional<ActionMode> mActionMode = Optional.absent();
        private int mClickedItem = -1;
        private int mCurrentPage;
        private SoundHelper mSoundHelper;
        private SharedPreferences mPrefs;
        private View mRootView;

        private void backfill() {
                new BackFiller(getActivity().getContentResolver()).execute();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                        Bundle savedInstanceState) {
                View rootView = inflater.inflate(R.layout.fragment_blade_reminder, container, false);
                ButterKnife.inject(this, rootView);
                mRootView = rootView;
                mHeaderView = inflater.inflate(R.layout.view_list_item_header, mListView, false);
                mColumnHeaders.add(ButterKnife.findById(mHeaderView, R.id.column_header1));
                mColumnHeaders.add(ButterKnife.findById(mHeaderView, R.id.column_header2));
                mColumnHeaders.add(ButterKnife.findById(mHeaderView, R.id.column_header3));
                mListView.addHeaderView(mHeaderView, null, false);
                mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                updateFabIcon();
                FabIconUpdater.register(this);

                mFAB.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                                heroaction();
                        }
                });

                mDataSource = new DataSource(rootView.getContext());
                String[] columns = new String[] { Shaves.DATE, Shaves.COUNT, Shaves.COMMENT};
                // Fields on the UI to which we map
                int[] layoutIds = new int[] {
                        R.id.date_label,
                        R.id.count_label,
                        R.id.comment,
                };
                getLoaderManager().initLoader(0, null, this);
                mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.shaveentry, null, columns,
                                        layoutIds, 0);
                mListView.setAdapter(mAdapter);
                mListView.setOnScrollListener(this);
                backfill();
                mCurrentPage = 0;
                mSoundHelper = new SoundHelper(rootView.getContext());
                return rootView;
        }

        @Override
        public void onDestroyView() {
                super.onDestroyView();
                ButterKnife.reset(this);
                FabIconUpdater.unregister();
        }

        @Override
        public void onResume() {
                super.onResume();
                // in case it was paused for a day, fill out today
                backfill();
        }

        void updateFabIcon() {
                String iconType = TextDrawableFactory.BLADE;
                Cursor cursor = getActivity().getContentResolver().query(
                                ShaveEntryContentProvider.CONTENT_URI,
                                PROJECTION, null, null, Shaves.DATE + " DESC");
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
                final String iconType2 = iconType;
                getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                                TextDrawable icon = TextDrawableFactory.createIcon(mRootView.getContext(), iconType2);
                                icon.setTextColor(Utils.LIGHT_TEXT);
                                mFAB.setImageDrawable(icon);
                        }
                });
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

        private void showActionModeForEntry(ShaveEntry selected) {
                Callback touse = (selected.getCount() == 0) ? mEditModeCallback : mEditEntryCallback;
                mActionMode = Optional.fromNullable(mMainActivity.startSupportActionMode(touse));
        }

        public static ShaveEntry cursorToEntry(Cursor cursor) {
                cursor.moveToFirst();
                ShaveEntry entry = cursor.isAfterLast() ? null :
                        DataSource.cursorToEntry(cursor);
                cursor.close();
                return entry;
        }

        @OnItemClick(R.id.shaveList)
        void onItemClick(long id) {
                if (mActionMode.isPresent()) {
                        mActionMode.get().finish();
                }
                mClickedItem = (int) id;
                Optional<ShaveEntry> maybeSelected = getItemAtPosition(mClickedItem);
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

        private void heroaction() {
                // find newest entry in database
                // "select id from shaves order by date DESC LIMIT 1";
                Cursor cursor = getActivity().getContentResolver().query(
                                ShaveEntryContentProvider.CONTENT_URI,
                                PROJECTION, null, null, Shaves.DATE + " DESC");
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

        private int getPreviousCount(final String date) {
                // "select count from shaves where date < ? order by date DESC LIMIT 1";
                String[] selectionArgs = new String[] { date };
                String selection = Shaves.DATE + " < ? AND " + Shaves.COUNT + " > 0";
                String[] countProjection = new String[] { Shaves.COUNT };
                Cursor cursor = getActivity().getContentResolver().query(ShaveEntryContentProvider.CONTENT_URI,
                                countProjection, selection, selectionArgs, Shaves.DATE + " DESC");

                cursor.moveToFirst();
                int result = cursor.isAfterLast() ? 0 : cursor.getInt(0);
                cursor.close();
                return result;
        }


        private class OneMoreSetter extends AsyncTask<Void, Void, Void> {

                private final ShaveEntry mSelected;
                private final int mPosition;

                public OneMoreSetter(ShaveEntry selected, int position) {
                        super();
                        mSelected = selected;
                        mPosition = position;
                        mMainActivity.start();
                }

                @Override
                protected Void doInBackground(Void... params) {
                        final String date = mSelected.getDate();
                        int lastCount = getPreviousCount(date);
                        Uri shaveId = Uri.parse(ShaveEntryContentProvider.CONTENT_URI + "/" + mPosition);
                        ContentValues values = new ContentValues();
                        values.put(Shaves.COUNT, lastCount + 1);
                        getActivity().getContentResolver().update(shaveId, values, null, null);

                        // this next bit takes a while...
                        mDataSource.bubbleForwards(mSelected.getDate());
                        getActivity().getContentResolver().notifyChange(ShaveEntryContentProvider.CONTENT_URI, null);
                        return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                        mMainActivity.stop();
                }

        }

        private Optional<ShaveEntry> getItemAtPosition(int pos) {
                Uri shaveId = Uri.parse(ShaveEntryContentProvider.CONTENT_URI + "/" + pos);
                Cursor cursor = getActivity().getContentResolver().query(shaveId, PROJECTION,
                                null, null, null);
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

                @Override
                protected Void doInBackground(ShaveEntry... params) {
                        ShaveEntry entry = params[0];
                        ContentValues values = new ContentValues();
                        values.put(Shaves.DATE, entry.getDate());
                        values.put(Shaves.COUNT, entry.getCount());
                        values.put(Shaves.COMMENT, entry.getComment());
                        Uri uri = Uri.parse(ShaveEntryContentProvider.CONTENT_URI + "/" + mIndex);
                        mContentResolver.update(uri, values, null, null);
                        return null;
                }

        }

        private void doOneMoreOrNew(int soundResource) {
                if (mClickedItem == -1)
                        return;
                Optional<ShaveEntry> maybeSelected = getItemAtPosition(mClickedItem);
                if (!maybeSelected.isPresent())
                        return;

                ShaveEntry selected = maybeSelected.get();
                if (selected.getCount() == 0) {
                        mSoundHelper.playRawSound(soundResource);
                        if (soundResource == R.raw.newping) {
                                ShaveEntry newentry = new ShaveEntry(selected.getID(), selected.getDate(),
                                                1, selected.getComment());
                                new SaveNew(mClickedItem, getActivity().getContentResolver()).execute(newentry);
                        } else if (soundResource == R.raw.plusone) {
                                new OneMoreSetter(selected, mClickedItem).execute();
                        }
                        showEditDialog(mClickedItem);
                }
        }

        public void doOneMore() {
                doOneMoreOrNew(R.raw.plusone);
        }

        public void doNew() {
                doOneMoreOrNew(R.raw.newping);
        }

        private class Deleter extends AsyncTask<ShaveEntry, Void, Void> {

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

        public void doDelete() {
                if (mClickedItem == -1)
                        return;
                Optional<ShaveEntry> maybeSelected = getItemAtPosition(mClickedItem);
                if (!maybeSelected.isPresent())
                        return;

                final ShaveEntry selected = maybeSelected.get();
                mLastDeleted = Optional.fromNullable(selected);
                mLastDeletedPosition = mClickedItem;
                Snackbar.make(mRootView, R.string.deleted, Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                        doUndo();
                                }
                        })
                        .show();

                mSoundHelper.playRawSound(R.raw.delete);
                new Deleter().execute(selected);
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
        public void onAttach(Activity activity) {
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

        private void doUndo() {
                // somehow undo
                if (mLastDeleted.isPresent()) {
                        new SaveNew(mLastDeletedPosition, getActivity().getContentResolver()).execute(mLastDeleted.get());
                        mLastDeleted = Optional.absent();
                }
        }

        private void updateFloatingActionButton(int firstVisibleItem) {
                // go up = vis, down = hidden
                if (firstVisibleItem > mPreviousVisibleItem) {
                        mFAB.hide(true);
                } else if (firstVisibleItem < mPreviousVisibleItem) {
                        mFAB.show(true);
                }
                mPreviousVisibleItem = firstVisibleItem;
        }

        /**
         * Required for the OnScrollListener interface.
         */
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                updateFloatingActionButton(firstVisibleItem);
                final float top = -mHeaderView.getTop();
                float height = mHeaderView.getHeight();
                if (top > height)
                        return;
                for (View textView : mColumnHeaders) {
                        textView.setTranslationY(top / 2f);
                }

        }

        @Override
        public void onScrollStateChanged(AbsListView listView, int scrollState) {
                int count = mListView.getCount();
                if (scrollState == SCROLL_STATE_IDLE) {
                        if (count > mListViewLastCount &&
                                mListView.getLastVisiblePosition() >= (count - 1 - LIST_THRESHOLD)) {
                                mCurrentPage++;
                                getLoaderManager().restartLoader(0, null, this);
                                mListViewLastCount = count;
                                Timber.d("mCurrentPage = %d", mCurrentPage);
                        }
                }
        }

        @Override
        public void onNotesEdit(int position, String newComment) {
                // no longer has race condition #1
                ContentValues values = new ContentValues();
                values.put(Shaves.COMMENT, newComment);
                Uri uri = Uri.parse(ShaveEntryContentProvider.CONTENT_URI + "/" + position);
                getActivity().getContentResolver().update(uri, values, null, null);
        }

        // creates a new loader after the initLoader() call
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
                mMainActivity.start();
                return new CursorLoader(getActivity().getApplicationContext(),
                                ShaveEntryContentProvider.CONTENT_URI, PROJECTION, selection,
                                selectionArgs, Shaves.DATE + " DESC");
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                mAdapter.swapCursor(data);
                mMainActivity.stop();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
                // data is not available anymore, delete reference
                mAdapter.swapCursor(null);
        }
}
