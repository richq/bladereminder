package com.github.gist.ssins;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public abstract class EndlessRecyclerOnScrollListener extends RecyclerView.OnScrollListener {

	private int mPreviousTotal = 0; // The total number of items in the dataset after the last load
	private boolean mLoading = true; // True if we are still waiting for the last set of data to load.
	private int mVisibleThreshold = 5; // The minimum amount of items to have below your current scroll position before loading more.
	int mFirstVisibleItem, mVisibleItemCount, mTotalItemCount;

	private int mCurrentPage = 1;

	private final LinearLayoutManager mLinearLayoutManager;

	public EndlessRecyclerOnScrollListener(LinearLayoutManager linearLayoutManager) {
		mLinearLayoutManager = linearLayoutManager;
	}

	@Override
	public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
		super.onScrolled(recyclerView, dx, dy);

		mVisibleItemCount = recyclerView.getChildCount();
		mTotalItemCount = mLinearLayoutManager.getItemCount();
		mFirstVisibleItem = mLinearLayoutManager.findFirstVisibleItemPosition();

		if (mLoading && mTotalItemCount > mPreviousTotal) {
			mLoading = false;
			mPreviousTotal = mTotalItemCount;
		}
		if (!mLoading &&
			(mTotalItemCount - mVisibleItemCount) <= (mFirstVisibleItem + mVisibleThreshold)) {
			// End has been reached
			// Do something
			mCurrentPage++;
			onLoadMore(mCurrentPage);
			mLoading = true;
		}
	}

	public abstract void onLoadMore(int mCurrentPage);
}

