package org.abstractidea.test.pinch;

import org.andengine.engine.camera.ZoomCamera;
import org.andengine.entity.scene.Scene;
import org.andengine.input.touch.TouchEvent;
import org.andengine.input.touch.detector.PinchZoomDetector;
import org.andengine.input.touch.detector.PinchZoomDetector.IPinchZoomDetectorListener;


public class HudZoomListener implements IPinchZoomDetectorListener {

	public static final float MAX_ZOOM = 4f;
	public static final float MIN_ZOOM = 1f;

	private ZoomCamera zoomCam;
	private Scene mScene;

	private float prevX;
	private float prevY;
	private float zoomFactorAtStart;

	public HudZoomListener(ZoomCamera cam, Scene hostScene) {
		zoomCam = cam;
		mScene = hostScene;

	}

	public void onPinchZoomStarted(PinchZoomDetector pPinchZoomDetector,
			TouchEvent pSceneTouchEvent) {
		zoomFactorAtStart = zoomCam.getZoomFactor();
		prevX = ((pSceneTouchEvent.getMotionEvent().getX(0) / zoomFactorAtStart) + (pSceneTouchEvent
				.getMotionEvent().getX(1) / zoomFactorAtStart)) / 2;
		prevY = ((pSceneTouchEvent.getMotionEvent().getY(0) / zoomFactorAtStart) + (pSceneTouchEvent
				.getMotionEvent().getY(1) / zoomFactorAtStart)) / 2;

	}

	public void onPinchZoom(PinchZoomDetector pPinchZoomDetector,
			TouchEvent pTouchEvent, float pZoomFactor) {

		float newX = ((pTouchEvent.getMotionEvent().getX(0) / this.zoomFactorAtStart) + (pTouchEvent
				.getMotionEvent().getX(1) / this.zoomFactorAtStart)) / 2;
		float newY = ((pTouchEvent.getMotionEvent().getY(0) / this.zoomFactorAtStart) + (pTouchEvent
				.getMotionEvent().getY(1) / this.zoomFactorAtStart)) / 2;

		this.zoomCam.offsetCenter((prevX - (newX)), (prevY - newY));

		prevX = newX;
		prevY = newY;
		// this.mZoomCamera.setZoomFactor(this.mPinchZoomStartedCameraZoomFactor
		// * pZoomFactor);
		if (this.zoomFactorAtStart * pZoomFactor > MAX_ZOOM) {
			this.zoomCam.setZoomFactor(MAX_ZOOM);
		} else if (this.zoomFactorAtStart * pZoomFactor < MIN_ZOOM) {
			this.zoomCam.setZoomFactor(MIN_ZOOM);
		} else {
			this.zoomCam.setZoomFactor(this.zoomFactorAtStart * pZoomFactor);
		}

	}

	public void onPinchZoomFinished(PinchZoomDetector pPinchZoomDetector,
			TouchEvent pTouchEvent, float pZoomFactor) {
		if (this.zoomFactorAtStart * pZoomFactor > MAX_ZOOM) {
			this.zoomCam.setZoomFactor(MAX_ZOOM);
		} else if (this.zoomFactorAtStart * pZoomFactor < MIN_ZOOM) {
			this.zoomCam.setZoomFactor(MIN_ZOOM);
		} else {
			this.zoomCam
					.setZoomFactor(this.zoomFactorAtStart
							* pZoomFactor);
		}
	}

}
