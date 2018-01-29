package com.spaceproject.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.SpaceProject;
import com.spaceproject.generation.TextureFactory;


public class Test3DScreen extends ScreenAdapter {

    CameraInputController camController;
    PerspectiveCamera cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    //OrthographicCamera cam = new OrthographicCamera();
    SpriteBatch batch = new SpriteBatch();
    ModelBatch modelBatch = new ModelBatch();

    //Entity test = EntityFactory.createPlanet(0, new Entity(),0, 0, 0, 0,false);
    Texture t = TextureFactory.generateCharacter();
    Texture tile = TextureFactory.createTile(Color.MAGENTA);
    Texture shipTex = TextureFactory.generateShip(123, 20);
    Thing ship3d;
    public Test3DScreen() {
        cam.position.set(0, 0, 350);
        cam.lookAt(0, 0, 0);
        //cam.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 0.1f;
        cam.far = 600f;
        camController = new CameraInputController(cam);
        Gdx.input.setInputProcessor(camController);

        Sprite sprite = new Sprite();
        sprite.setTexture(shipTex);
        ship3d = new Thing(sprite, sprite);
    }

    @Override
    public void render(float delta) {
        //Gdx.gl20.glClearColor(0,0,0,0);
        //Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glClear(Gdx.gl20.GL_COLOR_BUFFER_BIT | Gdx.gl20.GL_DEPTH_BUFFER_BIT);

        camController.update();
        cam.update();
        System.out.println(cam.position + "-" + cam.direction);
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            cam.position.x += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            cam.position.x -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            cam.position.y += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            cam.position.y -= 1f;
        }



        //Texture t = test.getComponent(TextureComponent.class).texture;
        //Vector3 pos = test.getComponent(TransformComponent.class).pos;
        batch.setProjectionMatrix(cam.combined);
        //batch.setTransformMatrix(cam.combined);
        batch.begin();
        batch.draw(tile, -100,-100,50,50);
        batch.draw(t, Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);




        float width = t.getWidth();
        float height = t.getHeight();
        float originX = width * 0.5f; //center
        float originY = height * 0.5f; //center
        int x = Gdx.input.getX();
        int y = Gdx.graphics.getHeight()-Gdx.input.getY();
        float scale = SpaceProject.scale;

        //draw texture
        batch.draw(t, (x - originX), (y - originY),
                originX, originY,
                width, height,
                scale, scale,
                0,
                0, 0, (int)width, (int)height, false, false);


        width = shipTex.getWidth();
        height = shipTex.getHeight();
        originX = width * 0.5f; //center
        originY = height * 0.5f; //center
        x = -100;
        y = x;

        //draw texture
        batch.draw(shipTex, (x - originX), (y - originY),
                originX, originY,
                width, height,
                scale, scale,
                0,
                0, 0, (int)width, (int)height, false, false);

        batch.end();


        ship3d.worldTransform.translate(0, 0, 0);
        //ship3d.worldTransform.rotate(Vector3.Y, 90 * delta);
        modelBatch.begin(cam);
        modelBatch.render(ship3d);
        modelBatch.end();
        //cam.update();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void hide() {
        super.hide();
    }

    @Override
    public void pause() {
        super.pause();
    }

    @Override
    public void resume() {
        super.resume();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        batch.dispose();
    }
}


//xoppa.github.io/blog/a-simple-card-game/
class Thing extends Renderable {

    public Thing(Sprite back, Sprite front) {
        material = new Material(
                TextureAttribute.createDiffuse(front.getTexture()),
                new BlendingAttribute(false, 1f),
                FloatAttribute.createAlphaTest(0.5f)
        );

        front.setSize(1, 1);
        back.setSize(1, 1);

        front.setPosition(-front.getWidth() * 0.5f, -front.getHeight() * 0.5f);
        back.setPosition(-back.getWidth() * 0.5f, -back.getHeight() * 0.5f);

        float[] vertices = convert(front.getVertices(), back.getVertices());
        short[] indices = new short[] {0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4 };

        // FIXME: this Mesh needs to be disposed
        meshPart.mesh = new Mesh(true, 8, 12, VertexAttribute.Position(), VertexAttribute.Normal(), VertexAttribute.TexCoords(0));
        meshPart.mesh.setVertices(vertices);
        meshPart.mesh.setIndices(indices);
        meshPart.offset = 0;
        meshPart.size = meshPart.mesh.getNumIndices();
        meshPart.primitiveType = Gdx.gl20.GL_TRIANGLES;
        meshPart.update();
    }

    private static float[] convert(float[] front, float[] back) {
        return new float[] {
                front[Batch.X2], front[Batch.Y2], 0, 0, 0, 1, front[Batch.U2], front[Batch.V2],
                front[Batch.X1], front[Batch.Y1], 0, 0, 0, 1, front[Batch.U1], front[Batch.V1],
                front[Batch.X4], front[Batch.Y4], 0, 0, 0, 1, front[Batch.U4], front[Batch.V4],
                front[Batch.X3], front[Batch.Y3], 0, 0, 0, 1, front[Batch.U3], front[Batch.V3],

                back[Batch.X1], back[Batch.Y1], 0, 0, 0, -1, back[Batch.U1], back[Batch.V1],
                back[Batch.X2], back[Batch.Y2], 0, 0, 0, -1, back[Batch.U2], back[Batch.V2],
                back[Batch.X3], back[Batch.Y3], 0, 0, 0, -1, back[Batch.U3], back[Batch.V3],
                back[Batch.X4], back[Batch.Y4], 0, 0, 0, -1, back[Batch.U4], back[Batch.V4]
        };
    }

}
