import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Random;
import javax.swing.*;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class PacMan extends JPanel implements ActionListener, KeyListener {

    private void playSound(String fileName, boolean loop) {
        try {
            File soundFile = new File(fileName);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            if (loop) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                clip.start();
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }


    class Block {
        int x;
        int y;
        int width;
        int height;
        Image image;

        int startX;
        int startY;
        char direction = 'U'; // U D L R
        int velocityX = 0;
        int velocityY = 0;

        Image originalImage;

        Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.originalImage = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }

        void updateDirection(char direction) {
            char prevDirection = this.direction;
            this.direction = direction;
            updateVelocity();
            this.x += this.velocityX;
            this.y += this.velocityY;
            for (Block wall : walls) {
                if (collision(this, wall)) {
                    this.x -= this.velocityX;
                    this.y -= this.velocityY;
                    this.direction = prevDirection;
                    updateVelocity();
                }
            }
        }

        void updateVelocity() {
            if (this.direction == 'U') {
                this.velocityX = 0;
                this.velocityY = -tileSize/4;
            }
            else if (this.direction == 'D') {
                this.velocityX = 0;
                this.velocityY = tileSize/4;
            }
            else if (this.direction == 'L') {
                this.velocityX = -tileSize/4;
                this.velocityY = 0;
            }
            else if (this.direction == 'R') {
                this.velocityX = tileSize/4;
                this.velocityY = 0;
            }
        }

        void reset() {
            this.x = this.startX;
            this.y = this.startY;
        }
    }

    private int rowCount = 21;
    private int columnCount = 19;
    private int tileSize = 32;
    private int boardWidth = columnCount * tileSize;
    private int boardHeight = rowCount * tileSize;

    private Image wallImage;
    private Image blueGhostImage;
    private Image orangeGhostImage;
    private Image pinkGhostImage;
    private Image redGhostImage;

    private Image pacmanUpImage;
    private Image pacmanDownImage;
    private Image pacmanLeftImage;
    private Image pacmanRightImage;
    private Image scaredGhostImage;
    
    private Image cherryImage;
    private boolean isCherryActive = false;
    private int cherryTimer = 0;
    private int cherrySpawnTimer = 0; 
    private final int CHERRY_SPAWN_INTERVAL = 300; 
    private final int CHERRY_DURATION = 100; 

    private final int CHERRY_FLASH_START = 100; 
    private final int CHERRY_FLASH_DURATION = 40;
    private boolean isCherryFlashing = false;
    private boolean showCherry = true; 
    private int cherryFlashCounter = 0;
    private Block activeCherry = null; 

    private boolean areGhostsScared = false;
    private int scaredTimer = 0;
    private final int SCARED_DURATION = 150;  // 100 * 50ms = 5000ms = 5s
    private boolean areGhostsFlashing = false;
    private int ghostFlashCounter = 0;
    private final int FLASH_DURATION = 20;    // 20 * 50ms = 1000ms = 1s
    private boolean showFlashingGhosts = true;
    

    //X = wall, O = skip, P = pac man, ' ' = food
    //Ghosts: b = blue, o = orange, p = pink, r = red
    private String[] tileMap = {
        "XXXXXXXXXXXXXXXXXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X                 X",
        "X XX X XXXXX X XX X",
        "X    X       X    X",
        "XXXX XXXX XXXX XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXrXX X XXXX",
        "O       bpo       O",
        "XXXX X XXXXX X XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXXXX X XXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X  X     P     X  X",
        "XX X X XXXXX X X XX",
        "X    X   X   X    X",
        "X XXXXXX X XXXXXX X",
        "X                 X",
        "XXXXXXXXXXXXXXXXXXX" 
    };

    HashSet<Block> walls;
    HashSet<Block> foods;
    HashSet<Block> ghosts;
    HashSet<Block> cherries = new HashSet<>();

    Block pacman;

    Timer gameLoop;
    char[] directions = {'U', 'D', 'L', 'R'}; //up down left right
    char nextDirection = ' ';
    Random random = new Random();
    int score = 0;
    int lives = 3;
    boolean gameOver = false;
    boolean isPacmanVisible = true;
   


    PacMan() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        //load images
        wallImage = new ImageIcon(getClass().getResource("./wall.png")).getImage();
        cherryImage = new ImageIcon(getClass().getResource("./cherry.png")).getImage();
        blueGhostImage = new ImageIcon(getClass().getResource("./blueGhost.png")).getImage();
        orangeGhostImage = new ImageIcon(getClass().getResource("./orangeGhost.png")).getImage();
        pinkGhostImage = new ImageIcon(getClass().getResource("./pinkGhost.png")).getImage();
        redGhostImage = new ImageIcon(getClass().getResource("./redGhost.png")).getImage();
        scaredGhostImage = new ImageIcon(getClass().getResource("./scaredGhost.png")).getImage();

        pacmanUpImage = new ImageIcon(getClass().getResource("./pacmanUp.png")).getImage();
        pacmanDownImage = new ImageIcon(getClass().getResource("./pacmanDown.png")).getImage();
        pacmanLeftImage = new ImageIcon(getClass().getResource("./pacmanLeft.png")).getImage();
        pacmanRightImage = new ImageIcon(getClass().getResource("./pacmanRight.png")).getImage();

        loadMap();
        for (Block ghost : ghosts) {
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
        //how long it takes to start timer, milliseconds gone between frames
        gameLoop = new Timer(50, this); //20fps (1000/50)
        gameLoop.start();
        playSound("start_music.wav", false); // background music loop
        cherrySpawnTimer = CHERRY_SPAWN_INTERVAL;

    }

    public void loadMap() {
        walls = new HashSet<Block>();
        foods = new HashSet<Block>();
        ghosts = new HashSet<Block>();
    
        int cherryCount = 0; // <-- Count cherries
    
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                String row = tileMap[r];
                char tileMapChar = row.charAt(c);
    
                int x = c * tileSize;
                int y = r * tileSize;
    
                if (tileMapChar == ' ') {
                    if (cherryCount < 2 && random.nextInt(1000) < 20) {
                        Block cherry = new Block(cherryImage, x, y, tileSize, tileSize);
                        cherries.add(cherry);
                        cherryCount++; 
                    } else {
                        Block food = new Block(null, x + 14, y + 14, 4, 4);
                        foods.add(food);
                    }
                }
                
    
                if (tileMapChar == 'X') {
                    Block wall = new Block(wallImage, x, y, tileSize, tileSize);
                    walls.add(wall);
                } else if (tileMapChar == 'b') {
                    ghosts.add(new Block(blueGhostImage, x, y, tileSize, tileSize));
                } else if (tileMapChar == 'o') {
                    ghosts.add(new Block(orangeGhostImage, x, y, tileSize, tileSize));
                } else if (tileMapChar == 'p') {
                    ghosts.add(new Block(pinkGhostImage, x, y, tileSize, tileSize));
                } else if (tileMapChar == 'r') {
                    ghosts.add(new Block(redGhostImage, x, y, tileSize, tileSize));
                } else if (tileMapChar == 'P') {
                    pacman = new Block(pacmanRightImage, x, y, tileSize, tileSize);
                }
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void playDeathAnimation() {
        gameLoop.stop();
    
        Timer flashTimer = new Timer(200, null); // Flash every 200ms
        final int[] flashes = {0};
        final int maxFlashes = 6;
    
        flashTimer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isPacmanVisible = !isPacmanVisible;
                repaint();
                flashes[0]++;
                if (flashes[0] >= maxFlashes) {
                    flashTimer.stop();
                    isPacmanVisible = true;
                    resetPositions();
                    gameLoop.start();
                }
            }
        });
    
        flashTimer.start();
    }

    public void draw(Graphics g) {
        if (isPacmanVisible) {
            g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);
        }
        for (Block ghost : ghosts) {
            Image ghostImg;

             if (areGhostsScared) {
               if (areGhostsFlashing) {
                 ghostImg = showFlashingGhosts ? scaredGhostImage : ghost.image;
                 } else {
                   ghostImg = scaredGhostImage;
                }
                } else {
                  ghostImg = ghost.image;
               }

            g.drawImage(ghostImg, ghost.x, ghost.y, ghost.width, ghost.height, null);
        }


        for (Block wall : walls) {
            g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null);
        }

        for (Block cherry : cherries) {
            if (!isCherryFlashing || showCherry) {
                g.drawImage(cherry.image, cherry.x, cherry.y, cherry.width, cherry.height, null);
            }
        }

        g.setColor(Color.WHITE);
        for (Block food : foods) {
            if (food.image != null) {
                if (food.image == cherryImage) {
                    if (!isCherryFlashing || showCherry) {
                        g.drawImage(food.image, food.x, food.y, food.width, food.height, null);
                    }
                } else {
                    g.drawImage(food.image, food.x, food.y, food.width, food.height, null);
                }
            } else {
                g.setColor(Color.WHITE);
                g.fillRect(food.x, food.y, food.width, food.height);
            }
        }
        //score
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        if (gameOver) {
            g.drawString("Game Over: " + String.valueOf(score), tileSize/2, tileSize/2);
        }
        else {
            g.drawString("x" + String.valueOf(lives) + " Score: " + String.valueOf(score), tileSize/2, tileSize/2);
        }
    }

    public void move() {
       
        if (nextDirection != pacman.direction && canMove(pacman, nextDirection)) {
            pacman.updateDirection(nextDirection);
            updatePacmanImage();
        }
       
        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;
        if (pacman.y == tileSize * 9) { // row with portals
            if (pacman.x < -pacman.width / 2) {
                pacman.x = boardWidth - tileSize;
            } else if (pacman.x > boardWidth - tileSize / 2) {
                pacman.x = 0;
            }
        }

        //check wall collisions
        for (Block wall : walls) {
            if (collision(pacman, wall)) {
                pacman.x -= pacman.velocityX;
                pacman.y -= pacman.velocityY;
                break;
            }
        }

        //check ghost collisions
        for (Block ghost : ghosts) {
            if (collision(ghost, pacman)) {
                if (isCherryActive) {
                    playSound("eat_ghost.wav", false);
                    ghost.reset();
                    ghost.image = ghost.originalImage;
                
                    // Flash ghost briefly
                    Timer flashGhost = new Timer(100, null);
                    final int[] flashCount = {0};
                    flashGhost.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            if (flashCount[0] % 2 == 0) {
                                ghost.image = null; // hide
                                isCherryActive = false;
                            } else {
                                ghost.image = scaredGhostImage; // show scared again briefly
                            }
                            flashCount[0]++;
                            if (flashCount[0] >= 6) { // 3 full flashes
                                ghost.image = ghost.originalImage;
                                flashGhost.stop();
                                isCherryActive = false;
                            }
                            repaint();
                        }
                    });
                    
                    flashGhost.start();
                
                    char newDirection = directions[random.nextInt(4)];
                    ghost.updateDirection(newDirection);
                    score += 50;
                }
                
                else{
                    lives -= 1;
                if (lives == 0) {
                gameOver = true;
                 return;
                 }
                playDeathAnimation();
                playSound("death.wav", false);
                return;
                }
            }
            if (ghost.y == tileSize*9 && ghost.direction != 'U' && ghost.direction != 'D') {
                ghost.updateDirection('U');
            }
            ghost.x += ghost.velocityX;
            ghost.y += ghost.velocityY;
            for (Block wall : walls) {
                if (collision(ghost, wall) || ghost.x <= 0 || ghost.x + ghost.width >= boardWidth) {
                    ghost.x -= ghost.velocityX;
                    ghost.y -= ghost.velocityY;
                    char newDirection = directions[random.nextInt(4)];
                    ghost.updateDirection(newDirection);
                }
            }
        }

        //check food collision
        Block foodEaten = null;
        for (Block food : foods) {
            if (collision(pacman, food)) {
                playSound("eat_dot.wav", false);
                if (food.image == cherryImage) {
                    isCherryActive = true;
                    cherryTimer = CHERRY_DURATION;

                    areGhostsScared = true;
                    scaredTimer = SCARED_DURATION;
                    areGhostsFlashing = false;
                    ghostFlashCounter = 0;
                    showFlashingGhosts = true;

                    break;
            }
                foodEaten = food;
                score += 10; 
            }
        }


    if (foodEaten != null) {
        foods.remove(foodEaten); // Remove the food after Pac-Man eats it
    }

        if (foods.isEmpty()) {
            loadMap();
            resetPositions();
        }

        if (cherryTimer > 0) {
            cherryTimer--;
        
            if (cherryTimer <= CHERRY_FLASH_START) {
                isCherryFlashing = true;
                cherryFlashCounter++;
                if (cherryFlashCounter % 5 == 0) { // toggle every few frames
                    showCherry = !showCherry;
                }
            }
        
            if (cherryTimer <= 0 && activeCherry != null) {
                foods.remove(activeCherry); // Remove cherry from food list
                activeCherry = null;
                isCherryFlashing = false;
                showCherry = true;
            }
        }

         if (cherrySpawnTimer <= 0) {
            spawnCherry();
          cherrySpawnTimer = CHERRY_SPAWN_INTERVAL;
}
 
}

