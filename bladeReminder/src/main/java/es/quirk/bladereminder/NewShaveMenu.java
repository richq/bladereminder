package es.quirk.bladereminder;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import es.quirk.bladereminder.fragments.ShaveListFragment;
import es.quirk.bladereminder.widgets.TextDrawable;
import es.quirk.bladereminder.widgets.TextDrawableFactory;
import timber.log.Timber;

public final class NewShaveMenu implements ActionMode.Callback {
	private final ShaveListFragment mShaveListFragment;

	public NewShaveMenu(ShaveListFragment shaveListFragment) {
		mShaveListFragment = shaveListFragment;
	}

	@Override
	public boolean onActionItemClicked(@NonNull ActionMode mode, @NonNull MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_item_one_more:
				mShaveListFragment.doOneMore();
				mode.finish();
				return true;
			case R.id.menu_item_new:
				mShaveListFragment.doNew();
				mode.finish();
				return true;
			default:
				break;
		}
		return false;
	}

	@Override
	public boolean onCreateActionMode(@NonNull ActionMode mode, @NonNull Menu menu) {
		Timber.d("onCreateActionMode called! This is gonna break stuff..");
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.contextual, menu);
		Context context = mShaveListFragment
				.getActivity()
				.getApplicationContext();
		TextDrawable drawableRazor = TextDrawableFactory.createIcon(context, TextDrawableFactory.RAZOR);
		TextDrawable drawableBlade = TextDrawableFactory.createIcon(context, TextDrawableFactory.BLADE);
		menu.findItem(R.id.menu_item_one_more).setIcon(drawableRazor);
		menu.findItem(R.id.menu_item_new).setIcon(drawableBlade);
		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		mShaveListFragment.onDestroyActionMode();
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}
}