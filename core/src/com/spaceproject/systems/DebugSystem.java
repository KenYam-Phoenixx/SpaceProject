package com.spaceproject.systems;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.CircumstellarDiscComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.DebugConfig;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.generation.FontFactory;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.debug.DebugText;
import com.spaceproject.ui.debug.DebugVec;
import com.spaceproject.ui.debug.ECSExplorerWindow;
import com.spaceproject.utility.DebugUtil;
import com.spaceproject.utility.ECSUtil;
import com.spaceproject.utility.Mappers;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Set;

public class DebugSystem extends IteratingSystem implements Disposable {
    
    private static final DebugConfig debugCFG = SpaceProject.configManager.getConfig(DebugConfig.class);
    private static final KeyConfig keyCFG = SpaceProject.configManager.getConfig(KeyConfig.class);
    
    private ECSExplorerWindow engineView;
    
    //rendering
    private static OrthographicCamera cam;
    private final SpriteBatch batch;
    private final ShapeRenderer shape;
    private final Matrix4 projectionMatrix;
    private final BitmapFont fontSmall, fontLarge;
    private final Box2DDebugRenderer debugRenderer;
    
    //textures
    private final Texture texCompBack = TextureFactory.createTile(Color.GRAY);
    private final Texture texCompSeparator = TextureFactory.createTile(Color.RED);
    
    //entity storage
    private final Array<Entity> objects;
    
    private static final ArrayList<DebugText> debugTexts = new ArrayList<DebugText>();
    private static final ArrayList<DebugVec> debugVecs = new ArrayList<DebugVec>();
    
    
    public DebugSystem() {
        super(Family.all(TransformComponent.class).get());
        
        cam = GameScreen.cam;
        batch = GameScreen.batch;
        shape = GameScreen.shape;
        projectionMatrix = new Matrix4();
        fontSmall = FontFactory.createFont(FontFactory.fontBitstreamVM, 10);
        fontLarge = FontFactory.createFont(FontFactory.fontBitstreamVMBold, 20);
        objects = new Array<>();
    
        debugRenderer = new Box2DDebugRenderer(
                debugCFG.drawBodies,
                debugCFG.drawJoints,
                debugCFG.drawAABBs,
                debugCFG.drawInactiveBodies,
                debugCFG.drawVelocities,
                debugCFG.drawContacts);
        
        GameScreen.getStage().addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                super.keyDown(event, keycode);
                engineView.keyDown(event, keycode);
                return false;
            }
        
