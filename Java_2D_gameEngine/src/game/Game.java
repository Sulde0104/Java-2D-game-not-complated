package game;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import game.Level.Level;
import game.entities.Player;
import game.entities.PlayerMP;
import game.gfx.Colours;
import game.gfx.Font;
import game.gfx.Screen;
import game.gfx.SpriteSheet;
import game.net.GameClient;
import game.net.GameServer;
import game.net.packets.Packet00Login;

public class Game extends Canvas implements Runnable {

	private static final long serialVersionUID = 1L;
	
	public static final int WIDTH = 455;
	public static final int HEIGHT = WIDTH / 12 * 7;
	public static final int SCALE = 3;
	public static final String NAME = "GAME";
	public static final Dimension DIMENSIONS = new Dimension(WIDTH * SCALE, HEIGHT * SCALE);
	public static Game game;
	public JFrame frame;
	
	private Thread thread;
	
	public boolean running = false;
	public int tickCount = 0;
	
	private BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
	private int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
	private int[] colours = new int[6 * 6 * 6];
	
	private Screen screen;
	public InputHandler input;
	public WindowHandler windowHandler;
	public Level level;
	public Player player;
	
	public GameClient socketClient;
	public GameServer socketServer;

	public boolean debug = true;
	public boolean isApplet = false;
	
	public Game() {
		setMinimumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
		setMaximumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
		setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
		
		frame = new JFrame(NAME);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		
		frame.add(this, BorderLayout.CENTER);
		frame.pack();
		
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

	}
	
	public void init() {
		game = this;
		int index = 0;
		for(int r = 0; r < 6; r++) {
			for(int g = 0; g < 6; g++) {
				for(int b = 0; b < 6; b++) {
					int rr = (r * 255 / 5);
					int gg = (g * 255 / 5);
					int bb = (b * 255 / 5);
					
					colours[index++] = rr << 16 | gg << 8 | bb;
				}
			}
		}
		screen = new Screen(WIDTH, HEIGHT, new SpriteSheet("/sprite_sheet.png"));
		input = new InputHandler(this);
		//windowHandler = new WindowHandler(this);
		level = new Level("/levels/water_test_level.png");
		player = new PlayerMP(level, 100, 100, input, JOptionPane.showInputDialog(this, "Тоглогчийн нэр оруулна уу"), null, -1);
	    level.addEntity(player);
	    if (!isApplet) {
	    	Packet00Login loginPacket = new Packet00Login(player.getUsername());
	    	if (socketServer != null) {
		    	socketServer.addConnection((PlayerMP) player, loginPacket);
		    }
		    loginPacket.writeData(socketClient);
	    } 
	}
	
	private synchronized void start() {
		running = true;
		thread = new Thread(this, NAME + "_main");
		thread.start();
		if (!isApplet) {
			if(JOptionPane.showConfirmDialog(this, "Сервер эхлүүлэх") == 0) {
				socketServer = new GameServer(this);
				socketServer.start();
			}
			
			socketClient = new GameClient(this, "localhost");
			socketClient.start();
		}
	}
	
	private synchronized void stop() {
		running = false;
	}
	
	public void run() {
		long lastTime = System.nanoTime();
		double nsPerTick = 1000000000D/60D;
		
		int ticks = 0;
		int frames = 0;
		
		long lastTimer = System.currentTimeMillis();
		double delta = 0;
		
		init();
		
		while (running) {
			long now = System.nanoTime();
			delta += (now - lastTime)/nsPerTick;
			lastTime = now;
			boolean shouldRender = true;
			
			while (delta >= 1) {
				ticks++;
				tick();
				delta -= 1;
				shouldRender = true;
			}
			
			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if(shouldRender) {
				frames++;
				render();
			}
			
			if (System.currentTimeMillis() - lastTimer >= 1000) {
				lastTimer += 1000;
				frame.setTitle(ticks + " ticks, " + frames + " frames");
				//debug(DebugLevel.INFO, ticks + " ticks, " + frames + " frames");
				frames = 0;
				ticks = 0;
				
			}
		}
	}
	
	public void tick() {
		tickCount++;
		level.tick();
	}
	
	private void render() {
		BufferStrategy bs = getBufferStrategy();
		if (bs==null) {
			createBufferStrategy(3);
			return;
		}
		
		//////// buruu bol if iig ustga
		if (player != null) {
			int xOffset = player.x - (screen.width / 2);
			int yOffset = player.y - (screen.height / 2);
		
			level.renderTiles(screen, xOffset, yOffset);
			level.renderEntities(screen);
		}
		////// 
		
		/*for (int x = 0; x < level.width; x++) {
			int colour = Colours.get(-1, -1, -1, 000);
			if(x % 10 == 0 && x != 0) {
				colour = Colours.get(-1, -1, -1, 500);
			}
		}*/
		
		/*for (int y = 0; y < 32; y++) {
			for (int x = 0; x < 32; x++) {
				boolean flipX = x % 2 == 1;
				boolean flipY = y % 2 == 1;
				screen.render(x << 3, y << 3, 0, Colours.get(555, 505, 055, 550), flipX, flipY);
			}
		}*/
		
		/*
		String msg = "This is my game!";
		Font.render(msg,screen, screen.xOffset + screen.width/2 - ((msg.length()*8)/2), screen.yOffset + screen.height/2, 
			Colours.get(-1, -1, -1, 000));
		*/
		
		for (int y = 0; y < screen.height; y++) {
			for (int x = 0; x < screen.width; x++) {
				int colourCode = screen.pixels[x + y * screen.width];
				if (colourCode < 255) 
					pixels[x + y * WIDTH] = colours[colourCode];
			}
		} 
		
		Graphics g = bs.getDrawGraphics();
		g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
		g.dispose();
		bs.show();
	}
	
	public static void main(String[] args) {
		new Game().start();
	}
	
	public static long fact(int n) {
		if (n <= 1) {
			return 1;
		} else {
			return n * fact(n - 1);
		}
	}
	
}
