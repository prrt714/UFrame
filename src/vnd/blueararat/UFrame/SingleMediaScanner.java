package vnd.blueararat.UFrame;

import java.io.File;
import java.io.FilenameFilter;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.widget.Toast;

public class SingleMediaScanner implements MediaScannerConnectionClient {

	private MediaScannerConnection mMs;
	private final File mFile;
	private Context mContext;
	private final boolean mShouldOpen;
	private final boolean mShouldRemove;
	private volatile int i = 0;
	private int j;

	public SingleMediaScanner(Context context, File f, boolean open,
			boolean remove) {
		mShouldOpen = open;
		mShouldRemove = remove;
		mContext = context;
		mFile = f;
		mMs = new MediaScannerConnection(context, this);
		mMs.connect();

	}

	@Override
	public void onMediaScannerConnected() {

		File[] files = mFile.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {

				return (filename.endsWith(".jpg") || filename.endsWith(".png"));
			}
		});
		if (files != null)
			j = files.length;
		if (files == null || j == 0) {
			if (mShouldOpen) {
				Toast.makeText(mContext, R.string.nothing_to_open,
						Toast.LENGTH_SHORT).show();
			} else if (mShouldRemove) {
				Toast.makeText(mContext, R.string.nothing_to_delete,
						Toast.LENGTH_SHORT).show();
			}
			return;
		}

		for (File file : files) {
			if (file.isFile()) {
				mMs.scanFile(file.getAbsolutePath(), null);
			}
		}
		if (mShouldRemove) {
			Toast.makeText(mContext,
					j + mContext.getString(R.string.files_were_deleted),
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onScanCompleted(String path, Uri uri) {
		i++;
		if (mShouldRemove && uri != null) {
			mContext.getContentResolver().delete(uri, null, null);
		}
		if (i == j) {
			try {
				if (mShouldOpen && uri != null) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(uri);
					mContext.startActivity(intent);
				}
			} finally {
				mMs.disconnect();
				mMs = null;
			}
		}
	}
}