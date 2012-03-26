package org.abstractidea.test.pinch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.andengine.engine.camera.ZoomCamera;
import org.andengine.engine.camera.hud.HUD;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.Sprite;
import org.andengine.input.touch.TouchEvent;
import org.andengine.input.touch.detector.PinchZoomDetector;
import org.andengine.input.touch.detector.PinchZoomDetector.IPinchZoomDetectorListener;
import org.andengine.opengl.texture.TextureManager;
import org.andengine.opengl.texture.bitmap.BitmapTexture;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TextureRegionFactory;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.adt.io.in.IInputStreamOpener;

import android.content.res.AssetManager;
import android.util.Log;

/**
 * HUDLoader allows the program to load the HUD. HUDLoader should be declared
 * during onCreateResources(), as it loads the HUD textures into Memory.
 * 
 * All textures must be 800x600, the default resolution the program renders at.
 * 
 * The configuration for the HUD is located in {@code PATH_TO_HUD_CFG}. An
 * example file can be found below:
 * 
 * {@code [levelname]
 * path=path/to/hud.png
 * x1,y1,width1,height1
 * x2,y2,width2,height2
 * [levelname2]
 * pat=path/to/hud2.png
 * x1,y1,width1,height1
 * x2,y2,width2,height2}
 * 
 * Note: This file is designed as a developers tool, and as such is not
 * resilient to mistyping.
 * 
 * @author David Burl
 */

// TODO: the ability to turn touch mask red w/ alpha .5 if a flag in the cfg is
// set
public class HUDLoader {

	/**
	 * The path to the HUD configuration file
	 */
	public static final String PATH_TO_HUD_CFG = "cfg/hud.cfg";
	/**
	 * The name of the HUD, as found in the configuration file
	 */
	private String name;
	/**
	 * The path to the texture for HUD name
	 */
	private String texturePath;
	/**
	 * A HUD object, which can be passed to the Scene in order to be drawn
	 */
	private HUD mHud;
	/**
	 * Array of all Rectangle based area that need to be touch enabled
	 */
	private ArrayList<Rectangle> mTouchMask;
	/**
	 * The texture for the HUD
	 */
	private BitmapTexture hudTexture;

	/**
	 * The TextureRegion that we need to display
	 */
	private ITextureRegion hudTextureRegion;
	/**
	 * VertexBufferObjectManager, used when loading rectangles into memory
	 */
	private VertexBufferObjectManager sceneVBOM;
	/**
	 * TextureManager, used when loading HUD textures into memory
	 */
	private TextureManager sceneTM;
	/**
	 * AssetManager, used to load assets from the program folder into our
	 * program.
	 */
	private AssetManager sceneAM;
	/**
	 * The Camera to attach the HUD too.
	 */

	/**
	 * The PinchZoomListener which detects and governs movement around the board
	 */
	private IPinchZoomDetectorListener hudPinchZoomListener;

	/**
	 * The pinchzoom detector, which is fed an even and determines if it's a
	 * pinching event. If it is, it will call hudPinchZoomListener
	 */
	private PinchZoomDetector hudPinchZoomDetector;

	/**
	 * The Scene the hud is over; used so the HUD can modify the scene with
	 * touchAreas.
	 */
	private Scene mScene;

	/**
	 * The camera that the hud is binded too; used so the HUD can bind itself to
	 * the camera, and change the camera's position and zoom.
	 */
	private ZoomCamera mCamera;

	/**
	 * @param hudName
	 *            The name of the HUD from the configuration in PATH_TO_HUD_CFG
	 *            to load
	 * @param VBOM
	 *            The Vertex Buffer Object Manager, which is used when loading
	 *            rectangles into memory
	 * @param TM
	 *            The Texture Manager, used when loading the HUD textures into
	 *            memory.
	 * @param AM
	 *            The assetManager used to load assets from the game's
	 *            directory.
	 * @param camera
	 *            The camera to attach the HUD too.
	 * @throws IOException
	 *             If the hudName does not exist, or if the files pointed too in
	 *             the configuration file at PATH_TO_HUD_CFG do not exist.
	 */

	public HUDLoader(String hudName, VertexBufferObjectManager VBOM,
			TextureManager TM, AssetManager AM, ZoomCamera camera) {

		mHud = new HUD();
		mTouchMask = new ArrayList<Rectangle>();
		sceneVBOM = VBOM;
		sceneTM = TM;
		name = hudName;
		sceneAM = AM;
		

		// Now Read all the information from the file, creating the rectangles
		// in mTouchMask and loading location of the texture into PathToHudImg

		readFile();

		// now load the texture found in PathToHudImg

		loadTextures();

		// everything is loaded, attach the HUD to the camera

		camera.setHUD(mHud);

	}

	/**
	 * Must be called before the first possible touch in order to register the
	 * TouchListeners
	 * 
	 * @param mainScene
	 *            The scene to add touch too.
	 */
	public void addTouchToScene(Scene mainScene, ZoomCamera newZoom) {
		// loop through all the rectangles, add touch to all of them
		for (int i = 0; i < mTouchMask.size(); i++) {
			mHud.registerTouchArea(mTouchMask.get(i));
		}
		
		mHud.setTouchAreaBindingOnActionDownEnabled(true);
		// now add the pinch zoom listeners
		// Note: need the use of "newZoom" - can't use the zoomCamera that 
		// was used to create the HUD - too old?
		hudPinchZoomListener = new HudZoomListener(newZoom, mScene);
		hudPinchZoomDetector = new PinchZoomDetector(hudPinchZoomListener);
	}

