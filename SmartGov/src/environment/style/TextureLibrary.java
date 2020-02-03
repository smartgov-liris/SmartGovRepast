package environment.style;

import gov.nasa.worldwind.render.PatternFactory;
import gov.nasa.worldwind.render.WWTexture;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import repast.simphony.visualization.gis3D.BufferedImageTexture;
import simulation.Vector2D;

/**
 * TextureLibrary stores static references to all textures used 
 * in the simulation.
 * @author Simon Pageaud
 *
 */
@SuppressWarnings("deprecation")
public class TextureLibrary {

	public static int size;
	public static float scale;
	public static final int NUMBER_OF_ORIENTATION = 90;
	public static final double DELTA_ORIENTATION = (2*Math.PI)/NUMBER_OF_ORIENTATION;
	public static final Vector2D xVector = new Vector2D(1.0, 0.0);

	public static WWTexture defaultTexture;

	public static WWTexture parkingSlotTexture_Occupied;
	public static WWTexture parkingSlotTexture_NotOccupied;
	public static WWTexture parkingSlotTexture_Problem;
	public static WWTexture parkingSlotTexture_Unavailable;

	public static List<WWTexture> agentBodyTexturePack;
	public static List<WWTexture> agentBodyParkedPack;
	public static List<WWTexture> agentBodyWanderPack;
	public static List<WWTexture> agentBodyAwarenessPack;
	public static List<WWTexture> agentBodyCloseToDestinationPack;
	public static List<WWTexture> agentBodyMovingToSpotPack;

	//Node relative texture
	public static WWTexture nodeTexture;
	public static WWTexture spawnTexture;
	public static WWTexture sinkTexture;

	public static WWTexture agentBodyParked;
	public static WWTexture agentBodyWander;
	public static WWTexture agentBodyAwareness;
	public static WWTexture agentBodyTexture;
	public static WWTexture agentBodyCloseToDestination;
	public static WWTexture agentBodyMovingToSpot;
	
	//Building Type Color
	public static Color WORK_OFFICE_COLOR;
	public static Color HOME_COLOR;
	public static Color MIXED_COLOR;
	public static Color SHOP_LEISURE_COLOR;
	
	public static void createTexturesAndColors(String colorType) {
		size = 10;
		scale = 0.7f;

		if(colorType.equals("1")){
			regularDisplayColors();
		} else {
			paperDisplayColors();
		}
		
		TextureLibrary.defaultTexture = createBufferedImage(PatternFactory.PATTERN_SQUARE, Color.red);

		TextureLibrary.nodeTexture = createBufferedImage(PatternFactory.PATTERN_TRIANGLE_UP, Color.green);
		TextureLibrary.spawnTexture = createBufferedImage(PatternFactory.PATTERN_CIRCLE, Color.white);
		TextureLibrary.sinkTexture = createBufferedImage(PatternFactory.PATTERN_TRIANGLE_UP, Color.gray);
	}

	public static BufferedImageTexture createBufferedImage(String pattern, Color color){
		BufferedImage image = PatternFactory.createPattern(pattern, new Dimension(size, size), scale,  color);
		return new BufferedImageTexture(image);
	}

	// Return color triangular texture, oriented according to vector(1.0, 0.0)
	public static BufferedImageTexture createTriangleBufferedImage(Color color){
		// Transparent background
		Color backColor = new Color(0f, 0f, 0f, 0f);
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR); 

		Graphics2D g2 = image.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Background
		g2.setPaint(backColor);
		g2.fillRect(0, 0, size, size);

