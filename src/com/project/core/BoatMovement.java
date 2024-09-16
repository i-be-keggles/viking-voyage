package com.project.core;

import com.project.system.*;
import org.w3c.dom.css.Rect;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;

public class BoatMovement extends Thread {

    public final float speed = 5;

    Graphics g;
    Point cursor = new Point(0,0);
    float curAngle = 0;
    Game game;
    BufferedImage base;
    int offset = -90;

    public final static int wakeSize = 25;

    public static Vector2 startPosition = new Vector2(30000, 30000);

    public Vector2 position;

    public float turbulence = 0.1f;
    public float wavePoint = 0;

    public Shape arrows;
    public int arrowRange = 80; //inPixels
    public float arrowWidth = 0.8f;
    Vector2 dir = new Vector2(0,0);

    //new movement handler with reference to graphics object and main class
    BoatMovement(Graphics _g, Game gm){
        position = startPosition;
        g = _g;
        game = gm;
        try{
            base = ImageIO.read(new File("assets/images/boat.png"));
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    //main loop
    public void run(){
        while(true){
            System.out.print("");
            if(game.paused || game.help) continue;

            //handle movement and wake
            if(!game.dead && !game.won){
                if(Vector2.distance(new Vector2(position.x + game.getWidth()/2f, position.y + game.getHeight()/2f), game.endPos) < 100){
                    game.won = true;
                    game.score += 100;
                    try {
                        AudioInputStream sound = AudioSystem.getAudioInputStream(new File("assets/sounds/win.wav"));
                        game.winFX = AudioSystem.getClip();
                        game.winFX.open(sound);
                        game.winFX.setFramePosition(0);
                        game.winFX.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                dir = new Vector2(game.getWidth() /2f - cursor.x, game.getHeight() /2f - cursor.y);
                curAngle = dir.toAngle();
                curAngle += offset;
                if(dir.x < 0) curAngle += 180;
                else if(dir.y < 0 || curAngle < 0) curAngle += 360;
                game.boat = ImageUtil.rotate(base, curAngle, (float)game.noise.water[game.getWidth() /2][game.getHeight() /2] * 0.5f);

                dir = dir.normalized();
                position = new Vector2(position.x - dir.x * speed, position.y - dir.y * speed);
                dir = new Vector2(dir.x * speed, dir.y * speed);

                Rectangle wake = new Rectangle((int) (game.getWidth()/2 - wakeSize/2 + dir.normalized().x * 10), (int)(game.getHeight()/2 - wakeSize/2 + dir.normalized().y * 10), wakeSize,  wakeSize);
                game.noise.addFoam(wake, new Vector2(game.getWidth()/2f, game.getHeight()/2f), dir);

                game.debugArrowRange = arrows;
            }

            //update environment
            wavePoint += 0.02f;

            game.noise.waterScroll += game.noise.waveSpeed;
            game.map = game.noise.displayNoise(game.noise.generateNoise(new Point(game.getWidth(), game.getHeight()), position), dir);

            //handle collider for damage/attacking
            AffineTransform tx = new AffineTransform();
            tx.rotate(Math.toRadians(curAngle), game.getWidth()/2f, game.getHeight()/2f);
            arrows = new Rectangle(game.getWidth()/2 - arrowRange, game.getHeight()/2 - (int)(Game.boatSize.height * arrowWidth/2), arrowRange*2, (int)(Game.boatSize.height * arrowWidth));
            arrows = tx.createTransformedShape(arrows);

            game.repaint();
        }
    }
}
