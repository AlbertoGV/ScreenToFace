package com.tec.fontsize;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.tec.fontsize.messages.MeasurementStepMessage;
import com.tec.fontsize.messages.MessageHUB;
import com.tec.fontsize.messages.MessageListener;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity implements MessageListener {

	public static final String CAM_SIZE_WIDTH = "intent_cam_size_width";
	public static final String CAM_SIZE_HEIGHT = "intent_cam_size_height";
	public static final String AVG_NUM = "intent_avg_num";
	public static final String PROBANT_NAME = "intent_probant_name";
	private static final int MY_PERMISSION_REQUEST_CAMERA = 80;

	private CameraSurfaceView _mySurfaceView;
	static Camera _cam;

	private final static DecimalFormat _decimalFormater = new DecimalFormat(
			"0.0");

	private float _currentDevicePosition;

	private int _cameraHeight;
	private int _cameraWidth;
	private int _avgNum;

	TextView _currentDistanceView;
	Button _calibrateButton;

	/**
	 * Abusing the media controls to create a remote control
	 */
	// ComponentName _headSetButtonReceiver;
	// AudioManager _audioManager;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.measurement_activity);
		openFrontFacing();
		if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CAMERA}, 50);
		}

		_mySurfaceView = (CameraSurfaceView) findViewById(R.id.surface_camera);

		RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(
				(int) (0.95 * this.getResources().getDisplayMetrics().widthPixels),
				(int) (0.6 * this.getResources().getDisplayMetrics().heightPixels));

		layout.setMargins(0, (int) (0.05 * this.getResources()
				.getDisplayMetrics().heightPixels), 0, 0);

		_mySurfaceView.setLayoutParams(layout);
		_currentDistanceView = (TextView) findViewById(R.id.currentDistance);
		_calibrateButton = (Button) findViewById(R.id.calibrateButton);

		// _audioManager = (AudioManager) this
		// .getSystemService(Context.AUDIO_SERVICE);
	}

	@Override
	protected void onResume() {
		super.onResume();

//
		// _audioManager.registerMediaButtonEventReceiver(_headSetButtonReceiver);

			MessageHUB.get().registerListener(this);
			_mySurfaceView.setCamera(openFrontFacing());

	}
	private Camera openFrontFacing() {
		int cameraCount = 0;

		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		cameraCount = Camera.getNumberOfCameras();
		for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
			Camera.getCameraInfo(camIdx, cameraInfo);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				try {
					_cam = Camera.open(camIdx);
				} catch (RuntimeException e) {
					Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage());
				}
			}
		}

		return _cam;
	}

	@Override
	protected void onPause() {
		super.onPause();

		MessageHUB.get().unregisterListener(this);

		// _audioManager
		// .unregisterMediaButtonEventReceiver(_headSetButtonReceiver);

		resetCam();
	}

	/**
	 * Sets the current eye distance to the calibration point.
	 * 
	 * @param v
	 */
	public void pressedCalibrate(final View v) {

		if (!_mySurfaceView.isCalibrated()) {

			_calibrateButton.setBackgroundResource(R.drawable.yellow_button);
			_mySurfaceView.calibrate();
		}
	}

	public void pressedReset(final View v) {

		if (_mySurfaceView.isCalibrated()) {

			_calibrateButton.setBackgroundResource(R.drawable.red_button);
			_mySurfaceView.reset();
		}
	}

	public void onShowMiddlePoint(final View view) {
		// Is the toggle on?
		boolean on = ((Switch) view).isChecked();
		_mySurfaceView.showMiddleEye(on);
	}

	public void onShowEyePoints(final View view) {
		// Is the toggle on?
		boolean on = ((Switch) view).isChecked();
		_mySurfaceView.showEyePoints(on);
	}




	@Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
													 @NonNull int[] grantResults) {
		if (requestCode != MY_PERMISSION_REQUEST_CAMERA) {
			return;
		}

		if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			Snackbar.make(this.findViewById(android.R.id.content), "Camera permission was granted. Now you can scan QR code", Snackbar.LENGTH_SHORT).show();

		} else {
			Snackbar.make(this.findViewById(android.R.id.content), "Camera permission request was denied, Can't able to start QR scan", Snackbar.LENGTH_SHORT)
					.show();
		}
	}

	/**
	 * Update the UI values.
	 * 
//	 * @param eyeDist
//	 * @param frameTime
	 */
	public void updateUI(final MeasurementStepMessage message) {

		_currentDistanceView.setText(_decimalFormater.format(message
				.getDistToFace()) + " cm");

		float fontRatio = message.getDistToFace() / 29.7f;

		_currentDistanceView.setTextSize(fontRatio * 5);

	}

	private void resetCam() {
		_mySurfaceView.reset();

		_cam.stopPreview();
		_cam.setPreviewCallback(null);
		_cam.release();
	}

	@Override
	public void onMessage(final int messageID, final Object message) {

		switch (messageID) {

		case MessageHUB.MEASUREMENT_STEP:
			updateUI((MeasurementStepMessage) message);
			break;

		case MessageHUB.DONE_CALIBRATION:

			_calibrateButton.setBackgroundResource(R.drawable.green_button);

			break;
		default:
			break;
		}

	}
}