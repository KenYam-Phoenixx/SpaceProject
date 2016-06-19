package com.spaceproject.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.SpaceProject;
import com.spaceproject.Tile;
import com.spaceproject.generation.FontFactory;
import com.spaceproject.ui.ColorProfile;
import com.spaceproject.ui.Slider;
import com.spaceproject.utility.MyMath;
import com.spaceproject.utility.NoiseGen;

/* TODO:
 * -zoom into mouse position
 * -text boxes for names and seed
 * -saving and loading color/feature profiles to json
 * -fix screen skew on resize
 */
public class TestNoiseScreen extends ScreenAdapter implements InputProcessor {
	
	SpriteBatch batch = new SpriteBatch();
	ShapeRenderer shape = new ShapeRenderer();
	private BitmapFont font;
	
	long seed; //make textbox
	
	int mapRenderWindowSize = 400;
	int mapSize = 120;//make slider
	int pixelSize = 3;//zoom
	
	float[][] heightMap;
	
	//feature sliders
	Slider scale, octave, persistence, lacunarity;
	
	//color picking tool
	ColorProfile colorProfile;
	
	boolean mouseDown = false;
	int offsetX = 0;
	int offsetY = 0;
	int prevX = 0;
	int prevY = 0;
	
	int mapX, mapY;
	
	public TestNoiseScreen(SpaceProject space) {
		//set this as input processor for mouse wheel scroll events
		Gdx.input.setInputProcessor(this);
		
		font = FontFactory.createFont(FontFactory.fontBitstreamVMBold, 15);
				
		seed = MathUtils.random(Long.MAX_VALUE);		
		
	
		int width = 200;
		int height = 30;
		int buttonWidth = 20;
		scale       = new Slider("scale",       1, 100, 40,  180, buttonWidth, width, height);//1 - x
		octave      = new Slider("octave",      1, 6,   40,  140, buttonWidth, width, height);//1 - x
		persistence = new Slider("persistence", 0, 1,   40,  100, buttonWidth, width, height);//0 - 1
		lacunarity  = new Slider("lacunarity",  0, 5,   40,   60, buttonWidth, width, height);//0 - x
		
		updateMap();
		
		mapX = 20; 
		mapY = Gdx.graphics.getHeight() -pixelSize - 20;
		//mapY = Gdx.graphics.getHeight() - heightMap.length*pixelSize - 20;
		
		colorProfile = new ColorProfile(500, 50, 50, 200);
		
		loadTestProfile();
		
		/*
		colorProfile.add(new Tile("r",  1f, Color.RED));
		colorProfile.add(new Tile("g",  0.666f, Color.GREEN));
		colorProfile.add(new Tile("b",  0.333f, Color.BLUE));
		*/
	}

	private void loadTestProfile() {
		colorProfile.add(new Tile("water",  0.41f,  Color.BLUE));
		colorProfile.add(new Tile("water1", 0.345f, new Color(0,0,0.42f,1)));
		colorProfile.add(new Tile("water2", 0.240f, new Color(0,0,0.23f,1)));
		colorProfile.add(new Tile("water3", 0.085f, new Color(0,0,0.1f,1)));
		colorProfile.add(new Tile("sand",   0.465f, Color.YELLOW));
		colorProfile.add(new Tile("grass",  0.625f, Color.GREEN));
		colorProfile.add(new Tile("grass1", 0.725f, new Color(0,0.63f,0,1)));
		colorProfile.add(new Tile("grass2", 0.815f, new Color(0,0.48f,0,1)));
		colorProfile.add(new Tile("lava",   1f,     Color.RED));
		colorProfile.add(new Tile("rock",   0.95f,  Color.BROWN));
		
		scale.setValue(100);
		octave.setValue(4);
		persistence.setValue(0.68f);
		lacunarity.setValue(2.6f);
	}
	
