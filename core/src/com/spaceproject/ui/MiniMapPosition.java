package com.spaceproject.ui;

public enum MiniMapPosition {
    topLeft,
    topRight,
    bottomRight,
    bottomLeft;
    //custom;
    
    private static MiniMapPosition[] vals = values();
    
    public MiniMapPosition next() {
        return vals[(this.ordinal() + 1) % vals.length];
    }
}