            @Override
            public boolean keyUp(InputEvent event, int keycode) {
                super.keyUp(event, keycode);
                return false;
            }
        
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                super.touchDown(event, x, y, pointer, button);
                return false;
            }
        });
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);

        engineView = new ECSExplorerWindow(engine);
    }
    
    @Override
    public void update(float delta) {
        engineView.update();
        
        updateKeyToggles();
        
        //don't update if we aren't drawing
        if (!debugCFG.drawDebugUI) return;
        super.update(delta);
        
        //set projection matrix so things render using correct coordinates
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.setProjectionMatrix(projectionMatrix);
        shape.setProjectionMatrix(cam.combined);
        
        
        shape.begin(ShapeType.Line);
        {
            // draw ring to visualize orbit path
            if (debugCFG.drawOrbitPath)
                drawOrbitPath(true);
            
            if (debugCFG.drawMousePos)
                drawMouseLine();
            
            drawDebugVectors();
        }
        shape.end();
        
        
        //draw diagnostic info
        int diagnosticX = 15;
        int diagnosticY = Gdx.graphics.getHeight() - 15;
        
        batch.begin();
        {
            if (debugCFG.drawFPS)
                drawFPS(diagnosticX, diagnosticY);
    
            if (debugCFG.drawDiagnosticInfo)
                drawDiagnosticInfo(diagnosticX, diagnosticY);
    
            if (debugCFG.drawPos)
                drawEntityPositions();
    
            if (debugCFG.drawMousePos)
                drawMousePos();
    
            if (debugCFG.drawEntityList)
                drawEntityList();
            
            if (debugCFG.drawComponentList)
                drawComponentList();
            
            //render debug called from other systems
            drawDebugTexts(batch);
        }
        batch.end();
    
        if (debugCFG.box2DDebugRender)
            debugRenderer.render(GameScreen.box2dWorld, GameScreen.cam.combined);
        
        objects.clear();
    }
    
    @Override
    public void processEntity(Entity entity, float deltaTime) {
        objects.add(entity);
    }
    
    private void updateKeyToggles() {
        if (Gdx.input.isKeyPressed(Input.Keys.NUM_8)) {
            GameScreen.adjustGameTime(2000);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.NUM_9)) {
            GameScreen.adjustGameTime(-2000);
        }
        
    
        if (Gdx.input.isKeyJustPressed(keyCFG.toggleEngineViewer)) {
            engineView.toggle();
        }
    
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            GameScreen.getStage().setDebugAll(!GameScreen.getStage().isDebugAll());
        }
        
        //toggle debug
        if (Gdx.input.isKeyJustPressed(keyCFG.toggleDebug)) {
            debugCFG.drawDebugUI = !debugCFG.drawDebugUI;
            Gdx.app.log(this.getClass().getSimpleName(), "DEBUG UI: " + debugCFG.drawDebugUI);
        }
        
        //toggle pos
        if (Gdx.input.isKeyJustPressed(keyCFG.togglePos)) {
            debugCFG.drawPos = !debugCFG.drawPos;
            if (debugCFG.drawComponentList) {
                debugCFG.drawComponentList = false;
            }
            Gdx.app.log(this.getClass().getSimpleName(), "[debug] draw pos: " + debugCFG.drawPos);
        }
        
        //toggle components
        if (Gdx.input.isKeyJustPressed(keyCFG.toggleComponents)) {
            debugCFG.drawComponentList = !debugCFG.drawComponentList;
            if (debugCFG.drawPos) {
                debugCFG.drawPos = false;
            }
            Gdx.app.log(this.getClass().getSimpleName(), "[debug] draw component list: " + debugCFG.drawComponentList);
        }
        
        //toggle bounds
        if (Gdx.input.isKeyJustPressed(keyCFG.toggleBounds)) {
            debugCFG.box2DDebugRender = !debugCFG.box2DDebugRender;
            Gdx.app.log(this.getClass().getSimpleName(), "[debug] draw bounds: " + debugCFG.box2DDebugRender);
        }
        
        //toggle fps
        if (Gdx.input.isKeyJustPressed(keyCFG.toggleFPS)) {
            debugCFG.drawFPS = !debugCFG.drawFPS;
            Gdx.app.log(this.getClass().getSimpleName(), "[debug] draw FPS: " + debugCFG.drawFPS);
        }
        
        //toggle orbit circle
        if (Gdx.input.isKeyJustPressed(keyCFG.toggleOrbit)) {
            debugCFG.drawOrbitPath = !debugCFG.drawOrbitPath;
            Gdx.app.log(this.getClass().getSimpleName(), "[debug] draw orbit path: " + debugCFG.drawOrbitPath);
        }
        
        //toggle vector
        if (Gdx.input.isKeyJustPressed(keyCFG.toggleVector)) {
            debugCFG.drawVelocities = !debugCFG.drawVelocities;
            Gdx.app.log(this.getClass().getSimpleName(), "[debug] draw vectors: " + debugCFG.drawVelocities);
        }
        
        
        debugRenderer.setDrawVelocities(debugCFG.drawVelocities);
        debugRenderer.setDrawBodies(debugCFG.drawBodies);
        debugRenderer.setDrawJoints(debugCFG.drawJoints);
        debugRenderer.setDrawAABBs(debugCFG.drawAABBs);
        debugRenderer.setDrawInactiveBodies(debugCFG.drawInactiveBodies);
        debugRenderer.setDrawVelocities(debugCFG.drawVelocities);
        debugRenderer.setDrawContacts(debugCFG.drawContacts);
    }
    
    private void drawFPS(int x, int y) {
        int fps = Gdx.graphics.getFramesPerSecond();
        Color fpsColor = fps >= 120 ? Color.SKY : fps > 45 ? Color.WHITE : fps > 30 ? Color.YELLOW : Color.RED;
        fontLarge.setColor(fpsColor);
        fontLarge.draw(batch, Integer.toString(fps), x, y);
    }
    
    private void drawDiagnosticInfo(int x, int y) {
        //camera position
        String camera = String.format("Pos: %s %s  Zoom:%3$.2f", (int) cam.position.x, (int) cam.position.y, cam.zoom);
        
        //memory
        String memory = DebugUtil.getMemory();
        
        //entity/component count
        String count = ECSUtil.getECSString(getEngine());
        
        //threads
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        String threads = "  Threads: " + threadSet.size();
        
        int sbCalls = GameScreen.batch.renderCalls;
        
        int linePos = 1;
        float lineHeight = fontLarge.getLineHeight();
        fontLarge.setColor(Color.WHITE);
        fontLarge.draw(batch, count, x, y - (lineHeight * linePos++));
        fontLarge.draw(batch, memory + threads, x, y - (lineHeight * linePos++));
        fontLarge.draw(batch, camera, x, y - (lineHeight * linePos++));
        fontLarge.draw(batch, "time: " + MyMath.formatDuration(GameScreen.getGameTimeCurrent())
                        + " (" + GameScreen.getGameTimeCurrent() + ")", Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() - 10);
        fontLarge.draw(batch, "seed: " + GameScreen.getGalaxySeed(), Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() - 10 - lineHeight);
        
        if (!GameScreen.inSpace()) {
            fontLarge.draw(batch, "planet: " + GameScreen.getPlanetSeed(),
                    Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() - 10 - lineHeight * 2);
        }
        
        //view threads
        String noisePool = GameScreen.noiseManager.getNoiseThreadPool().toString();
        fontSmall.draw(batch, noisePool, x, y - (lineHeight * linePos++));
        for (Thread t : threadSet) {
            fontSmall.draw(batch, t.toString(), x, y - (lineHeight * linePos++));
        }
    }

    private void drawEntityList() {
        float fontHeight = fontSmall.getLineHeight();
        int x = 30;
        int y = 30;
        int i = 0;
        for (Entity entity : getEngine().getEntities()) {
            fontSmall.draw(batch, Integer.toHexString(entity.hashCode()), x, y + (fontHeight * i++));
        }
    }
    
    /** Draw all Entity components and fields */
    private void drawComponentList() {
        // todo: this is a very heavy method, consider frustrum culling: if object is offscreen do not render!
        
        float fontHeight = fontSmall.getLineHeight();
        int backWidth = 400;//width of background
        
        fontSmall.setColor(1, 1, 1, 1);
        
        for (Entity entity : objects) {
            //get entities position and list of components
            TransformComponent t = Mappers.transform.get(entity);
            ImmutableArray<Component> components = entity.getComponents();
            
            //use Vector3.cpy() to project only the position and avoid modifying projection matrix for all coordinates
            Vector3 screenPos = cam.project(new Vector3(t.pos.cpy(), 0));
            
            //calculate spacing and offset for rendering
            int fields = 0;
            for (Component c : components) {
                fields += c.getClass().getFields().length;
            }
            float yOffset = fontHeight * fields / 2;
            int curLine = 0;
            
            //draw all components/fields
            for (Component c : components) {
                //save component line to draw name
                float compLine = curLine;
                
                //draw all fields
                for (Field f : c.getClass().getFields()) {
                    float yOffField = screenPos.y - (fontHeight * curLine) + yOffset;
                    batch.draw(texCompBack, screenPos.x, yOffField, backWidth, -fontHeight);
                    try {
                        fontSmall.draw(batch, String.format("%-14s %s", f.getName(), f.get(c)), screenPos.x + 130, yOffField);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    curLine++;
                }
                
                //draw backing on empty components
                if (c.getClass().getFields().length == 0) {
                    batch.draw(texCompBack, screenPos.x, screenPos.y - (fontHeight * curLine) + yOffset, backWidth, -fontHeight);
                    curLine++;
                }
                
                //draw separating line
                batch.draw(texCompSeparator, screenPos.x, screenPos.y - (fontHeight * curLine) + yOffset, backWidth, 1);
                
                //draw component name
                float yOffComp = screenPos.y - (fontHeight * compLine) + yOffset;
                fontSmall.draw(batch, "[" + c.getClass().getSimpleName() + "]", screenPos.x, yOffComp);
            }
        }
        
    }
    
    private void drawEntityPositions() {
        fontSmall.setColor(1, 1, 1, 1);
        for (Entity entity : objects) {
            TransformComponent t = Mappers.transform.get(entity);
            
            //String vel = " ~ " + MyMath.round(t.velocity.len(), 1);
            String info = MyMath.round(t.pos.x, 1) + "," + MyMath.round(t.pos.y, 1);
            
            Vector3 screenPos = cam.project(new Vector3(t.pos.cpy(), 0));
            fontSmall.draw(batch, Integer.toHexString(entity.hashCode()), screenPos.x, screenPos.y);
            fontSmall.draw(batch, info, screenPos.x, screenPos.y-10);
        }
    }
    
    private void drawMousePos() {
        int x = Gdx.input.getX();
        int y = Gdx.graphics.getHeight() - Gdx.input.getY();
        
        String localPos = x + "," + y;
        fontSmall.draw(batch, localPos, x, y);
    
        Vector3 worldPos = cam.unproject(new Vector3(x, y, 0));
        long seed = MyMath.getSeed(worldPos.x, worldPos.y);
        fontSmall.draw(batch, (int) worldPos.x + "," + (int) worldPos.y + " (" + seed + ")", x, y + fontSmall.getLineHeight());
    
        //float angle = MyMath.angleTo(Gdx.input.getX(), Gdx.input.getY(), Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);
        //fontSmall.draw(batch, MyMath.round(angle,3) + " / " + MyMath.round(angle * MathUtils.radDeg, 3), x, y + (int) fontSmall.getLineHeight()*2);
    }
    
    private void drawMouseLine() {
        int crossHairSize = 32;
        Color mouseLineColor = Color.BLACK;
        
        int x = Gdx.input.getX();
        int y = Gdx.input.getY();
        Vector3 worldPos = cam.unproject(new Vector3(x, y, 0));
        shape.setColor(mouseLineColor);
        shape.line(worldPos.x, worldPos.y + crossHairSize, worldPos.x, worldPos.y - crossHairSize);
        shape.line(worldPos.x + crossHairSize, worldPos.y, worldPos.x - crossHairSize, worldPos.y);
    }
    
    /** Draw lines to represent speed and direction of entity */
    private void drawVelocityVectors() {
        for (Entity entity : objects) {
            //get entities position and list of components
            PhysicsComponent t = Mappers.physics.get(entity);
            if (t != null && t.body != null) {
                float scale = 2.0f; //how long to make vectors (higher number is longer line)
                Vector2 end = MyMath.logVec(t.body.getLinearVelocity(), scale).add(t.body.getPosition());
                
                //draw line to represent movement
                debugVecs.add(new DebugVec(t.body.getPosition(), end, Color.RED, Color.MAGENTA));
            }
        }
    }
    
    /** Draw orbit path, a ring to visualize objects orbit  */
    private void drawOrbitPath(boolean showSyncedPos) {
        Color orbitObjectColor = new Color(1, 1, 1, 1);
        Color orbitSyncPosColor = new Color(1, 0, 0, 1);
        
        for (Entity entity : objects) {
            
            OrbitComponent orbit = Mappers.orbit.get(entity);
            if (orbit != null) {
                TransformComponent entityPos = Mappers.transform.get(entity);
                
                if (orbit.parent != null) {
                    TransformComponent parentPos = Mappers.transform.get(orbit.parent);
                    
                    if (showSyncedPos) {
                        //synced orbit position (where the object should be)
                        Vector2 orbitPos = OrbitSystem.getTimeSyncedPos(orbit, GameScreen.getGameTimeCurrent());
                        shape.setColor(orbitSyncPosColor);
                        shape.line(parentPos.pos.x, parentPos.pos.y, orbitPos.x, orbitPos.y);
                    }
                    
                    //actual position
                    shape.setColor(orbitObjectColor);
                    shape.circle(parentPos.pos.x, parentPos.pos.y, orbit.radialDistance);
                    shape.line(parentPos.pos.x, parentPos.pos.y, entityPos.pos.x, entityPos.pos.y);
                }
                
                TextureComponent tex = Mappers.texture.get(entity);
                if (tex != null) {
                    float radius = tex.texture.getWidth() * 0.5f * tex.scale;
                    Vector2 orientation = MyMath.vector(entityPos.rotation, radius).add(entityPos.pos);
                    shape.setColor(orbitObjectColor);
                    shape.line(entityPos.pos.x, entityPos.pos.y, orientation.x, orientation.y);
                    shape.circle(entityPos.pos.x, entityPos.pos.y, radius);
                }
            }
    
            CircumstellarDiscComponent stellarDisk = Mappers.circumstellar.get(entity);
            if (stellarDisk != null) {
                Vector2 pos = Mappers.transform.get(entity).pos;
                shape.setColor(Color.LIME);
                shape.circle(pos.x, pos.y, stellarDisk.radius);
                shape.setColor(Color.PURPLE);
                shape.circle(pos.x, pos.y, stellarDisk.radius - (stellarDisk.width / 2));
                shape.circle(pos.x, pos.y, stellarDisk.radius + (stellarDisk.width / 2));
            }
            
        }
    }
    
    //region external debug util
    public static void addDebugVec(Vector2 pos, Vector2 vec, Color color) {
        float scale = 20; //how long to make vectors (higher number is longer line)
        Vector2 end = MyMath.logVec(vec, scale).add(pos);
        debugVecs.add(new DebugVec(pos, end, color));
    }
    
    private void drawDebugVectors() {
        for (DebugVec debugVec : debugVecs) {
            shape.line(debugVec.vecA.x, debugVec.vecA.y, debugVec.vecB.x, debugVec.vecB.y, debugVec.colorA, debugVec.colorB);
        }
        debugVecs.clear();
    }
    
    public static void addDebugText(String text, float x, float y) {
        addDebugText(text, x, y, false);
    }
    
    public static void addDebugText(String text, float x, float y, boolean project) {
        if (project) {
            Vector3 screenPos = cam.project(new Vector3(x, y, 0));
            x = screenPos.x;
            y = screenPos.y;
        }
        debugTexts.add(new DebugText(text, x, y));
    }
    
    private void drawDebugTexts(SpriteBatch batch) {
        for (DebugText t : debugTexts) {
            if (t.font == null) {
                fontSmall.setColor(t.color);
                fontSmall.draw(batch, t.text, t.x, t.y);
            } else {
                t.font.setColor(t.color);
                t.font.draw(batch, t.text, t.x, t.y);
            }
        }
        debugTexts.clear();
    }
    //endregion
    
    @Override
    public void dispose() {
        texCompBack.dispose();
        texCompSeparator.dispose();
        fontSmall.dispose();
        fontLarge.dispose();
        
        getEngine().removeEntityListener(engineView);
    }
    
}
