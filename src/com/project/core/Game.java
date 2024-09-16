//Kye Electriciteh
//ICS3U1
//June 20th, 2021
//A game where you sail around a procedurally generated world collecting points and looking for valhalla

package com.project.core;

import com.project.generation.NoiseGenerator;
import com.project.system.ImageUtil;
import com.project.system.Vector2;

import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.swing.*;
import java.awt.image.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class Game extends JPanel implements ActionListener, KeyListener, MouseListener {
    static JFrame frame;
    Image boat;
    BufferedImage map;
    BufferedImage foam;
    BufferedImage clueImage;
    Image deathScreen;
    Image winScreen;
    Image startScreen;
    Image helpScreen;

    public int score;
    public static int pointValue = 5;
    public static int pointCount = 3;
    public List<Vector2> points = new ArrayList<>();
    public Vector2 pointPos;
    public static float pointDistance = 400;

    public boolean dead = false;
    public boolean inGame = false;
    public boolean paused = false;
    public boolean won = false;
    public boolean help = false;

    public BoatMovement movement;
    NoiseGenerator noise;

    public static Dimension screenSize = new Dimension(800, 500);
    static Dimension boatSize = new Dimension(100, 100);

    public Vector2 endPos; //position of valhalla in gameWorld
    static float endDistance = 1500;
    static float endDeviance = 300;

    public List<ArrowsGif> arrows = new ArrayList<>();
    public BufferedImage arrowGif;
    public Shape debugArrowRange;
    public boolean debugArrows = false;

    Vector2 cluePos;
    static int clueCount = 4;
    public Clue[][] clues = new Clue[3][];

    public Clip sailsFX, waterFX, crashFX, dingFX, winFX;
    public static Button howToButton;

    public static void main(String[] args){
        frame = new JFrame ("A Viking's Voyage");
        Game panel = new Game ();
        panel.setLayout(new GridLayout(4,4,4,4));

        frame.add (panel);
        frame.pack ();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible (true);
    }

    public Game (){
        //set up menu items
        JMenu gameMenu = new JMenu("OPTIONS");
        JMenuItem newGame = new JMenuItem("New Game");
        newGame.setActionCommand("new game");
        newGame.addActionListener(this);
        gameMenu.add(newGame);

        JMenuItem pause = new JMenuItem("Pause");
        pause.setActionCommand("pause");
        pause.addActionListener(this);
        gameMenu.add(pause);

        JMenuItem howTo = new JMenuItem("How To");
        howTo.setActionCommand("how to");
        howTo.addActionListener(this);
        gameMenu.add(howTo);

        JMenu devMenu = new JMenu("DEV TOOLS");
        JMenuItem arrowDebugOption = new JMenuItem("Debug Arrow Range");
        arrowDebugOption.setActionCommand("debug arrows");
        arrowDebugOption.addActionListener(this);
        devMenu.add(arrowDebugOption);

        JMenuBar menuBar = new JMenuBar();

        menuBar.add(newGame);
        menuBar.add(pause);
        menuBar.add(howTo);
        menuBar.add(devMenu);

        frame.setJMenuBar(menuBar);

        //basic frame initialization
        setPreferredSize (new Dimension (800, 500));
        setBackground (new Color (200, 200, 200));
        setLayout (new BoxLayout (this, BoxLayout.PAGE_AXIS));
        addKeyListener (this);

        MediaTracker tracker = new MediaTracker (this);

        //load all images
        boat = Toolkit.getDefaultToolkit().getImage("assets/images/boat.png");
        startScreen = Toolkit.getDefaultToolkit().getImage("assets/images/start.gif");
        deathScreen = Toolkit.getDefaultToolkit().getImage("assets/images/death.png");
        winScreen = Toolkit.getDefaultToolkit().getImage("assets/images/win.png");
        helpScreen = Toolkit.getDefaultToolkit().getImage("assets/images/help.png");
        try{
            clueImage = ImageIO.read(new File("assets/images/clue.png"));
            arrowGif = ImageIO.read(new File("assets/images/arrows.gif"));
        }
        catch (Exception e){
            e.printStackTrace();
        }
        //map = noise.displayNoise(noise.generateNoise(new Point(screenSize.width, screenSize.height), new Vector2(0,0)));
        map = new BufferedImage(screenSize.width, screenSize.height, BufferedImage.TYPE_INT_ARGB);
        foam = new BufferedImage(screenSize.width, screenSize.height, BufferedImage.TYPE_INT_ARGB);
        tracker.addImage(boat, 0);
        tracker.addImage(map, 1);
        tracker.addImage(foam, 2);
        tracker.addImage(clueImage, 3);
        tracker.addImage(arrowGif, 4);
        tracker.addImage(startScreen, 5);
        tracker.addImage(deathScreen, 6);
        tracker.addImage(winScreen, 7);
        tracker.addImage(helpScreen, 8);

        //background sounds
        try{
            AudioInputStream sound = AudioSystem.getAudioInputStream(new File("assets/sounds/sail.wav"));
            sailsFX = AudioSystem.getClip();
            sailsFX.open(sound);
            sailsFX.setFramePosition(0);
            sailsFX.loop(-1);
            sound = AudioSystem.getAudioInputStream(new File("assets/sounds/water.wav"));
            waterFX = AudioSystem.getClip();
            waterFX.open(sound);
            FloatControl gainControl = (FloatControl) waterFX.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(-20.0f);
            waterFX.setFramePosition(0);
            waterFX.loop(-1);
        }
        catch (Exception e){}

        //get mouse data
        this.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if(movement != null){
                    movement.cursor = e.getPoint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if(movement != null){
                    movement.cursor = e.getPoint();
                }
            }
        });

        //check for mouse clicks and shoot
        this.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //arrows.add(new ArrowsGif(movement.curAngle, new Vector2(movement.position.x + screenSize.width/2f, movement.position.y + screenSize.height/2f)));
            }
            @Override
            public void mousePressed(MouseEvent e) {

            }
            @Override
            public void mouseReleased(MouseEvent e) {

            }
            @Override
            public void mouseEntered(MouseEvent e) {

            }
            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
    }

    //generate and configure a new game
    public void newGame(){
        paused = true;
        score = 0;
        float endAngle = 360f * new Random().nextFloat();
        float endDist = endDistance + endDeviance * (new Random().nextFloat() - 0.5f) * 2;
        endPos = new Vector2(endAngle).normalized();
        Vector2 offset = BoatMovement.startPosition;
        endPos = new Vector2(offset.x + (float)screenSize.getWidth()/2f + endPos.x * endDist, offset.y + (float)screenSize.getHeight()/2f + endPos.y * endDist);

        noise = new NoiseGenerator(this, new Point(screenSize.width, screenSize.height));
        generateClues(BoatMovement.startPosition);
        generatePoints(BoatMovement.startPosition);


        if(movement != null){
            movement.position = BoatMovement.startPosition;
        }

        dead = false;
        inGame = true;
        won = false;
        paused = false;
        help = false;

        repaint();
    }

    public void paintComponent (Graphics g)
    {
        super.paintComponent(g);
        if(inGame){
            if(movement == null){
                movement = new BoatMovement(g, this);
                movement.start();
            }

            //draw environment
            g.drawImage(ImageUtil.pixelize(map, 4, new Point(screenSize.width, screenSize.height)), 0,0, screenSize.width, screenSize.height, this);
            //g.drawImage(foam, 0,0, screenSize.width, screenSize.height, this);
            g.drawImage(boat, (screenSize.width - boatSize.width)/2, (screenSize.height - boatSize.height)/2, boatSize.width, boatSize.height, this);

            //handle dynamic generation and display of clues and points
            if(Vector2.distance(new Vector2(movement.position.x + getWidth()/2f, movement.position.y + getHeight()/2f), cluePos) >= Game.endDistance*1.5f) generateClues(new Vector2(movement.position.x + getWidth()/2f, movement.position.y + getHeight()/2f));
            if(Vector2.distance(new Vector2(movement.position.x + getWidth()/2f, movement.position.y + getHeight()/2f), pointPos) >= pointDistance*2) generatePoints(new Vector2(movement.position.x + getWidth()/2f, movement.position.y + getHeight()/2f));

            for(int i = 0; i < clues.length; i++){
                for(int j = 0; j < clues[i].length; j++){
                    if(clues[i][j] != null && Vector2.distance(new Vector2(clues[i][j].x - movement.position.x, clues[i][j].y - movement.position.y), new Vector2((float)screenSize.width/2f, (float)screenSize.getHeight()/2f)) < screenSize.getWidth()){
                        g.drawImage(ImageUtil.rotate(clueImage, clues[i][j].direction + (!clues[i][j].flipped? 180 : 0), 0.5f), (int)(clues[i][j].x - movement.position.x), (int)(clues[i][j].y - movement.position.y), 50, 50, this);
                    }
                }
            }

        /*
        for(int i = 0; i < arrows.size(); i++){
            g.drawImage(ImageUtil.rotate(arrowGif, arrows.get(i).angle, 1), (int)(arrows.get(i).position.x - movement.position.x), (int)(arrows.get(i).position.y - movement.position.y), 50,50, this);
            arrows.get(i).timer--;
            if(arrows.get(i).timer <= 0){
                arrows.remove(i);
                i--;
            }
        }*/

            g.drawString("SCORE: " + score, 720, 10);

            if(dead){
                g.drawImage(deathScreen, 0,0, screenSize.width, screenSize.height, this);
            }
            else if(won){
                g.drawImage(winScreen, 0,0, screenSize.width, screenSize.height, this);
            }
        }
        else{
            g.drawImage(startScreen, 0,0, screenSize.width, screenSize.height, this);
        }

        if(help){
            g.drawImage(helpScreen, 0,0, screenSize.width, screenSize.height, this);
        }
    }

    //generates clues in a radius around position
    public void generateClues(Vector2 position) {
        cluePos = position;

        clues = new Clue[3][clueCount];

        for(int i = 0; i < clues.length; i++){
            clues[i] = new Clue[clueCount];
            for(int j = 0; j < clueCount; j++){
                Vector2 pos = new Vector2(position.x - screenSize.width/2f, position.y - screenSize.height/2f);

                //set position
                do {
                    float angle = 360f * new Random().nextFloat();
                    float dist = endDistance * 1.5f * (new Random().nextFloat() - 0.5f) * 2f;
                    pos = new Vector2(angle).normalized();
                    pos = new Vector2(position.x + (float)screenSize.getWidth()/2f + pos.x * dist, position.y + (float)screenSize.getHeight()/2f + pos.y * dist);

                }while(1 - noise.perlin.OctavePerlin((pos.x) / noise.scale,(pos.y) / noise.scale, noise.seed, 3, 1) < 0.6f);

                //set pointer
                if(i == 0){
                    clues[i][j] = new Clue(pos.x, pos.y, new Vector2(endPos.x - pos.x, endPos.y - pos.y).toAngle(), endPos.x - pos.x < 0);
                }
                else{
                    int n = new Random().nextInt(clueCount);
                    clues[i][j] = new Clue(pos.x, pos.y, new Vector2(clues[i-1][n].x - pos.x, clues[i-1][n].y - pos.y).toAngle(), clues[i-1][n].x - pos.x < 0);
                    //clues[i][j].order = i+1;
                }
            }
        }
    }

    //generates collectible points in a radius around position
    public void generatePoints(Vector2 position){
        pointPos = position;

        points.clear();

        for(int i = 0; i < pointCount; i++){
            Vector2 pos;

            //set position
            do {
                float angle = 360f * new Random().nextFloat();
                float dist = pointDistance + (new Random().nextFloat() * 400);
                pos = new Vector2(angle).normalized();
                pos = new Vector2(position.x + (float)screenSize.getWidth()/2f + pos.x * dist, position.y + (float)screenSize.getHeight()/2f + pos.y * dist);

            }while(1 - noise.perlin.OctavePerlin((pos.x) / noise.scale,(pos.y) / noise.scale, noise.seed, 3, 1) > 0.6f);

            points.add(new Vector2((int)pos.x, (int)pos.y));
        }
    }

    //data for arrows visual
    public static class ArrowsGif {
        public int timer = 20;
        public float angle;
        public Vector2 position;

        public ArrowsGif(float a, Vector2 p){
            angle = a;
            position = p;
        }
    }

    //handle menu items
    public void actionPerformed (ActionEvent event) {
        String eventName = event.getActionCommand ();
        if (eventName.equals ("new game"))
        {
            newGame();//for some reason you need to press it twice
        }
        else if (eventName.equals ("pause"))
        {
            paused = !paused;
            System.out.println("Paused: " + paused);
        }
        else if (eventName.equals ("debug arrows"))
        {
            debugArrows = !debugArrows;
        }
        else if(eventName.equals("how to")){
            help = !help;
        }
    }

    public void keyTyped(KeyEvent e) {

    }
    public void keyPressed(KeyEvent e) {

    }
    public void keyReleased(KeyEvent e) {

    }
    public void mouseClicked(MouseEvent e) {
    }
    public void mousePressed(MouseEvent e) {

    }
    public void mouseReleased(MouseEvent e) {
    }
    public void mouseEntered(MouseEvent e) {

    }
    public void mouseExited(MouseEvent e) {

    }

    //data for clues
    public static class Clue{
        public float x;
        public float y;
        public float direction; //angle
        public boolean flipped;
        public int order = 1;

        public Clue(float _x, float _y, float dir, boolean b){
            x = _x;
            y = _y;
            direction = dir;
            flipped = b;
        }
    }
}
