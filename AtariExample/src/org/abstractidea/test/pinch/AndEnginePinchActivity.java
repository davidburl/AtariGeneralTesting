package org.abstractidea.test.pinch;

import java.util.HashMap;

import org.andengine.engine.camera.ZoomCamera;
import org.andengine.engine.camera.hud.HUD;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.primitive.Line;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.Text.TextOptions;
import org.andengine.entity.util.FPSLogger;
import org.andengine.input.touch.TouchEvent;
import org.andengine.input.touch.controller.MultiTouch;
import org.andengine.input.touch.detector.PinchZoomDetector;
import org.andengine.input.touch.detector.PinchZoomDetector.IPinchZoomDetectorListener;
import org.andengine.input.touch.detector.ScrollDetector;
import org.andengine.input.touch.detector.ScrollDetector.IScrollDetectorListener;
import org.andengine.input.touch.detector.SurfaceScrollDetector;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TextureRegionFactory;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.HorizontalAlign;

import android.graphics.Typeface;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

/**
 * (c) 2010 Nicolas Gramlich (c) 2011 Zynga
 * 
 * @author Nicolas Gramlich
 * @since 15:44:58 - 05.11.2010
 */
public class AndEnginePinchActivity extends SimpleBaseGameActivity implements
		IOnSceneTouchListener {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int CAMERA_WIDTH = 480;
	private static final int CAMERA_HEIGHT = 800;

	// NUM_ROWSCOLS: The number of rows and columns to draw
	private static final int NUM_ROWSCOLS = 19;
	// BORDER_SIZE: The size of the edge
	private static final float BORDER_SIZE = 0.01f;
	// TOP_BORDER: The top border (leaving room for a HUD)
	private static final float TOP_BORDER = 0.25f * CAMERA_HEIGHT;
	// FIRST_SPACE : first space on the board
	private static final float FIRST_SPACE = BORDER_SIZE * CAMERA_WIDTH;
	// SPACING : Amount of space between each row
	private static final float SPACING = (CAMERA_WIDTH * (1 - (2 * BORDER_SIZE)))
			/ NUM_ROWSCOLS;

	private static final float MAX_ZOOM = 4f;
	private static final float MIN_ZOOM = 1f;

	// Start at BORDER_SIZE*CAMERA_WIDTH
	// Width between each:
	//

	// ===========================================================
	// Fields
	// ===========================================================

	private ZoomCamera mZoomCamera;

	private Scene mScene;

	private PinchZoomDetector mPinchZoomDetector;
	private float mPinchZoomStartedCameraZoomFactor;
	private Font mFont;
	private Text centerText;
	private float prevX;
	private float prevY;

	private HUDLoader hudLoader;

	private boolean selecting = false;

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	public EngineOptions onCreateEngineOptions() {
		this.mZoomCamera = new ZoomCamera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

		final EngineOptions engineOptions = new EngineOptions(true,
				ScreenOrientation.PORTRAIT_FIXED, new RatioResolutionPolicy(
						CAMERA_WIDTH, CAMERA_HEIGHT), this.mZoomCamera);

		return engineOptions;
	}

	@Override
	public void onCreateResources() {
		this.mFont = FontFactory.create(this.getFontManager(),
				this.getTextureManager(), 256, 256,
				Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 16);
		this.mFont.load();

		hudLoader = new HUDLoader("default",
				this.getVertexBufferObjectManager(), this.getTextureManager(),
				this.getAssets(), mZoomCamera);
		
	
	}

	@Override
	public Scene onCreateScene() {
		this.mEngine.registerUpdateHandler(new FPSLogger());

		this.mScene = new Scene();


		this.mScene.setOnAreaTouchTraversalFrontToBack();

		Rectangle easyRect = new Rectangle(FIRST_SPACE, FIRST_SPACE
				+ TOP_BORDER, CAMERA_WIDTH * (1 - (2 * BORDER_SIZE)),
				(CAMERA_WIDTH * (1 - (2 * BORDER_SIZE))),
				this.getVertexBufferObjectManager());
		easyRect.setColor(1, 1, 1);

		this.mScene.setBackground(new Background(0.09804f, 0.6274f, 0.8784f));

		mScene.attachChild(easyRect);

		;
		float pos_at = FIRST_SPACE;
		for (int i = 0; i <= NUM_ROWSCOLS; i++) {
			Line boardLineVert = new Line(pos_at, FIRST_SPACE + TOP_BORDER,
					pos_at, (CAMERA_WIDTH * (1 - BORDER_SIZE)) + TOP_BORDER,
					this.getVertexBufferObjectManager());
			Line boardLineHorz = new Line(FIRST_SPACE, pos_at + TOP_BORDER,
					CAMERA_WIDTH * (1 - BORDER_SIZE), pos_at + TOP_BORDER,
					this.getVertexBufferObjectManager());
			boardLineVert.setColor(0, 0, 0);
			boardLineHorz.setColor(0, 0, 0);
			mScene.attachChild(boardLineHorz);
			mScene.attachChild(boardLineVert);
			pos_at = pos_at + SPACING;
		}

		 hudLoader.addTouchToScene(mScene, mZoomCamera);
		
		this.mScene.setTouchAreaBindingOnActionDownEnabled(true);

		return this.mScene;
	}

	public void onPinchZoom(final PinchZoomDetector pPinchZoomDetector,
			final TouchEvent pTouchEvent, final float pZoomFactor) {

		final float zoomFactor = this.mZoomCamera.getZoomFactor();
		float newX = ((pTouchEvent.getMotionEvent().getX(0) / this.mPinchZoomStartedCameraZoomFactor) + (pTouchEvent
				.getMotionEvent().getX(1) / this.mPinchZoomStartedCameraZoomFactor)) / 2;
		float newY = ((pTouchEvent.getMotionEvent().getY(0) / this.mPinchZoomStartedCameraZoomFactor) + (pTouchEvent
				.getMotionEvent().getY(1) / this.mPinchZoomStartedCameraZoomFactor)) / 2;

		this.mZoomCamera.offsetCenter((prevX - (newX)), (prevY - newY));

		prevX = newX;
		prevY = newY;
		// this.mZoomCamera.setZoomFactor(this.mPinchZoomStartedCameraZoomFactor
		// * pZoomFactor);
		if (this.mPinchZoomStartedCameraZoomFactor * pZoomFactor > MAX_ZOOM) {
			this.mZoomCamera.setZoomFactor(MAX_ZOOM);
		} else if (this.mPinchZoomStartedCameraZoomFactor * pZoomFactor < MIN_ZOOM) {
			this.mZoomCamera.setZoomFactor(MIN_ZOOM);
		} else {
			this.mZoomCamera
					.setZoomFactor(this.mPinchZoomStartedCameraZoomFactor
							* pZoomFactor);
		}

		/*centerText.setText("(" + mZoomCamera.getCenterX() + ","
				+ mZoomCamera.getCenterY() + ")\n" + "zoomFactor = "
				+ zoomFactor + "\n");*/
	}

	public void onPinchZoomFinished(final PinchZoomDetector pPinchZoomDetector,
			final TouchEvent pTouchEvent, final float pZoomFactor) {
		if (this.mPinchZoomStartedCameraZoomFactor * pZoomFactor > MAX_ZOOM) {
			this.mZoomCamera.setZoomFactor(MAX_ZOOM);
		} else if (this.mPinchZoomStartedCameraZoomFactor * pZoomFactor < MIN_ZOOM) {
			this.mZoomCamera.setZoomFactor(MIN_ZOOM);
		} else {
			this.mZoomCamera
					.setZoomFactor(this.mPinchZoomStartedCameraZoomFactor
							* pZoomFactor);
		}
	}

	public boolean onSceneTouchEvent(final Scene pScene,
			final TouchEvent pSceneTouchEvent) {
		Log.d("ATARI", "sceneTouched");
		this.mPinchZoomDetector.onTouchEvent(pSceneTouchEvent);
		if (!this.mPinchZoomDetector.isZooming()) {
			// check for a button press
			final int action = pSceneTouchEvent.getMotionEvent().getAction()
					& MotionEvent.ACTION_MASK;

			// centerText.setText(String.valueOf(action));

			switch (action) {

			// note that these methods will activate even if
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				centerText.setText("Pointer Down.");
				selecting = true;
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				if (selecting) {
					centerText.setText("Would have placed");
				}
				selecting = false;
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_OUTSIDE:
				centerText.setText("Pointer Done.");
				selecting = false;
				break;
			case MotionEvent.ACTION_MOVE:
				if (selecting) {
					centerText.setText("Pointer Move.");
				}
				break;
			default:
				throw new IllegalArgumentException("Invalid Action detected: "
						+ action);
			}
		} else {
			selecting = false;
		}
		return true;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}