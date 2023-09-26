package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.components.ItemComponent;
import com.spaceproject.components.SoundComponent;

public class SoundSystem extends EntitySystem implements Disposable {
    
    //  todo: should be event based?, so any system can call
    //  currently any systems wanting to make a sound can get the system from the engine, but inter-system coupling.
    //  see: https://github.com/libgdx/ashley/blob/master/ashley/tests/com/badlogic/ashley/signals/SignalTests.java
    
    //  The current upper limit for decoded audio is 1 MB.
    //  https://libgdx.com/wiki/audio/sound-effects
    //
    //  Supported Formats: MP3, OGG and WAV
    //  - WAV files are quite large compared to other formats
    //      - WAV files must have 16 bits per sample
    //  - OGG files don’t work on RoboVM (iOS) nor with Safari (GWT)
    //  - MP3 files have issues with seamless looping.
    //
    //  Continuous / looping sound: soundID = sound.loop();
    //  - raw sound file must have no gaps
    //  - start and end of wave should line up to prevent clipping
    //  - must not use mp3
    //
    //  Panning:
    //  - sounds must be MONO
    //
    //  Latency is not great and the default implementation is not recommended for latency sensitive apps like rhythm games.
    //  https://libgdx.com/wiki/audio/audio#audio-on-android
    //  Given the physics based nature of this game, id say rhythm is certainly important given we want collisions to feel accurate
    
    //  Given the above sound files will be expected as:
    //  16-bit WAV -> MONO
    //  Roughly -3 to -6 db track rendering?
    
    private float engineVolume = 0.5f;
    private boolean soundEnabled = true;
    
    static ArrayMap<Long, Sound> activeLoops;
    AssetManager assetManager;
    
    Sound shipEngineActiveLoop, shipEngineAmbientLoop;
    Sound shipExplode;
    Sound f3;
    Sound break0, break1, break2;
    Sound bounce0, bounce1, bounce2;
    Sound laserShoot, laserShootCharge;
    Sound hullImpact, hullImpactHeavy;
    Sound shieldImpact;
    Sound shieldCharge, shieldOn, shieldOff, shieldAmbientLoop;
    Sound hyperdriveEngage;
    Sound pickup;
    Sound dockStation, undockStation;
    
    @Override
    public void addedToEngine(Engine engine) {
        activeLoops = new ArrayMap<>();
        
        /*
        //todo: should use assetmanager?
        assetManager = new AssetManager();
        assetManager.load("sound/brownNoise.wav", Sound.class);
        assetManager.finishLoading();
        shipEngineActiveLoop = assetManager.get("sound/55hz.wav", Sound.class);//fails: wrong path?
         */
        
        //load sounds
        shipEngineActiveLoop = Gdx.audio.newSound(Gdx.files.internal("sound/brownNoise.wav"));
        shipEngineAmbientLoop = Gdx.audio.newSound(Gdx.files.internal("sound/55hz.wav"));
        shipExplode = Gdx.audio.newSound(Gdx.files.internal("sound/explode.wav"));
        
        f3 = Gdx.audio.newSound(Gdx.files.internal("sound/f3.wav"));

        //break0 = Gdx.audio.newSound(Gdx.files.internal("sound/" + ItemComponent.Resource.RED.getSound()));
        //Gdx.audio.newSound(Gdx.files.internal("sound/" + ItemComponent.Resource.GREEN.getSound()));
        //Gdx.audio.newSound(Gdx.files.internal("sound/" + ItemComponent.Resource.BLUE.getSound()));
        //Gdx.audio.newSound(Gdx.files.internal("sound/" + ItemComponent.Resource.SILVER.getSound()));
        //Gdx.audio.newSound(Gdx.files.internal("sound/" + ItemComponent.Resource.GOLD.getSound()));

        laserShoot = Gdx.audio.newSound(Gdx.files.internal("sound/laserShoot.wav"));// laserShootW2
        laserShootCharge = Gdx.audio.newSound(Gdx.files.internal("sound/laserChargeW.mp3"));
        
        hullImpact = Gdx.audio.newSound(Gdx.files.internal("sound/hullImpactLight.mp3"));
        hullImpactHeavy = Gdx.audio.newSound(Gdx.files.internal("sound/hullImpactHeavy.mp3"));
        
        shieldImpact = Gdx.audio.newSound(Gdx.files.internal("sound/shieldImpact.mp3"));
        //shieldCharge = Gdx.audio.newSound(Gdx.files.internal("sound/shieldChargeUp.mp3"));
        shieldOn = Gdx.audio.newSound(Gdx.files.internal("sound/shieldOn.mp3"));
        shieldOff = Gdx.audio.newSound(Gdx.files.internal("sound/shieldOff.mp3"));
        shieldAmbientLoop = Gdx.audio.newSound(Gdx.files.internal("sound/shieldAmbient.wav"));
        
        //hyperdriveEngage = Gdx.audio.newSound(Gdx.files.internal("sound/hyperdriveInit.wav"));
        //hyperDriveDisengage = Gdx.audio.newSound(Gdx.files.internal("sound/hyperCharge.wav"));
        
        pickup = Gdx.audio.newSound(Gdx.files.internal("sound/pickup.wav"));

        dockStation = Gdx.audio.newSound(Gdx.files.internal("sound/dockStation.wav"));
        undockStation = Gdx.audio.newSound(Gdx.files.internal("sound/undockStation.wav"));
    }
    
    public static void stopSound(SoundComponent soundComponent) {
        Sound sound = activeLoops.get(soundComponent.soundID);
        if (sound != null) {
            sound.stop(soundComponent.soundID);
            sound.stop(soundComponent.soundID1);
        }
        activeLoops.removeKey(soundComponent.soundID);
        activeLoops.removeKey(soundComponent.soundID1);
        soundComponent.soundID = -1;
        soundComponent.soundID1 = -1;
        soundComponent.active = false;
    }
    
