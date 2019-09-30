package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.physics.box2d.World;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.IRequireGameContext;
import com.spaceproject.utility.PhysicsContactListener;
import com.spaceproject.utility.Mappers;

// based off:
// http://gafferongames.com/game-physics/fix-your-timestep/
// http://saltares.com/blog/games/fixing-your-timestep-in-libgdx-and-box2d/
public class FixedPhysicsSystem extends EntitySystem implements IRequireGameContext {
    
    private static final int VELOCITY_ITERATIONS = 6;//TODO: move to engine config
    private static final int POSITION_ITERATIONS = 2;//TODO: move to engine config
    private static final int STEP_PER_FRAME = 60;//TODO: move to engine config
    private static final float TIME_STEP = 1 / (float) STEP_PER_FRAME;
    private static float accumulator = 0f;
    
    //movement limit = 2 * units per step
    //eg step of 60: 60 * 2 = 120,  max vel = 120
    
    private World world;
    
    private ImmutableArray<Entity> entities;
    
    @Override
    public void initContext(GameScreen gameScreen) {
        this.world = gameScreen.box2dWorld;
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        Family family = Family.all(PhysicsComponent.class, TransformComponent.class).get();
        entities = engine.getEntitiesFor(family);
    
        world.setContactListener(new PhysicsContactListener(engine));
    }
    
    @Override
    public void update(float deltaTime) {
        accumulator += deltaTime;
        while (accumulator >= TIME_STEP) {
            //System.out.println("update: " + deltaTime + ". " + accumulator);
            world.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
            accumulator -= TIME_STEP;
            
            updateTransform();
        }
        
        interpolate(deltaTime, accumulator);
    }
    
    private void updateTransform() {
        for (Entity entity : entities) {
            PhysicsComponent physics = Mappers.physics.get(entity);
            
            if (!physics.body.isActive()) {
                return;
            }
            
            TransformComponent transform = Mappers.transform.get(entity);
            transform.pos.set(physics.body.getPosition());
            transform.rotation = physics.body.getAngle();
        }
    }
    
    private void interpolate(float deltaTime, float accumulator) {
        /*
        if (physics.body.isActive()) {
            transform.position.x = physics.body.getPosition().x * alpha + old.position.x * (1.0f - alpha);
            transform.position.y = physics.body.getPosition().y * alpha + old.position.y * (1.0f - alpha);
            transform.angle = physics.body.getAngle() * MathUtils.radiansToDegrees * alpha + old.angle * (1.0f - alpha);
        }*/
    }
    
}


