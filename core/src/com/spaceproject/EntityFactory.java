package com.spaceproject;


import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.ExpireComponent;
import com.spaceproject.components.MovementComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.ProjectileComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;

public class EntityFactory {

	public static Entity[] createPlanetarySystem(float x, float y) {
		MathUtils.random.setSeed((long)(x + y) * SpaceProject.SEED);
		
		Entity[] planetarySystemEntities = new Entity[MathUtils.random(1,10) + 1];
		boolean rotationDirection = MathUtils.randomBoolean(); //rotation of system (orbits and spins)
		
		//add star to center of planetary system
		Entity star = createStar(x, y, rotationDirection);
		planetarySystemEntities[0] = star;
		
		float distance = 0;
		
		//create planets around star
		for (int i = 1; i < planetarySystemEntities.length; ++i) {
			distance += MathUtils.random(1600, 2100);
			planetarySystemEntities[i] = createPlanet(star, distance, rotationDirection);
		}
				
		return planetarySystemEntities;
		
	}
	
	public static Entity createStar(float x, float y, boolean rotationDir) {
		MathUtils.random.setSeed((long)(x + y) * SpaceProject.SEED);
		Entity entity = new Entity();

		// create star texture
		TextureComponent texture = new TextureComponent();
		float scale = 4.0f;
		int minSize = 20;
		int maxSize = 250;
		int radius = MathUtils.random(minSize, maxSize);	
		texture.texture = TextureFactory.generateStar(radius);
		texture.scale = scale;
		
		// set position
		TransformComponent transform = new TransformComponent();
		transform.pos.set(x, y, 0); 

		//add bounding box
		BoundsComponent bounds = new BoundsComponent();
		bounds.bounds.height = radius*2*scale;
		bounds.bounds.width = radius*2*scale;
		
		//orbit for rotation of self (kinda hacky; not really orbiting, just rotating)
		OrbitComponent orbit = new OrbitComponent();
		orbit.parent = entity;//set to self to negate orbit
		orbit.rotateClockwise = rotationDir;
		orbit.rotSpeed = MathUtils.random(0.002f, 0.01f); //rotation speed of star
		
		//add components to entity
		entity.add(orbit);
		entity.add(bounds);
		entity.add(transform);
		entity.add(texture);

		return entity;
	}
	
	public static Entity createPlanet(Entity parent, float distance, boolean rotationDir) {
		Vector3 parentPos = parent.getComponent(TransformComponent.class).pos;
		MathUtils.random.setSeed((long)(parentPos.x + parentPos.y * distance) * SpaceProject.SEED);
		Entity entity = new Entity();	
		
		//create texture
		TextureComponent texture = new TextureComponent();
		float scale = 4.0f;
		int minRad = 12;
		int maxRad = 200;	
		int radius = MathUtils.random(minRad, maxRad);	
		texture.texture = TextureFactory.generatePlanet(radius);
		texture.scale = scale;

		//add bounding box
		BoundsComponent bounds = new BoundsComponent();
		bounds.bounds.height = radius*2*scale;
		bounds.bounds.width = radius*2*scale;
		
		//orbit 
		OrbitComponent orbit = new OrbitComponent();
		orbit.parent = parent;
		orbit.rotSpeed = MathUtils.random(0.002f, 0.05f); //rotation speed of planet
		orbit.orbitSpeed = MathUtils.random(0.0007f, 0.005f); //orbit speed of planet	
		orbit.angle = MathUtils.random(3.14f * 2); //angle from star
		orbit.distance = distance;
		orbit.rotateClockwise = rotationDir;
		
		//add components to entity
		entity.add(bounds);
		entity.add(new TransformComponent());
		entity.add(texture);
		entity.add(orbit);

		return entity;
	}
	
	public static Entity createProjectile(TransformComponent t, float dx, float dy, int size) {
		Entity entity = new Entity();
				
		//create texture
		TextureComponent texture = new TextureComponent();
		float scale = 4.0f;	
		texture.texture = TextureFactory.generateProjectile(size);
		texture.scale = scale;
		
		//bounding box
		BoundsComponent bounds = new BoundsComponent();
		bounds.bounds.width = size * scale;
		bounds.bounds.height = size/2 == 0 ? 1 * scale : (size/2) * scale;
		
		//set position and orientation
		TransformComponent transform = new TransformComponent();
		transform.pos.set(t.pos);
		transform.rotation = t.rotation;
		
		//set position
		MovementComponent movement = new MovementComponent();
		movement.velocity.add(dx, dy);
		
		//set expire time
		ExpireComponent expire = new ExpireComponent();
		expire.time = 5;//in seconds ~approx
		
		entity.add(expire);
		entity.add(texture);
		entity.add(bounds);
		entity.add(transform);
		entity.add(movement);
		
		return entity;
	}
	