    boolean isEngineLooping = false;
    public long shipEngineActive(boolean startLoop, float pitch) {
        long shipEngineActiveID = -1;
        if (startLoop) {
            if (!isEngineLooping) {
                shipEngineActiveID = shipEngineActiveLoop.loop(1, pitch, 0);
                //activeLoops.put(shipEngineActiveID, shipEngineActiveLoop);
            }
            isEngineLooping = true;
            //shipEngineActiveLoop.setPitch(shipEngineActiveID, pitch);
        } else {
            isEngineLooping = false;
            shipEngineActiveLoop.stop();
        }
        return shipEngineActiveID;
    }
    
    float accumulator;
    public long shipEngineAmbient(SoundComponent sound, boolean active, float velocity, float angleDelta, float deltaTime) {
        if (active) {
            if (sound.soundID == -1) {
                sound.soundID = shipEngineAmbientLoop.loop();
                sound.soundID1 = shipEngineAmbientLoop.loop();
                sound.active = true;
                activeLoops.put(sound.soundID, shipEngineAmbientLoop);
                activeLoops.put(sound.soundID1, shipEngineActiveLoop);
            }
            //todo sound id of 0 seems to not play?
            //if (sound.soundID == 0) { Gdx.app.error(getClass().getSimpleName(), sound.soundID + ""); }
            
            //pitch
            float relVel = velocity / Box2DPhysicsSystem.getVelocityLimit();
            float pitch = MathUtils.map(0f, 1f, 0.5f, 2.0f, relVel);
            shipEngineAmbientLoop.setPitch(sound.soundID, pitch);
            shipEngineAmbientLoop.setPitch(sound.soundID1, pitch * (1.5f));
            //volume
            accumulator += 30.0f * relVel * deltaTime;
            float oscillator = (float) Math.abs(Math.sin(accumulator*3));//todo move to sound component?
            //shipEngineAmbientLoop.setVolume(sound.soundID, oscillator);
            //shipEngineAmbientLoop.setVolume(sound.soundID1, masterVolume);
            //DebugSystem.addDebugText(angleDelta + "", 500 ,550);
            shipEngineAmbientLoop.setPan(sound.soundID1, (float) Math.sin(accumulator), oscillator * engineVolume * 0.45f);
            shipEngineAmbientLoop.setPan(sound.soundID, 0, oscillator * engineVolume);
        } else {
            //if (true) return 0;
            if (sound.soundID != -1) {
                shipEngineAmbientLoop.setLooping(sound.soundID, false);
                shipEngineAmbientLoop.setLooping(sound.soundID1, false);
                activeLoops.removeKey(sound.soundID);
                activeLoops.removeKey(sound.soundID1);
                sound.soundID = -1;
                sound.soundID1 = -1;
                sound.active = false;
            }
        }
        return sound.soundID;
    }
    
    boolean isShieldLoop = false;
    public long shieldAmbient(boolean startLoop) {
        long shieldAmbientID = -1;
        if (startLoop) {
            if (!isShieldLoop) {
                shieldAmbientID = shieldAmbientLoop.play();
            }
            isShieldLoop = true;
        } else {
            isShieldLoop = false;
            shieldAmbientLoop.stop();
        }
        shieldAmbientLoop.setLooping(shieldAmbientID, startLoop);
        return shieldAmbientID;
    }
    
    public long asteroidShatter(float pitch, ItemComponent.Resource resource) {
        //todo: load resource sound from resource?
        //for now map ID -> break0(default),break1,break2,break3
        //resource.getId()
        //also: similar sound variation for bounce with shield: bounce0(default), bounce1, bounce2, bounce3

        return f3.play(0.25f, pitch, 0);
    }
    
    public void laserShoot() {
        laserShoot(0.3f, 1 + MathUtils.random(-0.02f, 0.02f));
    }
    
    public long laserShoot(float volume, float pitch) {
        return laserShoot.play(volume, pitch, 0);
    }
    
    public long laserCharge(float volume, float pitch) {
        return laserShootCharge.play(volume, pitch, 0);
    }
    
    public long hullImpactLight(float volume) {
        return hullImpact.play(volume, 2, 0);
    }
    
    public long hullImpactHeavy(float pitch) {
        return hullImpactHeavy.play(1, pitch, 0);
    }
    
    public long shieldImpact(float volume) {
        return shieldImpact.play(volume, 1, 0);
    }
    
    public long shieldCharge() {
        return shieldCharge.play();
    }
    
    public long shieldOn() {
        return shieldOn.play();
    }
    
    public long shieldOff() {
        return shieldOff.play();
    }
    
    public long hyperdriveEngage() {
        return 0;//disable for now
        //return hyperdriveEngageID = hyperdriveEngage.play();
    }
    
    public long pickup() {
        float pitch = MathUtils.random(0.5f, 2.0f);
        return pickup.play(0.5f, pitch, 0);
    }
    
    public long shipExplode() {
        return shipExplode.play();
    }

    public long dockStation() {
        return dockStation.play(0.25f);
    }

    public long undockStation() {
        return undockStation.play(0.25f);
    }

    @Override
    public void dispose() {
        shipEngineActiveLoop.dispose();
        shipEngineAmbientLoop.dispose();
        shipExplode.dispose();
        f3.dispose();
        laserShoot.dispose();
        laserShootCharge.dispose();
        hullImpact.dispose();
        hullImpactHeavy.dispose();
        shieldImpact.dispose();
        shieldOn.dispose();
        shieldOff.dispose();
        shieldAmbientLoop.dispose();
        pickup.dispose();
        dockStation.dispose();
        undockStation.dispose();
    }

}
