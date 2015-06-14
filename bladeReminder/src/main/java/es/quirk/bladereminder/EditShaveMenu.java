package es.quirk.bladereminder;

import android.content.Context;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import es.quirk.bladereminder.widgets.TextDrawable;
import es.quirk.bladereminder.widgets.TextDrawableFactory;

final class EditShaveMenu implements ActionMode.Callback {
	private final ShaveFragment mShaveFragment;

	public EditShaveMenu(ShaveFragment shaveFragment) {
		mShaveFragment = shaveFragment;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_item_edit:
				mShaveFragment.doEdit();
				mode.finish();
				return true;
			case R.id.menu_item_delete:
				mShaveFragment.doDelete();
				mode.finish();
				return true;
			default:
				break;
		}
		return false;
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.editentry, menu);
		Context context = mShaveFragment
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
		mShaveFragment.onDestroyActionMode();
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}
}