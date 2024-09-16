package com.project.generation;

import com.project.system.*;
import com.project.core.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.Buffer;
import java.util.*;
import java.util.List;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.*;

public class NoiseGenerator {

    public float scale = 400f;

    public final float waterHeight = 0.6f;
    public final float waveStrength = 0.01f;
    public static Color waterColour = new Color(177,222,228);

    public float foamLine = 0.57f;
    public float foamWidth = 0.005f;
    public float foamAmount = 0.575f;

    public double[][] noise;

    public double[][] water;
    public float waterScroll = 0;
    public float waveSpeed = 0.02f;
    public float foamFade = 0.05f;

    public FoamData[][] foam;
    public float wakeCutoff = 0f;

    Vector2 position;

    public PerlinNoise perlin;

    public float seed;
    Game game;

        public static Region[] regions = new Region[]{
            new Region("Water", new Color(177,222,228), 0f),
            new Region("ShallowWater", new Color(192, 238, 245), 0.55f),
            new Region("Rock", Color.GRAY, 0.6f),
            new Region("Grass", new Color(58, 125, 24), 0.63f),
            new Region("Rock", Color.GRAY, 0.7f),
            new Region("Snow", Color.white, 0.72f)
    };

    //new world generator with reference to game with dimensions size
    public NoiseGenerator(Game _game, Point size){
        perlin = new PerlinNoise(0);

        game = _game;

        seed = new Random().nextFloat() * 99999f;

        //generate seed
        while(1 - perlin.OctavePerlin((BoatMovement.startPosition.x + game.getWidth()/2f) / scale,(BoatMovement.startPosition.y + game.getHeight()/2f) / scale, seed, 3, 1) >= regions[1].height){
            seed = new Random().nextFloat() * 99999f;
        }

        foam = new FoamData[size.x][size.y];
        water = new double[size.x][size.y];
    }

    //generates heightmap with dimensions size starting at position pos
    public double[][] generateNoise(Point size, Vector2 pos){
        position = pos;
        noise = new double[size.x][size.y];
        water = new double[size.x][size.y];

        for(int x = 0; x < noise.length; x++){
            for(int y = 0; y < noise[x].length; y++){
                noise[x][y] = 1 - perlin.OctavePerlin((x + pos.x) / scale,(y + pos.y) / scale, seed, 3, 1);
                water[x][y] = perlin.perlin((x + pos.x) / scale,(y + pos.y) / scale, waterScroll);
            }
        }

        //returns heightmap
        return noise;
    }

    //adds foam particles within area at position with velocity out
    public void addFoam(Shape area, Vector2 position, Vector2 out){
        final float wakeSpread = 50; //in degrees
        float angle = out.toAngle();

        float absAngle = angle > 90? angle : angle;

        out = new Vector2(position.x + out.x * 10, position.y * out.y * 10);

        Vector2 right = new Vector2(position.x + new Vector2(absAngle + wakeSpread).x, position.y + new Vector2(absAngle + wakeSpread).y);
        Vector2 left = new Vector2(position.x + new Vector2(absAngle - wakeSpread).x, position.y + new Vector2(absAngle - wakeSpread).y);

        for(int x = 0; x < foam.length; x++){
            for(int y = 0; y < foam[0].length; y++){
                if(area.contains(new Point(x,y))){
                    foam[x][y] = new FoamData(x,y);

                    int fixSide = out.x - position.x > 0? 1 : -1;
                    float _angle = angle;

                    float ox = out.x - position.x;
                    ox /= -Math.abs(ox);
                    foam[x][y].spread = Vector2.distance(right, new Vector2(x,y)) > Vector2.distance(left, new Vector2(x,y))? (int)(ox) : -(int)(ox);

                    _angle += wakeSpread * foam[x][y].spread;

                    Vector2 dir = new Vector2(_angle);
                    float rspeed = (80 + new Random().nextInt(40))/100f; //for randomness
                    foam[x][y].dir = new Vector2(dir.x * rspeed * fixSide, dir.y * rspeed * fixSide);
                }
            }
        }
    }

    //moves foam particles based on velocity and camera position, offset
    public void moveFoam(Vector2 offset){
        FoamData[][] newFoam = new FoamData[foam.length][foam[0].length];

        for(int x = 0; x < foam.length; x++){
            for(int y = 0; y < foam[0].length; y++){
                if(foam[x][y] == null){
                    foam[x][y] = new FoamData(-1,-1);
                }
                if(foam[x][y].x >= 0 && foam[x][y].y >= 0){
                    foam[x][y].x += offset.x + foam[x][y].dir.x * 3;
                    foam[x][y].y += offset.y + foam[x][y].dir.y * 3;
                    int _x = (int)Math.floor(foam[x][y].x+offset.x);
                    int _y = (int)Math.floor(foam[x][y].y+offset.y);
                    if(_x < foam.length && _x >= 0  &&  _y < foam[0].length && _y >= 0){
                        if(noise[_x][_y] < regions[2].height)
                            newFoam[_x][_y] = foam[x][y];
                        else
                            foam[x][y].dir = new Vector2(0,0);
                        //continue;
                    }
                }

                foam[x][y] = new FoamData(-1,-1);
            }
        }

        foam = newFoam;
    }