	public static Entity createCharacter(int x, int y) {
		Entity entity = new Entity();
		
		TransformComponent transform = new TransformComponent();
		transform.pos.set(x, y, 0);
		
		TextureComponent texture = new TextureComponent();
		float scale = 4.0f;		
		texture.texture = TextureFactory.generateCharacter();
		texture.scale = scale;
		
		BoundsComponent bounds = new BoundsComponent();
		bounds.bounds.height = texture.texture.getWidth() * scale;
		bounds.bounds.width = texture.texture.getWidth() * scale;
			
		entity.add(bounds);
		entity.add(transform);
		entity.add(texture);
		entity.add(new MovementComponent());
			
		return entity;
	}
	
	@Deprecated
	public static Entity createShip2(int x, int y) {
		MathUtils.random.setSeed((long)(x + y) * SpaceProject.SEED);
		Entity entity = new Entity();

		TransformComponent transform = new TransformComponent();
		TextureComponent texture = new TextureComponent();

		transform.pos.set(x, y, -10);
		transform.rotation = (float) Math.PI/2; //face upwards
		
		//generate random even size 
		int size;
		int minSize = 8;
		int maxSize = 36;
		do {		
			size = MathUtils.random(minSize, maxSize);
		} while (size % 2 == 1);
		
		// generate pixmap texture
		//int size = 24;
		Pixmap pixmap = new Pixmap(size, size/2, Format.RGBA8888);
		pixmap.setColor(1, 1, 1, 1);
		pixmap.fillRectangle(0, 0, size, size);
		
		pixmap.setColor(0.7f,0.7f,0.7f,1);
		pixmap.drawRectangle(0, 0, size-1, size-1/2);
		
		Texture pixmapTex = new Texture(pixmap);
		float scale = 4.0f;
		pixmap.dispose(); // clean up
		texture.texture = pixmapTex;// give texture component the generated pixmapTexture
		texture.scale = scale;
		
		BoundsComponent bounds = new BoundsComponent();
		bounds.bounds.height = size * scale;
		bounds.bounds.width = size * scale;

		
		entity.add(bounds);
		entity.add(texture);
		entity.add(transform);
		entity.add(new MovementComponent());
		entity.add(new VehicleComponent());
		
		return entity;
	}
	
	public static Entity createShip3(int x, int y) {
		
		Entity entity = new Entity();

		MathUtils.random.setSeed((long)(x + y) * SpaceProject.SEED);
		
		TransformComponent transform = new TransformComponent();
		TextureComponent texture = new TextureComponent();

		transform.pos.set(x, y, -10);
		transform.rotation = (float) Math.PI/2; //face upwards
		
		//generate random even size 
		int size;
		int minSize = 10;
		int maxSize = 36;
		
		do {
			//generate even size
			size = MathUtils.random(minSize, maxSize);
		} while (size % 2 == 1);
		
		Texture pixmapTex = TextureFactory.generateShip(size);
		float scale = 4.0f;
		texture.texture = pixmapTex;// give texture component the generated pixmapTexture
		texture.scale = scale;
		
		//collision detection
		BoundsComponent bounds = new BoundsComponent();
		bounds.bounds.height = size * scale;
		bounds.bounds.width = size * scale;

		//weapon
		ProjectileComponent proj = new ProjectileComponent();
		proj.maxAmmo = 5;
		proj.curAmmo = proj.maxAmmo;
		proj.fireRate = 20; //lower is faster
		proj.size = 1;
		proj.velocity = 650;
		proj.rechargeRate = 60; //lower is faster
		
		//engine data and marks entity as drive-able
		VehicleComponent vehicle = new VehicleComponent();
		vehicle.thrust = 320;
		
		entity.add(proj);
		entity.add(bounds);
		entity.add(texture);
		entity.add(transform);
		entity.add(vehicle);
		entity.add(new MovementComponent());
		
		return entity;
	}
	
	@Deprecated
	public static Entity createShip(int x, int y) {
		Entity entity = new Entity();

		TransformComponent transform = new TransformComponent();
		TextureComponent texture = new TextureComponent();

		transform.pos.set(x, y, -10);
		transform.rotation = (float) Math.PI/2; //face upwards
		
		// generate pixmap texture
		int size = 16;
		Pixmap pixmap = new Pixmap(size, size, Format.RGB565);
		pixmap.setColor(1, 1, 1, 1);
		pixmap.fillTriangle(0, 0, 0, size-1, size-1, size/2);
		
		pixmap.setColor(0, 1, 1, 1);
		pixmap.drawLine(size, size/2, size/2, size/2);
		
		
		Texture pixmapTex = new Texture(pixmap);
		float scale = 4.0f;
		pixmap.dispose(); // clean up
		texture.texture = pixmapTex;// give texture component the generated pixmapTexture
		texture.scale = scale;
		
		BoundsComponent bounds = new BoundsComponent();
		bounds.bounds.height = size * scale;
		bounds.bounds.width = size * scale;

		
		entity.add(bounds);
		entity.add(texture);
		entity.add(transform);
		entity.add(new MovementComponent());
		entity.add(new VehicleComponent());
		
		return entity;
	}
}
