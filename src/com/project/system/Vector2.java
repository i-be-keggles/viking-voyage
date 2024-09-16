package com.project.system;

public class Vector2 {
    public float x;
    public float y;

    public Vector2(float _x, float _y){
        x = _x;
        y = _y;
    }

    //creates a vector from angle angle
    public Vector2(float angle){
        angle = (float)Math.toRadians(angle);

        x = (float) Math.cos(angle);
        y = (float) Math.sin(angle);
    }

    //returns vector with length 1
    public Vector2 normalized(){
        float scale = (float)Math.sqrt(this.x*this.x + this.y*this.y);
        return new Vector2(this.x / scale, this.y /scale);
    }

    //returns angle from vector
    public float toAngle(){
        return (float) Math.toDegrees(Math.atan(this.y / this.x));
    }

    //returns distance between two points
    public static float distance(Vector2 a, Vector2 b){
        return (float) Math.sqrt(Math.pow(b.x-a.x, 2) + Math.pow(b.y-a.y, 2));
    }
}