		// Arrow/triangle
		g2.setPaint(color);
		g2.setStroke(new BasicStroke(scale*scale));
		g2.fillPolygon(new int[] {0,size, 0}, new int[] {1, size/2, size-1}, 3);
		return new BufferedImageTexture(image);
	}

	public static List<WWTexture> createTexturePack(List<WWTexture> texturesToStore, WWTexture initialTextureToStore){
		texturesToStore = new ArrayList<>();
		for(int i = 1; i <= NUMBER_OF_ORIENTATION; i++){
			texturesToStore.add(rotateTexture(initialTextureToStore, (2*Math.PI/NUMBER_OF_ORIENTATION)*i));
		}
		return texturesToStore;
	}

	// Rotate texture with an angle in radian
	// Get image from wwtexture, apply rotation and create a new texture from the image
	public static WWTexture rotateTexture(WWTexture texture, double angle) {
		AffineTransform transform = new AffineTransform();
		transform.rotate(angle, TextureLibrary.size/2, TextureLibrary.size/2);
		AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR );
		BufferedImage image = (BufferedImage) op.filter((BufferedImage) texture.getImageSource(), null);
		return new BufferedImageTexture(image);	
	}
	
	private static void regularAgentTextureColor(){
		TextureLibrary.agentBodyParked = createTriangleBufferedImage (Color.blue);
		TextureLibrary.agentBodyWander = createTriangleBufferedImage (Color.yellow);
		TextureLibrary.agentBodyAwareness = createTriangleBufferedImage ( Color.red);
		TextureLibrary.agentBodyTexture = createTriangleBufferedImage (new Color(115,115,115));
		TextureLibrary.agentBodyCloseToDestination = createTriangleBufferedImage(Color.orange);
		TextureLibrary.agentBodyMovingToSpot = createTriangleBufferedImage(Color.magenta);
		createAgentTexturePack();
	}
	
	private static void paperDisplayAgentTextureColor(){
		TextureLibrary.agentBodyParked = createTriangleBufferedImage (Color.black);
		TextureLibrary.agentBodyWander = createTriangleBufferedImage (Color.black);
		TextureLibrary.agentBodyAwareness = createTriangleBufferedImage (Color.black);
		TextureLibrary.agentBodyTexture = createTriangleBufferedImage (Color.black);
		TextureLibrary.agentBodyCloseToDestination = createTriangleBufferedImage(Color.black);
		TextureLibrary.agentBodyMovingToSpot = createTriangleBufferedImage(Color.black);
		createAgentTexturePack();
	}
	
	private static void paperDisplayColors(){
		paperDisplayAgentTextureColor();
		paperDisplaySpotsDisplayColor();
		paperDisplayBuildingColors();
	}
	
	private static void regularDisplayColors(){
		regularAgentTextureColor();
		regularSpotsDisplayColor();
		regularDisplayBuildingColors();
	}
	
	private static void paperDisplayBuildingColors(){
		WORK_OFFICE_COLOR = Color.DARK_GRAY;
		HOME_COLOR = Color.LIGHT_GRAY;
		MIXED_COLOR = Color.BLUE;
		SHOP_LEISURE_COLOR = Color.MAGENTA;
	}
	
	private static void regularDisplayBuildingColors(){
		WORK_OFFICE_COLOR = Color.ORANGE;
		HOME_COLOR = Color.GREEN;
		MIXED_COLOR = Color.BLUE;
		SHOP_LEISURE_COLOR = Color.MAGENTA;
	}
	
	private static void regularSpotsDisplayColor(){
		TextureLibrary.parkingSlotTexture_NotOccupied = createBufferedImage(PatternFactory.PATTERN_SQUARE, new Color(98, 232, 116));
		TextureLibrary.parkingSlotTexture_Occupied = createBufferedImage(PatternFactory.PATTERN_SQUARE, new Color(232, 98, 98));
		TextureLibrary.parkingSlotTexture_Problem = createBufferedImage(PatternFactory.PATTERN_SQUARE, new Color(255,255,255));
		TextureLibrary.parkingSlotTexture_Unavailable = createBufferedImage(PatternFactory.PATTERN_SQUARE, new Color(204,45,255));
	}
	
	private static void paperDisplaySpotsDisplayColor(){
		TextureLibrary.parkingSlotTexture_NotOccupied = createBufferedImage(PatternFactory.PATTERN_SQUARE, rgb2grayscale(new Color(98, 232, 116)));
		TextureLibrary.parkingSlotTexture_Occupied = createBufferedImage(PatternFactory.PATTERN_SQUARE, rgb2grayscale(new Color(232, 98, 98)));
		TextureLibrary.parkingSlotTexture_Problem = createBufferedImage(PatternFactory.PATTERN_SQUARE, rgb2grayscale(new Color(255,255,255)));
		TextureLibrary.parkingSlotTexture_Unavailable = createBufferedImage(PatternFactory.PATTERN_SQUARE, rgb2grayscale(new Color(204,45,255)));
	}
	
	private static void createAgentTexturePack(){
		agentBodyTexturePack = createTexturePack(agentBodyTexturePack, agentBodyTexture);
		agentBodyParkedPack = createTexturePack(agentBodyParkedPack, agentBodyParked);
		agentBodyWanderPack = createTexturePack(agentBodyWanderPack, agentBodyWander);
		agentBodyAwarenessPack = createTexturePack(agentBodyAwarenessPack, agentBodyAwareness);
		agentBodyCloseToDestinationPack = createTexturePack(agentBodyCloseToDestinationPack, agentBodyCloseToDestination);
		agentBodyMovingToSpotPack = createTexturePack(agentBodyMovingToSpotPack, agentBodyMovingToSpot);
	}
	
	/*
	 * https://stackoverflow.com/questions/687261/converting-rgb-to-grayscale-intensity
	 */
	private static Color rgb2grayscale(Color rgbColor){
		int y = (int) Math.round(rgbColor.getRed() * 0.2126 + 0.7152 * rgbColor.getGreen() + 0.0722 * rgbColor.getBlue());
		return new Color(y,y,y);
	}
}
