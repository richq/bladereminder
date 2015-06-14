package es.quirk.bladereminder;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.preference.PreferenceManager;
import android.util.SparseIntArray;

import com.google.common.collect.Sets;

import java.util.HashSet;

class SoundHelper implements OnLoadCompleteListener {
	private final SoundPool mSoundPool = VersionedSoundPoolBuilder.newInstance().createSoundPool();
	private final SparseIntArray mResourceIdToSoundIdMap = new SparseIntArray();
	private final HashSet<Integer> mLoaded = Sets.newHashSetWithExpectedSize(3);
	private final Context mContext;

	public SoundHelper(Context context) {
		mContext = context;
		mSoundPool.setOnLoadCompleteListener(this);
		loadSound(R.raw.delete);
		loadSound(R.raw.newping);
		loadSound(R.raw.plusone);
	}

	private void loadSound(int identifier) {
		Integer soundID = mSoundPool.load(mContext, identifier, 1);
		mResourceIdToSoundIdMap.put(identifier, soundID);
	}

	@Override
	public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
		mLoaded.add(sampleId);
	}

	public void playRawSound(int resourceID) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		boolean enabled = prefs.getBoolean("sounds_enabled", false);
		if (!enabled)
			return;
		AudioManager audioManager = (AudioManager) mContext.getSystemService( Context.AUDIO_SERVICE);
		float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		float volume = actualVolume / (2 * maxVolume);
		int soundID = mResourceIdToSoundIdMap.get(resourceID);
		// Is the sound loaded already?
		if (mLoaded.contains(soundID)) {
			mSoundPool.play(soundID, volume, volume, 1, 0, 1f);
		}
	}
}
