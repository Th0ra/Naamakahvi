package naamakahvi.android;

import java.io.ByteArrayOutputStream;
import java.util.List;

import naamakahvi.android.components.FaceDetectView;
import naamakahvi.android.utils.Basket;
import naamakahvi.android.utils.Config;
import naamakahvi.android.utils.ExtraNames;
import naamakahvi.naamakahviclient.Client;
import naamakahvi.naamakahviclient.ClientException;
import naamakahvi.naamakahviclient.IStation;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

public class RecogActivity extends Activity {
	public static final short SELECT_USERNAME = 0;
	private Basket mOrder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recog_user);
		FaceDetectView face = (FaceDetectView) findViewById(R.id.faceDetectView1);
		face.openCamera();
		mOrder = getIntent().getParcelableExtra(ExtraNames.PRODUCTS);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		FaceDetectView face = (FaceDetectView) findViewById(R.id.faceDetectView1);
		face.releaseCamera();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		FaceDetectView face = (FaceDetectView) findViewById(R.id.faceDetectView1);
		face.openCamera();
	}

	public void userListClick(View v) {
		Intent i = new Intent(this, LoginwithusernameActivity.class);
		startActivityForResult(i, SELECT_USERNAME);
	}

	public void onBackClick(View v) {
		final Context con = this;
		final Handler hand = new Handler(getMainLooper());
		new Thread(new Runnable() {

			public void run() {
				FaceDetectView face = (FaceDetectView) findViewById(R.id.faceDetectView1);
				List<IStation> s;
				try {
					s = Client.listStations(Config.SERVER_URL,
							Config.SERVER_PORT);
					
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					face.grabFrame().compress(CompressFormat.PNG, 0, bos);
					byte[] bitmapdata = bos.toByteArray();
					
					Client client = new Client(Config.SERVER_URL,
							Config.SERVER_PORT, s.get(0));

					final String[] users = client.identifyImage(bitmapdata);

					hand.post(new Runnable() {
						public void run() {
							Intent i = new Intent();
							i.putExtra(ExtraNames.USERS, users);
							i.putExtra(ExtraNames.PRODUCTS, mOrder);
							setResult(RESULT_OK, i);
							finish();
						}
					});

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}).start();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SELECT_USERNAME) {
			switch (resultCode) {
			case RESULT_OK:
				Intent i = new Intent();
				i.putExtra(ExtraNames.USERS, new String[] { data.getExtras()
						.getString(ExtraNames.SELECTED_USER)});
				i.putExtra(ExtraNames.PRODUCTS, mOrder);
				setResult(RESULT_OK, i);
				finish();
				break;
			case RESULT_CANCELED:
				break;
			}

		}
	}

}