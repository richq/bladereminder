package es.quirk.bladereminder;

import android.annotation.TargetApi;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;

public abstract class VersionedSoundPoolBuilder {

	private static final int NUMBER_OF_CHANNELS = 4;
	public abstract SoundPool createSoundPool();

	private static class LollipopSoundPoolBuilder extends VersionedSoundPoolBuilder {
		@TargetApi(Build.VERSION_CODES.LOLLIPOP)
		public SoundPool createSoundPool() {
			AudioAttributes audioAttributes = new AudioAttributes.Builder()
				.setUsage(AudioAttributes.USAGE_GAME)
				.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
			SoundPool.Builder builder =  new SoundPool.Builder();
			builder.setMaxStreams(NUMBER_OF_CHANNELS);
			builder.setAudioAttributes(audioAttributes);
			return builder.build();
		}
	}

	private static class KitSoundPoolBuilder extends VersionedSoundPoolBuilder {

		@SuppressWarnings("deprecation")
		public SoundPool createSoundPool() {
			return new SoundPool(NUMBER_OF_CHANNELS, AudioManager.STREAM_MUSIC, 0);
		}
	}

	public static VersionedSoundPoolBuilder newInstance() {
		final int sdkVersion = Build.VERSION.SDK_INT;
		VersionedSoundPoolBuilder spb;
		if (sdkVersion >= Build.VERSION_CODES.LOLLIPOP) {
			spb = new LollipopSoundPoolBuilder();
		} else {
			spb = new KitSoundPoolBuilder();
		}
		return spb;
	}
}