	public void render(float delta) {
		Gdx.gl20.glClearColor(0.5f, 0.5f, 0.5f, 1);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
			
		
		shape.begin(ShapeType.Filled);		
		
		//draw noise map
		drawMap();
		//drawMapLerped();
		
		drawPixelatedMap();
			
		//draw UI tool
		colorProfile.draw(shape);
		
		shape.end();
		
		
		batch.begin();
		
		//draw UI tool
		colorProfile.draw(batch, font);
		
		//draw feature sliders
		scale.draw(batch, font);
		octave.draw(batch, font);
		persistence.draw(batch, font);
		lacunarity.draw(batch, font);
		
		
		font.draw(batch, "Seed: " + seed, 15, Gdx.graphics.getHeight() - 15);
		font.draw(batch, "Zoom: " + pixelSize, 15, Gdx.graphics.getHeight() - 30);
		batch.end();
		
		//update
		colorProfile.update();
		
		
		updateClickDragMapOffset();

		
		boolean change = false;
		if (scale.update() || octave.update() 
				|| persistence.update() || lacunarity.update()){
			change = true;
		}

		//TODO: make UI sliders for these values
		if (Gdx.input.isKeyJustPressed(Keys.SPACE)){
			seed = MathUtils.random(Long.MAX_VALUE);
			change = true;
		}
		
		if (Gdx.input.isKeyPressed(Keys.EQUALS)) {
			pixelSize += 0.5;
			change = true;		
		}
		if (Gdx.input.isKeyPressed(Keys.MINUS)) {
			pixelSize -= 0.5;
			change = true;
		}
		
		if (change) {
			updateMap();
		}
			
	}

	private void updateClickDragMapOffset() {	
		// click and drag move map around
		int mouseX = Gdx.input.getX();
		int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
		if (Gdx.input.justTouched() && mouseX > mapX && mouseX < mapX + mapRenderWindowSize
				&& mouseY < mapY && mouseY > mapY - mapRenderWindowSize) {
			mouseDown = true;

			prevX = offsetX + mouseX / pixelSize;
			prevY = offsetY - mouseY / pixelSize;
		}
		
		if (Gdx.input.isTouched() && mouseDown) {
			offsetX = prevX - mouseX / pixelSize;
			offsetY = prevY + mouseY / pixelSize;
		} else {
			mouseDown = false;
		}
	}

	private void updateMap() {
		double s = scale.getValue();			
		int o = (int)octave.getValue();
		float p = persistence.getValue();
		float l = lacunarity.getValue();

		heightMap = NoiseGen.generateWrappingNoise4D(seed, mapSize, s, o, p, l);
	}
	
	private void drawMap() {					
		for (int y = 0; y * pixelSize <= mapRenderWindowSize; y++) {
			for (int x = 0; x * pixelSize <= mapRenderWindowSize; x++) {
				
				//wrap tiles
				int tX = (x + offsetX) % heightMap.length;
				int tY = (y + offsetY) % heightMap.length;
				if (tX < 0) tX += heightMap.length;
				if (tY < 0) tY += heightMap.length;
				
				//pick color
				float i = heightMap[tX][tY];
				//TODO: color doesn't need to be calculated every loop. Save tile index to array.
				for (int k = colorProfile.getTiles().size()-1; k >= 0; k--) {
					Tile tile = colorProfile.getTiles().get(k);
					if (i <= tile.getHeight() || k == 0) {
						shape.setColor(tile.getColor());
						break;
					}
				}
				//draw grid to visualize wrap
				if (tX == heightMap.length-1 || tY == heightMap.length-1) {
					shape.setColor(Color.BLACK);
				}
				
				//draw
				shape.rect(mapX + x * pixelSize, mapY - y * pixelSize, pixelSize, pixelSize);
				
				//grayscale debug
				shape.setColor(i, i, i, i);
				shape.rect(mapX + x * pixelSize + mapRenderWindowSize, mapY - y * pixelSize, pixelSize, pixelSize);
			}
		}	
	}

	private void drawMapLerped() {
		for (int y = 0; y * pixelSize <= mapRenderWindowSize; y++) {
			for (int x = 0; x * pixelSize <= mapRenderWindowSize; x++) {
				
				//wrap tiles
				int tX = (x + offsetX) % heightMap.length;
				int tY = (y + offsetY) % heightMap.length;
				if (tX < 0) tX += heightMap.length;
				if (tY < 0) tY += heightMap.length;
				
				//pick color
				float i = heightMap[tX][tY];
				for (int k = colorProfile.getTiles().size()-1; k >= 0; k--) {
					Tile tile = colorProfile.getTiles().get(k);
					
					if (i <= tile.getHeight() || k == 0) {
						if (k == colorProfile.getTiles().size()-1) {
							shape.setColor(tile.getColor());
							break;
						}
						Tile next = colorProfile.getTiles().get(k+1);
						float gradient = MyMath.inverseLerp(next.getHeight(), tile.getHeight(), i);
						shape.setColor(next.getColor().cpy().lerp(tile.getColor(), gradient));
						
						break;
					}
				}
				
				//draw grid to visualize wrap
				if (tX == heightMap.length-1 || tY == heightMap.length-1) {
					shape.setColor(Color.BLACK);
				}
				
				//draw
				shape.rect(mapX + x * pixelSize, mapY - y * pixelSize, pixelSize, pixelSize);
				
				//grayscale debug
				shape.setColor(i, i, i, i);
				shape.rect(mapX + x * pixelSize + mapRenderWindowSize, mapY - y * pixelSize, pixelSize, pixelSize);
			}
		}
	}
	