private void spawnCherry() {
    for (Block food : foods) {
        if (food.image == null && random.nextInt(100) < 10) {
            food.image = cherryImage;
            activeCherry = food;
            cherryTimer = CHERRY_FLASH_START + CHERRY_FLASH_DURATION;
            isCherryFlashing = false;
            showCherry = true;
            cherryFlashCounter = 0;
            return;
        }
    }
}
    public boolean collision(Block a, Block b) {
        return  a.x < b.x + b.width &&
                a.x + a.width > b.x &&
                a.y < b.y + b.height &&
                a.y + a.height > b.y;
    }

    public void resetPositions() {
        pacman.reset();
        pacman.velocityX = 0;
        pacman.velocityY = 0;
        for (Block ghost : ghosts) {
            ghost.reset();
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
    }

    public void updatePacmanImage() {
        switch (pacman.direction) {
            case 'U': pacman.image = pacmanUpImage; break;
            case 'D': pacman.image = pacmanDownImage; break;
            case 'L': pacman.image = pacmanLeftImage; break;
            case 'R': pacman.image = pacmanRightImage; break;
        }
    }

    public boolean canMove(Block block, char direction) {
        int testX = block.x;
        int testY = block.y;
    
        int speed = tileSize / 4; // Same as your current speed
    
        switch (direction) {
            case 'U': testY -= speed; break;
            case 'D': testY += speed; break;
            case 'L': testX -= speed; break;
            case 'R': testX += speed; break;
        }
    
        // Check collision with walls
        for (Block wall : walls) {
            if (testX < wall.x + wall.width &&
                testX + block.width > wall.x &&
                testY < wall.y + wall.height &&
                testY + block.height > wall.y) {
                return false; // Blocked by wall
            }
        }
    
        return true; // No collision, move is valid
    }

    private void spawnNewCherry() {
        int emptyTileAttempts = 100;
    
        while (emptyTileAttempts-- > 0) {
            int r = random.nextInt(rowCount);
            int c = random.nextInt(columnCount);
    
            if (tileMap[r].charAt(c) == ' ') {
                int x = c * tileSize;
                int y = r * tileSize;
    
                // Make sure no wall or cherry is already here
                boolean isOccupied = false;
                for (Block wall : walls) {
                    if (wall.x == x && wall.y == y) {
                        isOccupied = true;
                        break;
                    }
                }
                for (Block cherry : cherries) {
                    if (cherry.x == x && cherry.y == y) {
                        isOccupied = true;
                        break;
                    }
                }
    
                if (!isOccupied) {
                    Block newCherry = new Block(cherryImage, x, y, tileSize, tileSize);
                    cherries.add(newCherry);
                    break;
                }
            }
        }
    }
    

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
        if (gameOver) {
            gameLoop.stop();
        }

        HashSet<Block> eatenCherries = new HashSet<>();
          for (Block cherry : cherries) {
          if (collision(pacman, cherry)) {
            playSound("eat_cherry.wav",false);
            playSound("ghost_scared", false);
           eatenCherries.add(cherry);
           isCherryActive = true;
           areGhostsScared = true;
           scaredTimer = SCARED_DURATION;
           areGhostsFlashing = false;
           ghostFlashCounter = 0;
    }
}
cherries.removeAll(eatenCherries);

if (!cherries.isEmpty()) {
    cherryTimer++;
    if (cherryTimer >= CHERRY_FLASH_START) {
        isCherryFlashing = true;
        cherryFlashCounter++;
        showCherry = (cherryFlashCounter / 10) % 2 == 0;

        if (cherryFlashCounter >= CHERRY_FLASH_DURATION) {
            cherries.clear(); // Despawn cherries
            isCherryFlashing = false;
            cherryTimer = 0;
            cherryFlashCounter = 0;
        }
    }
} else {
    cherrySpawnTimer--;
    if (cherrySpawnTimer <= 0) {
        spawnNewCherry(); 
        cherrySpawnTimer = CHERRY_SPAWN_INTERVAL;
    }
}

   

        if (areGhostsScared) {
            scaredTimer--;
            
            if (scaredTimer <= 20 && !areGhostsFlashing) {  // 1s left
                areGhostsFlashing = true;
                ghostFlashCounter = FLASH_DURATION;
            }
        
            if (areGhostsFlashing) {
                ghostFlashCounter--;
                if (ghostFlashCounter % 5 == 0) { // toggle every 250ms
                    showFlashingGhosts = !showFlashingGhosts;
                }
                if (ghostFlashCounter <= 0) {
                    areGhostsScared = false;
                    areGhostsFlashing = false;
                    showFlashingGhosts = true;
                }
            }
        }

    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameOver) {
            loadMap();
            resetPositions();
            lives = 3;
            score = 0;
            gameOver = false;
            gameLoop.start();
            cherries.clear();
            cherryTimer = 0;
            cherryFlashCounter = 0;
            cherrySpawnTimer = CHERRY_SPAWN_INTERVAL;
        }
        // System.out.println("KeyEvent: " + e.getKeyCode());
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            nextDirection = 'U';
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            nextDirection = 'D';
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            nextDirection = 'L';
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            nextDirection = 'R';
        }
    }
}