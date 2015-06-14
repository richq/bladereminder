package es.quirk.bladereminder;

import android.content.Context;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import es.quirk.bladereminder.widgets.TextDrawable;
import es.quirk.bladereminder.widgets.TextDrawableFactory;
import timber.log.Timber;

final class NewShaveMenu implements ActionMode.Callback {
	private final ShaveFragment mShaveFragment;

	public NewShaveMenu(ShaveFragment shaveFragment) {
		mShaveFragment = shaveFragment;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_item_one_more:
				mShaveFragment.doOneMore();
				mode.finish();
				return true;
			case R.id.menu_item_new:
				mShaveFragment.doNew();
				mode.finish();
				return true;
			default:
				break;
		}
		return false;
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		Timber.d("onCreateActionMode called! This is gonna break stuff..");
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.contextual, menu);
		Context context = mShaveFragment
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
		mShaveFragment.onDestroyActionMode();
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}
}