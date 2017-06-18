//TODO: standardize speed var, IRC action bug, txt scrolling

package org.chernovia.twitch.following;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

import org.chernovia.twitch.ChatBot;
import org.chernovia.twitch.ChatListener;
import org.chernovia.twitch.gson.Follower;

import java.io.*;
import java.util.*;
import javax.sound.sampled.*;

public class TwitchBox extends JFrame implements Runnable, ActionListener, 
FollowerListener, ChatListener {
	private static final long serialVersionUID = 1L;
	public static final ClassLoader loader = TwitchBox.class.getClassLoader();
	public static final int STYLES = 7, STYLE_ACID = 0, STYLE_MELT = 1, 
	STYLE_GLITTER = 2, STYLE_LIFE = 3, STYLE_SCAT = 4, STYLE_MORPH = 5, STYLE_CHAT = 6;
	public static final String[] StyleStrings = { 
		"Acid","Melt","Glitter","Life","Scatter","Morph","Chat"
	};
	float acid_var = 9; long anim_spd = 10;
	ScreenCan canvas; boolean RUNNING = false, TWITCHING = false;
	JMenuBar mainBar; boolean MENU = true;
	JMenu twitchMenu; JRadioButtonMenuItem[] styleButtons; ButtonGroup styleGroup;
	JMenuItem exitItem, acidItem, spdItem; 
	JCheckBoxMenuItem screenChk, soundChk, borderChk;
	int STYLE = STYLE_CHAT;
	BufferedImage boxView,screenCap;
	int posX, posY;
	FollowerBot followBot; String lastFollow = "Zippy the Pinhead";
	Image followImg = null, logoImg = null;
	BufferedImage followBuff;
	ChatBot chatBot; Vector<TwitchMessage> chatLog; 
	Hashtable<String,Color> chatters; int CHAT_WIDTH = 36; boolean UPDATE_CHAT = true;
	Clip followClip, chatClip; boolean CHAT_SOUNDS = false; boolean BORDERS = false;
	
	public static void main(String[] args) {
		TwitchBox box = new TwitchBox(255,255);
		new Thread(box).start();
		new FollowerBot("zugaddict",box).start();
		box.chatBot = new ChatBot(args[0],args[1],args[2],args[3],box);
	}

	public TwitchBox(int width, int height) {
		chatters = new Hashtable<String,Color>();
		chatLog = new Vector<TwitchMessage>();
		setAlwaysOnTop(true);
		setUndecorated(true);
		setSize(width,height);
		
		addMouseListener(new MouseAdapter() {
	        public void mousePressed(MouseEvent e) { posX = e.getX(); posY = e.getY(); }
	        public void mouseClicked(MouseEvent e) {
	        	if (e.getClickCount() == 2) {
	        		//System.out.println("Clicked!");
	        		if (!MENU) { setJMenuBar(mainBar); MENU = true; }
	        		else { setJMenuBar(null); MENU = false; }
	        		repaint();
	        	}
	        }
	    });
	    addMouseMotionListener(new MouseAdapter() {
	        public void mouseDragged(MouseEvent evt) {
	            Rectangle rectangle = getBounds();
	            setBounds(evt.getXOnScreen() - posX, evt.getYOnScreen() - posY, 
	            rectangle.width, rectangle.height);
	        }
	    });
		
		canvas = new ScreenCan(width,height);
		add(canvas);

		mainBar = new JMenuBar();
		twitchMenu = new JMenu("TwitchBox");
		styleButtons = new JRadioButtonMenuItem[STYLES];
		styleGroup = new ButtonGroup();
		for (int i=0;i<STYLES;i++) {
			styleButtons[i] = new JRadioButtonMenuItem(StyleStrings[i]);
			styleGroup.add(styleButtons[i]);
			twitchMenu.add(styleButtons[i]);
			styleButtons[i].setActionCommand(StyleStrings[i]);
			styleButtons[i].addActionListener(this);
		}
		twitchMenu.addSeparator();
		screenChk = new JCheckBoxMenuItem("Screen Capture");
		screenChk.setState(false);
		twitchMenu.add(screenChk);
		soundChk = new JCheckBoxMenuItem("Chat Sounds");
		soundChk.setState(false);
		twitchMenu.add(soundChk);
		borderChk = new JCheckBoxMenuItem("Animation Borders");
		borderChk.setState(false);
		twitchMenu.add(borderChk);
		twitchMenu.addSeparator();
		spdItem = new JMenuItem("Animation Speed");
		spdItem.setActionCommand("speed");
		spdItem.addActionListener(this);
		twitchMenu.add(spdItem);
		acidItem = new JMenuItem("Funkiness");
		acidItem.setActionCommand("acid");
		acidItem.addActionListener(this);
		twitchMenu.add(acidItem);
		exitItem = new JMenuItem("Exit");
		exitItem.setActionCommand("exit");
		exitItem.addActionListener(this);
		twitchMenu.add(exitItem);
		mainBar.add(twitchMenu);
	    if (MENU) setJMenuBar(mainBar); else setJMenuBar(null); 
	    
		logoImg = Toolkit.getDefaultToolkit().createImage(
		"res/panic.jpg").getScaledInstance(getWidth(),getHeight(),Image.SCALE_SMOOTH);
		followImg = Toolkit.getDefaultToolkit().createImage(
		"res/follow.jpg").getScaledInstance(getWidth(),getHeight(),Image.SCALE_SMOOTH);
		followBuff = new BufferedImage(
		getWidth(),getHeight(),BufferedImage.TYPE_INT_RGB);
		followBuff.createGraphics().drawImage(followImg,0,0,null);
	    //if (followImg != null) canvas.img = followImg; //Why is this necessary?!
	    
		File followFile = new File("res/follow.wav");
		File chatFile = new File("res/chat.wav");
		try {
			followClip = AudioSystem.getClip();
			AudioInputStream ais = AudioSystem.getAudioInputStream(followFile);
			followClip.open(ais);
			chatClip = AudioSystem.getClip();
			ais = AudioSystem.getAudioInputStream(chatFile);
			chatClip.open(ais);
		} 
		catch 
		(UnsupportedAudioFileException | IOException | LineUnavailableException e1) {
			e1.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		System.out.println("Running..."); TWITCHING = true;
		while(TWITCHING) {
			RUNNING = true;
			initGraphics(1280,1024,getWidth(),getHeight());
			switch(STYLE) {
				case STYLE_ACID: acidTrip(); break;
				case STYLE_MELT: melt(); break;
				case STYLE_GLITTER: glitter(); break;
				case STYLE_LIFE: life(); break;
				case STYLE_SCAT: scatter(20); break;
				case STYLE_MORPH: morph(logoImg,50); break;
				case STYLE_CHAT: updateChat(); chatLoop(); break;
			}
		}
	}
	
	public void chatLoop() {
		while (RUNNING) {
			try { Thread.sleep(1000); } catch (InterruptedException e) {}
		}
	}
	
	public void updateChat() {
		int w = canvas.getWidth(), h = canvas.getHeight(); 
		int y = 0; Graphics2D g = boxView.createGraphics();

		String text = "";
		for (int i=0;i<(CHAT_WIDTH+2);i++) text += "a"; //maybe space?
		int fontSize = 0; 
		int fontWidth = 0;
		while (fontWidth < w) {
			fontWidth = g.getFontMetrics(
			new Font("Monospaced",Font.PLAIN,++fontSize)).stringWidth(text);
		}
		Font font = new Font("Monospaced",Font.PLAIN,fontSize); //+?!
		int fontHeight = g.getFontMetrics(font).getHeight();
		int columns = h/fontHeight; 
		g.setFont(font);

		if (STYLE == STYLE_CHAT) {
			g.setColor(Color.BLACK); g.fillRect(0,0,w,h);
		}
		for (int i = ((chatLog.size()>columns) ? (chatLog.size()-columns) : 0);
		i<chatLog.size();i++) {
			TwitchMessage msg = chatLog.get(i);
			g.setColor(msg.col);
			if (msg.handle.equals("")) g.drawString("> " + msg.msg, 0, y);
			else g.drawString(msg.handle + ": " + msg.msg, 0, y);
			y += fontHeight;
		}
		canvas.img = boxView; canvas.repaint();
		if (STYLE != STYLE_CHAT) RUNNING = false; //reboot!
	}
	
	public void morph(Image targImg, int frames) {
		int w = getWidth(), h = getHeight(); 
		boxView.setData(screenCap.getRaster());
		canvas.img = boxView; canvas.repaint();
		BufferedImage buffImg = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		buffImg.getGraphics().drawImage(targImg,0,0,null);
		for (int i = 0; i < frames || (frames == 0 && RUNNING); i++) {
			for (int x=0;x<w;x++)
			for (int y=0;y<h;y++) {
				//Color c = new Color(screenCap.getRGB(x, y));
				Color c1 = new Color(boxView.getRGB(x, y));
				Color c2 = new Color(buffImg.getRGB(x, y));
				
				int d = c2.getRed() - c1.getRed();
				int rx = 1; if (d < 0) rx = -1; else if (d == 0) rx = 0;
				
				d = c2.getGreen() - c1.getGreen();
				int gx = 1; if (d < 0) gx = -1; else if (d == 0) gx = 0;
				
				d = c2.getBlue() - c1.getBlue();
				int bx = 1; if (d < 0) bx = -1; else if (d == 0) bx = 0;
				
				boxView.setRGB(x, y, new Color(
				c1.getRed() + rx,c1.getGreen() + gx,c1.getBlue() + bx).getRGB());
			}
			canvas.img = boxView; canvas.repaint();
			try { Thread.sleep(10); } catch (InterruptedException ignore) {}
		}
		canvas.repaint();
		try { Thread.sleep(1000); } catch (InterruptedException ignore) {}
	}
		
	public void glitter() {
		int w = getWidth(), h = getHeight();
		boxView.setData(screenCap.getRaster());
		while (RUNNING) {
			for (int x=0;x<w;x++)
			for (int y=0;y<h;y++) {
				Color c = new Color(screenCap.getRGB(x, y));
				int r = (Math.random() < .5) ? c.getBlue() : c.getGreen();
				int g = (Math.random() < .5) ? c.getBlue() : c.getRed();
				int b = (Math.random() < .5) ? c.getRed() : c.getGreen();
				boxView.setRGB(x, y, new Color(r,g,b).getRGB());
			}
			canvas.img = boxView; canvas.repaint();
			try { Thread.sleep(50); } catch (InterruptedException ignore) {}
		}
	}
	
	public void scatter(double distance) {
		int w = getWidth(), h = getHeight();
		boxView.setData(screenCap.getRaster());
		while (RUNNING) {
			for (int x=0;x<w;x++)
			for (int y=0;y<h;y++) {
				int nx = (int)(Math.random() * getWidth());
				int ny = (int)(Math.random() * getHeight());
				if (Math.hypot(x-nx,y-ny) < distance) {
					boxView.setRGB(x, y,screenCap.getRGB(nx, ny));
					boxView.setRGB(nx, ny,screenCap.getRGB(x, y));
				}
			}
			canvas.img = boxView; canvas.repaint();
			try { Thread.sleep(50); } catch (InterruptedException ignore) {}
			screenCap = boxView;
		}
	}
	
	public void melt() {
		//try { Thread.sleep(1000); } catch (InterruptedException ignore) {}
		int w = getWidth(), h = getHeight();
		Color c = null;
		while (RUNNING) { 
			for (int x=0;x<w;x++)
			for (int y=0;y<h;y++) {
				int ra = 0, ga = 0, ba = 0; 
				for (int i=x-1;i<=x+1;i++)
				for (int j=y-1;j<=y+1;j++) {
					int x2 = i, y2 = j;
					if (x2<0) x2=w-1; if (y2<0) y2=h-1; 
					if (x2>=w) x2=0; if (y2>=h) y2=0;
					c = new Color(screenCap.getRGB(x2,y2));
					ra += c.getRed(); ga += c.getGreen(); ba += c.getBlue();
				}
				int r = (int)(ra/9), g = (int)(ga/9),	b = (int)(ba/9);	
				boxView.setRGB(x, y,new Color(r,g,b).getRGB());
			}
			canvas.img = boxView; canvas.repaint();
			try { Thread.sleep(50); } catch (InterruptedException ignore) {}
			screenCap = boxView;
		}
	}
	
	public void acidTrip() {
		int w = getWidth(), h = getHeight();
		int x2 = 0, y2 = 0;
		while (RUNNING) {
			boolean wrap = !borderChk.getState();
			for (int x = (wrap ? 0 : 1); x < (wrap ? w : w-1); x++)
			for (int y = (wrap ? 0 : 1); y < (wrap ? h : h-1); y++) {
				int v = 0; 
				for (int i=x-1;i<=x+1;i++)
				for (int j=y-1;j<=y+1;j++) {
					x2 = i; y2 = j;
					if (x2<0) x2=w-1; if (y2<0) y2=h-1;
					if (x2>=w) x2=0; if (y2>=h) y2=0;
					v += screenCap.getRGB(x2,y2);
					
				}
				boxView.setRGB(x, y,(int)(v/acid_var));
			}
			canvas.img = boxView; canvas.repaint();
			try { Thread.sleep(anim_spd); } catch (InterruptedException ignore) {}
			boxView.copyData(screenCap.getRaster());
		}
	}
	
	public void life() {
		int w = getWidth(), h = getHeight();
		Color c = null, c2 = null;
		while (RUNNING) { 
			for (int x=0;x<w;x++)
			for (int y=0;y<h;y++) {
				c = new Color(screenCap.getRGB(x,y));
				int ra = 0, ga = 0, ba = 0;
				int nr = 0, ng = 0, nb = 0;
				for (int i=x-1;i<=x+1;i++)
				for (int j=y-1;j<=y+1;j++) 
				if (i!=x || j!=y) {
					int x2 = i, y2 = j;
					if (x2<0) x2=w-1; if (y2<0) y2=h-1; 
					if (x2>=w) x2=0; if (y2>=h) y2=0;
					c2 = new Color(screenCap.getRGB(x2,y2));
					if (c2.getRed() == 255) nr++;
					if (c2.getGreen() == 255) ng++;
					if (c2.getBlue() == 255) nb++;
					ra += c2.getRed(); ga += c2.getGreen(); ba += c2.getBlue();
				}
				
				int r = ra/8; //0; //(int)(Math.random() * 64); //c.getRed()); 
				int g = ga/8; //0; //(int)(Math.random() * 64); //c.getGreen());
				int b = ba/8; //0; //(int)(Math.random() * 64); //c.getBlue());
				
				if (c.getRed()<255 && nr==3) r = 255;
				else if (c.getRed()==255 && (nr==2 || nr==3)) r = 255;
				else if (nr<1 || nr>6) r = 0;
				
				if (c.getGreen()<255 && ng==3) g = 255;
				else if (c.getGreen()==255 && (ng==2 || ng==3)) g = 255; 
				else if (ng<1 || ng>6) g = 0;
				
				if (c.getBlue()<255 && nb==3) b = 255;
				else if (c.getBlue()==255 && (nb==2 || nb==3)) b = 255;
				else if (nb<1 || nb>6) b = 0;
				
				boxView.setRGB(x,y,new Color(r,g,b).getRGB());
			}
			canvas.img = boxView; canvas.repaint();
			try { Thread.sleep(10); } catch (InterruptedException ignore) {}
			boxView.copyData(screenCap.getRaster());
		}
	}

	public void initGraphics(int x, int y, int w, int h) {
		if (screenChk.getState() == true) {
			setVisible(false);
			Robot bot;
			Rectangle R = new Rectangle(new Point(0,0),new Dimension(x,y));
			try { bot = new Robot(); } catch (AWTException augh) { return; }
			Image img = 
			bot.createScreenCapture(R).getScaledInstance(w,h,BufferedImage.SCALE_SMOOTH);
			screenCap = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
			screenCap.getGraphics().drawImage(img,0,0,null);
		}
		else {
			screenCap = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
			screenCap.getGraphics().drawImage(boxView,0,0,null);
		}
		boxView = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		setVisible(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("exit")) System.exit(-1);
		int oldStyle = STYLE;
		for (int i=0;i<STYLES;i++) 
		if (e.getActionCommand().equals(StyleStrings[i])) {
			STYLE = i; if (STYLE != oldStyle) RUNNING = false; return;
		}
		if (e.getActionCommand().equals("acid")) {
			acid_var = floatDialog("Acid Var:","Whee",acid_var);
		}
		if (e.getActionCommand().equals("speed")) {
			anim_spd = (long)floatDialog("Animation Speed:","Whee",anim_spd);
		} 

	}
	
	public float floatDialog(String prompt, String title, float def) {
		Float n; try {
			String s = (String)JOptionPane.showInputDialog(this,
			prompt,title,JOptionPane.PLAIN_MESSAGE,null,null,def);
			if (s != null) n = Float.parseFloat(s); else return def;
		}
		catch (NumberFormatException augh) { return def; }
		return (n.floatValue());
	}

	@Override
	public void newFollower(Follower f) {
		lastFollow = f.user.name; 
		boxView = 
		new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_INT_RGB);
		Graphics2D g = followBuff.createGraphics();
		g.drawImage(followImg,0,0,null);
		g.setColor(Color.GREEN);
		g.setFont(new Font("Arial", Font.BOLD, 22));
		g.drawString(lastFollow,getWidth()/8,getHeight()/2 + 25);
		newMessage(chatBot.getName(),chatBot.getNick(),chatBot.getName(),
		chatBot.getServer(),"*** New Follower *** --> " + lastFollow);
		STYLE = STYLE_CHAT; RUNNING = false; playClip(followClip);
	}

	@Override
	public void newMessage(String channel, String sender, String login,
	String hostname, String message) {
		if (chatters.get(sender) == null) chatters.put(sender, rndPastel());
		
		String currLine = message.trim();
		int wraps = 0;
		int max_chars = CHAT_WIDTH - sender.length() - 1;
		while (currLine.length() > max_chars) {
			int lastSpace = currLine.substring(
			0,Math.min(currLine.length(),max_chars)).lastIndexOf(" ");
			
			String str;
			if (lastSpace == -1) str = currLine.substring(0,max_chars-1);
			else str = currLine.substring(0,lastSpace).trim();
			
			chatLog.add(
			new TwitchMessage((wraps > 0) ? "" : sender,str,chatters.get(sender)));
			
			if (currLine.length() > str.length()) {
				currLine = currLine.substring(str.length()).trim();
			}
			
			max_chars = CHAT_WIDTH - 1; wraps++;
		} 
		chatLog.add(new TwitchMessage((wraps >= 1) ? "" : sender,
		currLine,chatters.get(sender)));
				
		if (STYLE == STYLE_CHAT || UPDATE_CHAT) updateChat();
		if (CHAT_SOUNDS) playClip(chatClip);
		//if (message.equals("follow")) {
		//	Follower f = new Follower(); f.user = new User(); f.user.name = sender;
		//	newFollower(f);
		//}
	}
	
	//TODO: make (at least) one color = 255
	public Color rndPastel() {
		//to get rainbow, pastel colors
		Random random = new Random();
		final float hue = random.nextFloat();
		final float saturation = 0.9f;//1.0 for brilliant, 0.0 for dull
		final float luminance = 1.0f; //1.0 for brighter, 0.0 for black
		return Color.getHSBColor(hue, saturation, luminance);
	}
	
	public void playClip(Clip clip) {
		if (clip.isRunning()) clip.stop();
		clip.setFramePosition(0); 
		//if (!clip.isRunning()) 
		clip.start(); 
	}
	
}

class TwitchMessage {
	String handle, msg; Color col;
	public TwitchMessage(String h, String m, Color c) {
		handle = h;	msg = m; col = c;
	}
}

class ScreenCan extends JPanel {
	private static final long serialVersionUID = 1L;
	int width; int height;
	Image img;
	
	public ScreenCan(int w, int h) {
		width = w; height = h;
		setSize(width,height);
	}
	
	public void paintComponent(Graphics g) {
		if (img != null) g.drawImage(img,0,0,null); 
	}
	
}
