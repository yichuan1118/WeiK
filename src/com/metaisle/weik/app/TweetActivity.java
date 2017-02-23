package com.metaisle.weik.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.metaisle.util.Util;
import com.metaisle.weik.R;
import com.metaisle.weik.data.Prefs;
import com.metaisle.weik.weibo.TaskHandler;
import com.metaisle.weik.weibo.TweetTask;

public class TweetActivity extends SherlockFragmentActivity {
	public static final int ACTIVITY_CODE_TAKE_PHOTO = 0x234;
	public static final int ACTIVITY_CODE_CHOOSE_PHOTO = 0x235;

	private EditText mTweetEdit;
	private Button mTweetButton;
	private DiscardDialogFragment mDiscardDialogFragment;
	NotificationManager mNotificationManager;

	public static final String KEY_PRE_TEXT = "key_pre_text";
	public static final String KEY_IN_REPLY_TO = "key_in_reply_to";

	private String mPreText;

	private ImageView mPreviewIV;

	private static String mPhotoPath;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tweet);

		mPreviewIV = (ImageView) findViewById(R.id.preview);

		if (getIntent() != null && getIntent().getExtras() != null) {
			mPreText = getIntent().getExtras().getString(KEY_PRE_TEXT);
		}

		mTweetEdit = (EditText) findViewById(R.id.tweet_edit);

		if (mPreText != null) {
			Util.log("mPreText " + mPreText);
			mTweetEdit.setText(mPreText);
		}

		Button cancelButton = (Button) findViewById(R.id.cancel_button);
		mTweetButton = (Button) findViewById(R.id.tweet_button);

		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mTweetEdit.getText().length() == 0) {
					finish();
				} else {
					promptExit();
				}
			}
		});

		mTweetButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sendTweet();
			}
		});

		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_tweet, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_add_photo) {
			Intent intent = new Intent();
			intent.setType("image/*");
			intent.putExtra("return-data", true);
			intent.setAction(Intent.ACTION_PICK);

			startActivityForResult(
					Intent.createChooser(intent, "Select Picture"),
					ACTIVITY_CODE_CHOOSE_PHOTO);

			// PhotoDialogFragment dialog = PhotoDialogFragment.newInstance();
			// dialog.show(getSupportFragmentManager(), "photo");
		}
		return true;
	}

	private void galleryAddPic() {
		Intent mediaScanIntent = new Intent(
				Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		File f = new File(mPhotoPath);
		Uri contentUri = Uri.fromFile(f);
		mediaScanIntent.setData(contentUri);
		this.sendBroadcast(mediaScanIntent);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == ACTIVITY_CODE_TAKE_PHOTO) {
			if (resultCode == RESULT_OK) {
				handleSmallCameraPhoto(data);
				galleryAddPic();
			} else {
				mPhotoPath = null;
			}
		}

		if (requestCode == ACTIVITY_CODE_CHOOSE_PHOTO) {
			if (resultCode == RESULT_OK) {
				try {
					Uri selectedImage = data.getData();

					Util.log("choose " + selectedImage);

					if (selectedImage.toString().startsWith("http")) {
						Toast.makeText(this,
								getString(R.string.local_photo_only),
								Toast.LENGTH_LONG).show();
						return;
					}

					mPhotoPath = getPath(this, selectedImage);

					ContentResolver cr = getContentResolver();
					InputStream in = cr.openInputStream(selectedImage);
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inSampleSize = 8;
					Bitmap thumb = BitmapFactory
							.decodeStream(in, null, options);

					// ---------------------------
					int orientation = 0;
					File imageFile = new File(mPhotoPath);
					ExifInterface exif = new ExifInterface(
							imageFile.getAbsolutePath());
					int rotate = exif.getAttributeInt(
							ExifInterface.TAG_ORIENTATION,
							ExifInterface.ORIENTATION_NORMAL);

					switch (rotate) {
					case ExifInterface.ORIENTATION_ROTATE_270:
						orientation = 270;
						break;
					case ExifInterface.ORIENTATION_ROTATE_180:
						orientation = 180;
						break;
					case ExifInterface.ORIENTATION_ROTATE_90:
						orientation = 90;
						break;
					}

					// Log.v("TweetActivity ", "" + orientation);

					if (orientation > 0) {
						Matrix matrix = new Matrix();
						matrix.postRotate(orientation);

						thumb = Bitmap.createBitmap(thumb, 0, 0,
								thumb.getWidth(), thumb.getHeight(), matrix,
								true);
					}

					// ---------------------------

					mPreviewIV.setImageBitmap(thumb);
				} catch (URISyntaxException e) {
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				mPhotoPath = null;
			}
		}

		Util.log("mPhotoPath " + mPhotoPath);
		super.onActivityResult(requestCode, resultCode, data);
	}

	public static String getPath(Context context, Uri uri)
			throws URISyntaxException {
		if ("content".equalsIgnoreCase(uri.getScheme())) {
			String[] projection = { "_data" };
			Cursor cursor = null;

			try {
				cursor = context.getContentResolver().query(uri, projection,
						null, null, null);
				int column_index = cursor.getColumnIndexOrThrow("_data");
				if (cursor.moveToFirst()) {
					return cursor.getString(column_index);
				}
			} catch (Exception e) {
				// Eat it
			}
		}

		else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}

		return null;
	}

	private void handleSmallCameraPhoto(Intent intent) {
		Bundle extras = intent.getExtras();

		Util.log("extras " + extras.toString());

		Bitmap bmp = (Bitmap) extras.get("data");
		mPreviewIV.setImageBitmap(bmp);
	}

	@Override
	public void onBackPressed() {
		if (mTweetEdit.getText().toString().length() == 0) {
			super.onBackPressed();
		} else {
			promptExit();
		}
	}

	@SuppressWarnings("deprecation")
	private void sendTweet() {
		String status_text = mTweetEdit.getText().toString();

		if (status_text.length() == 0) {
			return;
		}
		Util.log("tweeting " + status_text);
		mTweetEdit.setEnabled(false);
		mTweetButton.setEnabled(false);

		new TweetTask(getApplicationContext(), status_text, mPhotoPath,
				new TweetHandler(getApplicationContext())).execute();

		CharSequence tickerText = getString(R.string.sending);
		long when = System.currentTimeMillis();
		Notification notification = new Notification(R.drawable.ic_launcher,
				tickerText, when);
		Intent notificationIntent = new Intent();
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(this, getString(R.string.app_name),
				getString(R.string.sending), contentIntent);
		mNotificationManager.notify(Prefs.NOTIFICATION_TWEET_ID, notification);

		finish();
	}

	class TweetHandler extends TaskHandler {

		public TweetHandler(Context context) {
			super(context);
		}

		@SuppressWarnings("deprecation")
		@Override
		protected void RunSuccess() {
			mNotificationManager.cancel(Prefs.NOTIFICATION_TWEET_ID);

			CharSequence tickerText = getString(R.string.send_success);
			long when = System.currentTimeMillis();
			Notification notification = new Notification(
					R.drawable.ic_launcher, tickerText, when);
			Intent notificationIntent = new Intent();
			PendingIntent contentIntent = PendingIntent.getActivity(
					TweetActivity.this, 0, notificationIntent, 0);
			notification.setLatestEventInfo(TweetActivity.this, "", "",
					contentIntent);
			mNotificationManager.notify(Prefs.NOTIFICATION_TWEET_ID,
					notification);

			mNotificationManager.cancel(Prefs.NOTIFICATION_TWEET_ID);
		}

		@SuppressWarnings("deprecation")
		@Override
		protected void RunFail() {
			mNotificationManager.cancel(Prefs.NOTIFICATION_TWEET_ID);

			CharSequence tickerText = getString(R.string.send_failed);
			long when = System.currentTimeMillis();
			Notification notification = new Notification(
					R.drawable.ic_launcher, tickerText, when);
			Intent notificationIntent = new Intent();
			PendingIntent contentIntent = PendingIntent.getActivity(
					TweetActivity.this, 0, notificationIntent, 0);
			notification.setLatestEventInfo(TweetActivity.this, "", "",
					contentIntent);
			mNotificationManager.notify(Prefs.NOTIFICATION_TWEET_ID,
					notification);

			mNotificationManager.cancel(Prefs.NOTIFICATION_TWEET_ID);
		}

	}

	private void promptExit() {
		mDiscardDialogFragment = DiscardDialogFragment
				.newInstance(getString(R.string.discard_prompt));
		mDiscardDialogFragment.show(getSupportFragmentManager(), "discard");
	}

	public static class DiscardDialogFragment extends DialogFragment {

		public static DiscardDialogFragment newInstance(String title) {
			DiscardDialogFragment frag = new DiscardDialogFragment();
			Bundle args = new Bundle();
			args.putString("title", title);
			frag.setArguments(args);
			return frag;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			String title = getArguments().getString("title");

			return new AlertDialog.Builder(getActivity())
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(title)
					.setPositiveButton(getString(R.string.discard),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									getActivity().finish();
								}
							})
					.setNegativeButton(getString(R.string.cancel),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									dismiss();
								}
							}).create();
		}
	}

	public static class PhotoDialogFragment extends DialogFragment {

		public static PhotoDialogFragment newInstance() {
			PhotoDialogFragment frag = new PhotoDialogFragment();
			return frag;
		}

		public static boolean isIntentAvailable(Context context, String action) {
			final PackageManager packageManager = context.getPackageManager();
			final Intent intent = new Intent(action);
			List<ResolveInfo> list = packageManager.queryIntentActivities(
					intent, PackageManager.MATCH_DEFAULT_ONLY);
			return list.size() > 0;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			if (!isIntentAvailable(getActivity(),
					MediaStore.ACTION_IMAGE_CAPTURE)) {
				return new AlertDialog.Builder(getActivity())
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setTitle(getString(R.string.weibo_picture))

						.setNegativeButton(
								getString(R.string.choose_existing_photo),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										Intent intent = new Intent();
										intent.setType("image/*");
										intent.putExtra("return-data", true);
										intent.setAction(Intent.ACTION_PICK);

										getActivity().startActivityForResult(
												Intent.createChooser(intent,
														"Select Picture"),
												ACTIVITY_CODE_CHOOSE_PHOTO);

										// getActivity()
										// .startActivityForResult(
										// new Intent(
										// Intent.ACTION_PICK,
										// android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
										// ACTIVITY_CODE_CHOOSE_PHOTO);
									}
								}).create();
			} else {

				return new AlertDialog.Builder(getActivity())
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setTitle(getString(R.string.weibo_picture))
						.setPositiveButton(getString(R.string.take_new_photo),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										Intent takePictureIntent = new Intent(
												MediaStore.ACTION_IMAGE_CAPTURE);

										File storageDir = new File(
												Environment
														.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
												"WeiK");

										// Create an image file name
										String timeStamp = new SimpleDateFormat(
												"yyyyMMdd_HHmmss")
												.format(new Date());
										String imageFileName = "DCIM_"
												+ timeStamp + "_";
										mPhotoPath = null;
										try {
											File image = File.createTempFile(
													imageFileName, ".JPG",
													storageDir);
											mPhotoPath = image
													.getAbsolutePath();
											takePictureIntent.putExtra(
													MediaStore.EXTRA_OUTPUT,
													Uri.fromFile(image));
										} catch (IOException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}

										getActivity().startActivityForResult(
												takePictureIntent,
												ACTIVITY_CODE_TAKE_PHOTO);
									}
								})
						.setNegativeButton(
								getString(R.string.choose_existing_photo),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										getActivity()
												.startActivityForResult(
														new Intent(
																Intent.ACTION_PICK,
																android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI),
														ACTIVITY_CODE_CHOOSE_PHOTO);
									}
								}).create();
			}
		}
	}
}