    //creates a coloured image with heightdata noise and moves foam with offset
    public BufferedImage displayNoise(double[][] noise, Vector2 offset){
        moveFoam(offset);
        BufferedImage texture = new BufferedImage(noise.length, noise[0].length, BufferedImage.TYPE_INT_RGB);

        try{
            for(int x = 0; x < noise.length; x++){
                for(int y = 0; y < noise[x].length; y++){
                    int n = 0;

                    if(foam[x][y] == null){
                        foam[x][y] = new FoamData(-1,-1);
                    }

                    //draw and handle foam
                    if(foam[x][y].x >= 0 && foam[x][y].y >= 0 && foam[x][y].val >= wakeCutoff && noise[x][y] < regions[2].height){
                        n = Color.white.getRGB();
                        foam[x][y].val -= foamFade;
                    }
                    else if(noise[x][y] > foamLine - foamWidth && noise[x][y] < foamLine + foamWidth && Math.sin(water[x][y]) > foamAmount){
                        n = Color.white.getRGB();
                    }
                    //draw water and islands
                    else{
                        for(int i = 0; i < regions.length; i++){
                            if(noise[x][y] > regions[i].height){
                                n = regions[i].colour.getRGB();
                                //check for collision
                                if(!game.dead && game.inGame && i >= 2 && Vector2.distance(new Vector2(x + position.x, y+ position.y), new Vector2(position.x + game.getWidth()/2f, position.y + game.getHeight()/2f)) < 20){
                                    game.dead = true;
                                    AudioInputStream sound = AudioSystem.getAudioInputStream(new File("assets/sounds/crash.wav"));
                                    game.crashFX = AudioSystem.getClip();
                                    game.crashFX.open(sound);
                                    game.crashFX.setFramePosition(0);
                                    game.crashFX.start();
                                }
                                //n = new Color((int) (regions[i].colour.getRed() * noise[x][y]), (int) (regions[i].colour.getGreen() * noise[x][y]), (int) (regions[i].colour.getBlue() * noise[x][y])).getRGB();
                            }
                        }
                    }

                    //debug attack range
                    if(game.debugArrows && game.debugArrowRange != null && game.debugArrowRange.contains(new Point(x,y))){
                        n = blendColour(Color.gray, new Color(n)).getRGB();
                    }

                    //draw valhalla
                    if(Vector2.distance(new Vector2(x + position.x, y+ position.y), game.endPos) < 20){
                        n = Color.yellow.getRGB();
                    }

                    //draw collectible points and check for pickup
                    for(int i = 0; i < game.points.size(); i++){
                        if(Vector2.distance(game.points.get(i), new Vector2(position.x + game.getWidth()/2f, position.y + game.getHeight()/2f)) < 20){
                            game.points.remove(i);
                            i--;
                            game.score+= Game.pointValue;
                            AudioInputStream sound = AudioSystem.getAudioInputStream(new File("assets/sounds/coin.wav"));
                            game.dingFX = AudioSystem.getClip();
                            game.dingFX.open(sound);
                            game.dingFX.setFramePosition(0);
                            game.dingFX.start();
                        }
                        else if(Vector2.distance(game.points.get(i), new Vector2(x + position.x, y+ position.y)) < 5){
                            n = Color.yellow.getRGB();
                        }
                    }

                    texture.setRGB(x,y, n);

                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        //returns the image
        return texture;
    }

    //data for different world regions and colours
    public static class Region{
        public String name; //for organization
        public Color colour;
        public float height;

        public Region(String s, Color c, float h){
            name = s;
            colour = c;
            height = h;
        }
    }

    //foam particles
    public static class FoamData {
        public float x;
        public float y;
        public float val = 1;

        public Vector2 dir = new Vector2(0,0);

        public int spread = 0; //for debugging

        public FoamData(float _x, float _y){
            x = _x;
            y = _y;
        }
    }

    //returns colour between a and b
    public static Color blendColour(Color a, Color b) {
        double totalAlpha = a.getAlpha() + b.getAlpha();
        double weighta = a.getAlpha() / totalAlpha;
        double weightb = b.getAlpha() / totalAlpha;

        double _r = weighta * a.getRed() + weightb * b.getRed();
        double _g = weighta * a.getGreen() + weightb * b.getGreen();
        double _b = weighta * a.getBlue() + weightb * b.getBlue();
        double _a = Math.max(a.getAlpha(), b.getAlpha());

        return new Color((int) _r, (int) _g, (int) _b, (int) _a);
    }
}
