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

public final class EditShaveMenu implements ActionMode.Callback {
	private final ShaveListFragment mShaveListFragment;

	public EditShaveMenu(ShaveListFragment shaveListFragment) {
		mShaveListFragment = shaveListFragment;
	}

	@Override
	public boolean onActionItemClicked(@NonNull ActionMode mode, @NonNull MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_item_edit:
				mShaveListFragment.doEdit();
				mode.finish();
				return true;
			case R.id.menu_item_delete:
				mShaveListFragment.doDelete();
				mode.finish();
				return true;
			default:
				break;
		}
		return false;
	}

	@Override
	public boolean onCreateActionMode(@NonNull ActionMode mode, @NonNull Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.editentry, menu);
		Context context = mShaveListFragment
				.getActivity()
				.getApplicationContext();
		TextDrawable editIcon = TextDrawableFactory.createIcon(context, "gmd-edit");
		menu.findItem(R.id.menu_item_edit).setIcon(editIcon);
		TextDrawable deleteIcon = TextDrawableFactory.createIcon(context, "gmd-delete");
		menu.findItem(R.id.menu_item_delete).setIcon(deleteIcon);
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