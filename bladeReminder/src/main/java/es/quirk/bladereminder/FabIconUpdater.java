package es.quirk.bladereminder;

import android.database.ContentObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import es.quirk.bladereminder.contentprovider.ShaveEntryContentProvider;
import es.quirk.bladereminder.fragments.ShaveListFragment;
import timber.log.Timber;

public final class FabIconUpdater extends ContentObserver {

	private final ShaveListFragment mFragment;
	private final Handler mHandler;
	@Nullable
	private static FabIconUpdater sContentObserver;

	private FabIconUpdater(ShaveListFragment f, Handler h) {
		super(h);
		mFragment = f;
		mHandler = h;
	}

	@NonNull
	private static Handler getBackgroundThread() {
		HandlerThread thread = new HandlerThread("FabIconUpdater-worker");
		thread.start();
		return new Handler(thread.getLooper());
	}

	public synchronized static void register(@NonNull ShaveListFragment frag) {
		Timber.d("newInstance called in FabIconUpdater");
		if (sContentObserver == null) {
			sContentObserver = new FabIconUpdater(frag, getBackgroundThread());
			frag.getActivity().getContentResolver().registerContentObserver(
					ShaveEntryContentProvider.CONTENT_URI,
					true,
					sContentObserver);
		}
	}

	public synchronized static void unregister() {
		Timber.d("UNregister called in FabIconUpdater");
		if (sContentObserver == null)
			return;

		sContentObserver.unregisterAndStop();
		sContentObserver = null;
	}

	private void unregisterAndStop() {
		mFragment.getActivity().getContentResolver().unregisterContentObserver(this);
		mHandler.getLooper().quit();
	}

	@Override
	public void onChange(boolean selfChange) {
		mFragment.updateFabIcon();
	}
}