	/**
	 * Used to clean up the constructor; readFile reads the file from <@code
	 * PATH_TO_HUD_CFG>, adds the rectangles to mTouchMask and loads the hud
	 * texture into hudTexture and hudTextureRegion.
	 */

	private void readFile() {

		// this sections job is to fill in pathToHudImg & touchMaskRegions

		String line = "";
		BufferedReader reader = null;
		// create a bufferedreader from the pathToHudCfg
		try {
			reader = new BufferedReader(new InputStreamReader(
					sceneAM.open(PATH_TO_HUD_CFG)));
		} catch (IOException e) {
			// should this occur, PATH_TO_HUD_CFG is incorrect
			Log.e("ATARI", "Hud config not found: " + PATH_TO_HUD_CFG
					+ " is incorrect.");
			// no need to throw an exception, as the parent function would
			// need to catch a problem that's our fault
		}
		// read the first line
		try {
			line = reader.readLine();
		} catch (IOException e) {
			// should this occur, PATH_TO_HUD_CFG is incorrect
			Log.e("ATARI", "Error Reading Configuration file.");
			// no need to throw an exception, as the parent function would
			// need to catch a problem that's our fault
		}

		// read through the file
		while (line != null) {
			line = line.trim(); // remove any whitespace
			if (line.charAt(0) == '[') {
				// new section
				name = line.substring(1, line.length() - 1);
			} else if (name.equals(name)) {
				// in the correct section, load information
				if (line.substring(0, 5).equals("path=")) {
					texturePath = line.substring(5);
					Log.d("ATARI", "Found path at " + texturePath);
				} else if (line.substring(0, 4).equals("box=")) {
					// line is x,y,width,height
					String tmp = line.substring(4, line.indexOf(','));
					Log.d("ATARI", "Parsing " + tmp);
					// tmp is now x
					float x = Float.valueOf(tmp);
					// remove that first part from line
					line = line.substring(line.indexOf(',') + 1);
					// now have y,width,height
					tmp = line.substring(0, line.indexOf(','));
					Log.d("ATARI", "Parsing " + tmp);
					float y = Float.valueOf(tmp);
					// remove that first part
					line = line.substring(line.indexOf(',') + 1);
					// now have width,height
					tmp = line.substring(0, line.indexOf(','));
					Log.d("ATARI", "Parsing " + tmp);
					float width = Float.valueOf(tmp);
					line = line.substring(line.indexOf(',') + 1);
					// now have height
					Log.d("ATARI", "Parsing " + tmp);
					float height = Float.valueOf(line);
					// create a new rectangle with the coordinates just read
					Rectangle maskBox = new Rectangle(x, y, width, height,
							this.sceneVBOM) {
						public boolean onAreaTouched(
								final TouchEvent pSceneTouchEvent,
								final float pTouchAreaLocalX,
								final float pTouchAreaLocalY) {
							// TODO: this might be changed to a notifier for an
							// event
							onBoxTouchEvent(pSceneTouchEvent);
							//mTouchEventNotifier.notifyDispatcher( pSceneTouchEvent );
							return true;
						}

					};
					Log.d("ATARI", "Added Rectangle");
					mTouchMask.add(maskBox);
					maskBox.setColor(1, 0, 0);
					maskBox.setAlpha(0.4f);
					mHud.attachChild(maskBox);
				}
			}
			try {
				line = reader.readLine();
			} catch (IOException e) {
				// should this occur, PATH_TO_HUD_CFG is incorrect
				Log.e("ATARI", "Error Reading Configuration file.");
				// no need to throw an exception, as the parent function would
				// need to catch a problem that's our fault
			}
		}

	}

	/**
	 * Loads the HUD texture and places it into the HUD. Assumes that the <@code
	 * texturePath> variable has been filled
	 */

	private void loadTextures() {

		try {
			this.hudTexture = new BitmapTexture(this.sceneTM,
					new customizedIInputStreamOpener(texturePath));
		} catch (IOException e) {
			Log.e("ATARI", "Error: texture for hud not found");
		}

		this.hudTexture.load();
		this.hudTextureRegion = TextureRegionFactory
				.extractFromTexture(this.hudTexture);

		Sprite hudSprite = new Sprite(0, 0, hudTextureRegion, sceneVBOM);

		mHud.attachChild(hudSprite);
		
		
	}

	/**
	 * requires scene to have been loaded into the HUDLoader
	 * 
	 * @param sceneTouchEvent
	 */

	private void onBoxTouchEvent(TouchEvent sceneTouchEvent) {
		hudPinchZoomDetector.onTouchEvent(sceneTouchEvent);
	}

	// TODO: write a method to return starting height of the board

	/**
	 * Used to load a custom file via IInputStreamOpener
	 * 
	 * @author David Burl
	 */
	public class customizedIInputStreamOpener implements IInputStreamOpener {

		private String fileToOpen;

		public customizedIInputStreamOpener(String toOpen) {
			fileToOpen = toOpen;
		}

		public InputStream open() throws IOException {
			return sceneAM.open(fileToOpen);
		}
	}

}