	private void drawPixelatedMap() {
		if (colorProfile.getTiles().isEmpty()) {
			return;
		}
		
		int mapX = Gdx.graphics.getWidth() - heightMap.length - 20;
		int mapY = Gdx.graphics.getHeight() - 20;
		
		int chunkSize = 6;
		//chunkSize must evenly divide into mapsize
		while (heightMap.length % chunkSize != 0) {
			chunkSize--;
		}
		chunkSize = Math.abs(chunkSize);
		int chunks = heightMap.length/chunkSize;

		//for each chunk
		for (int cY = 0; cY < chunks; cY++) {
			for (int cX = 0; cX < chunks; cX++) {
			
				int chunkX = cX * chunkSize;
				int chunkY = cY * chunkSize;
				
				int[] count = new int[colorProfile.getTiles().size()];
				
				//for each tile in chunk, count occurrence of tiles within a chunk
				for (int y = chunkY; y < chunkY+chunkSize; y++) {
					for (int x = chunkX; x < chunkX+chunkSize; x++) {
						
						//wrap tiles
						int tX = (x + offsetX) % heightMap.length;
						int tY = (y + offsetY) % heightMap.length;
						if (tX < 0) tX += heightMap.length;
						if (tY < 0) tY += heightMap.length;
						
						//count colors
						float i = heightMap[tX][tY];
						for (int k = colorProfile.getTiles().size() - 1; k >= 0; k--) {
							Tile tile = colorProfile.getTiles().get(k);
							if (i <= tile.getHeight() || k == 0) {
								count[k]++;
								break;
							}
						}
					}
				}
				
				//set color to highest tile count
				int max = count[0];
				int index = 0;
				for (int i = 0; i < count.length; i++) {
					if (count[i] > max) {
						max = count[i];
						index = i;
					}
				}
				shape.setColor(colorProfile.getTiles().get(index).getColor());
				
			}
		}
		
		
	}
	
	private static Texture createNoiseMapTex(float[][] map) {
		//create image
		Pixmap pixmap = new Pixmap(map.length, map.length, Format.RGB888);
		for (int x = 0; x < map.length; ++x) {
			for (int y = 0; y < map.length; ++y) {
				
				float i = map[x][y];
				pixmap.setColor(new Color(i, i , i, 1));
				
				pixmap.drawPixel(x, y);
			}
		}	

		Texture t = new Texture(pixmap);
		pixmap.dispose();
		return t;
	}

	public void resize(int width, int height) {
		Gdx.app.log("screen", width + ", " + height);
	}

	public void dispose() { }

	public void hide() { }

	public void pause() { }

	public void resume() { }
	
	@Override
	public boolean keyDown(int keycode) { return false; }

	@Override
	public boolean keyUp(int keycode) { return false; }

	@Override
	public boolean keyTyped(char character) { return false; }

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }

	@Override
	public boolean mouseMoved(int screenX, int screenY) { return false; }

	@Override
	public boolean scrolled(int amount) {
		pixelSize = MathUtils.clamp(pixelSize - amount, 1, 32);
		
		/*
		int mouseX = Gdx.input.getX();
		int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
		//prevX = offsetX + mouseX / pixelSize;
		//prevY = offsetY - mouseY / pixelSize;
		offsetX = mouseX / pixelSize;
		offsetY = mouseY / pixelSize;*/
		
		//mapY = Gdx.graphics.getHeight() - heightMap.length*pixelSize - 20;
		mapY = Gdx.graphics.getHeight() -pixelSize - 20;
		return false;
	}
	
}
