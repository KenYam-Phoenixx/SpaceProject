package com.spaceproject.config;


public class EntityConfig extends Config {
    //TODO: find better way to do entity config + generation

    //character
    public float characterHealth;
    public float characterWalkSpeed;
    
    //control
    public long controlTimerDodge;
    public long controlTimerVehicle;
    public long controlATimerCoolDown;
    
    //ship
    public int shipSizeMin;
    public int shipSizeMax;
    public float shipHealth;
    public float engineThrust;
    public float hyperSpeed;
    public float dodgeForce;
    public long dodgeAnimationTimer;
    public int cannonSize;
    public int cannonAmmo;
    public int cannonDamage;
    public long cannonFireRate;
    public long cannonRechargeRate;
    public float cannonVelocity;
    public float cannonAcceleration;
    public long shrinkGrowAnimTime;
    
    
    
    @Override
    public void loadDefault() {
        characterHealth = 100;
        characterWalkSpeed = 200f;
        
        controlTimerVehicle = 1000;
        controlTimerDodge = 500;
        controlATimerCoolDown = 2000;
        
        shipSizeMin = 10/2;
        shipSizeMax = 36/2;
        shipHealth = 200;
        engineThrust = 200;
        hyperSpeed = 320;
        dodgeAnimationTimer = 475;
        dodgeForce = 5f;
        cannonSize = 1;
        cannonAmmo = 5;
        cannonDamage = 15;
        cannonFireRate = 200;
        cannonRechargeRate = 1000;
        cannonVelocity = 60;
        cannonAcceleration = 200;
        shrinkGrowAnimTime = 2500;
        
    }
    
}
