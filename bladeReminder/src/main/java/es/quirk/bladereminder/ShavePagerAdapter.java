package es.quirk.bladereminder;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;

import android.view.ViewGroup;
import es.quirk.bladereminder.database.DataSource;
import es.quirk.bladereminder.fragments.ShaveListFragment;

public class ShavePagerAdapter extends FragmentStatePagerAdapter {
	final AppCompatActivity mActivity;
	private final SparseArray<Fragment> mPageRef = new SparseArray<Fragment>();
	private final SparseArray<String> mPageTitles = new SparseArray<String>();
	private int mCount = -1;

	public interface IRazorCountChangeListener {
		void notifyRazorCountChange(int newcount);
	}

	public ShavePagerAdapter(FragmentManager fm, AppCompatActivity activity) {
		super(fm);
		mActivity = activity;
	}

	@Override
	public Fragment getItem(int i) {
		ShaveListFragment fragment = new ShaveListFragment();
		fragment.setPagerAdapter(this);
		mPageRef.put(i, fragment);
		Bundle args = new Bundle();
		args.putInt(ShaveListFragment.ARG_RAZOR, i);
		fragment.setArguments(args);
		return fragment;
	}

	private void invalidateCachedValues() {
		mCount = -1;
		mPageTitles.clear();
	}

	@Override
	public void notifyDataSetChanged() {
		invalidateCachedValues();
		super.notifyDataSetChanged();
		((IRazorCountChangeListener)mActivity).notifyRazorCountChange(getCount());
	}

	@Override
	public void destroyItem (ViewGroup container, int position, Object object) {
		super.destroyItem(container, position, object);
		mPageRef.remove(position);
	}

	@Override
	public int getCount() {
		if (mCount == -1) {
			DataSource dataSource = new DataSource(mActivity.getApplicationContext());
			int count = dataSource.getRazorCount();
			if (count == 0)
				count = 1;
			mCount = count;
		}
		return mCount;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		String razor = mPageTitles.get(position);
		if (razor == null) {
			DataSource dataSource = new DataSource(mActivity.getApplicationContext());
			razor = dataSource.getRazor(position);
			mPageTitles.put(position, razor);
		}
		return razor;
	}

	public Fragment getFragment(int key) {
		return mPageRef.get(key);
	}
}

