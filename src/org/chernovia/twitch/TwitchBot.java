package org.chernovia.twitch;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Vector;

import org.jibble.pircbot.PircBot;

public abstract class TwitchBot extends PircBot {
	Vector<String> admins; 
	Vector<String> channels;
	public String CR = " <3 ";
	public static int MAX_CHARS = 400;

	public void initIRC(
	String name, String host, String auth, String chan) {
		setName(name); setVerbose(true);
        try { 
        	connect(host,6667,auth);
        }
        catch (Exception augh) { augh.printStackTrace(); }
        System.out.println("Connected?!");
        setMessageDelay(2000);
        admins = new Vector<String>();
        channels = new Vector<String>(); joinChan(chan);
        sendRawLine("CAP REQ :twitch.tv/commands");
	}
		
	public void addAdmin(String admin) { admins.add(admin.toLowerCase()); }
	public void removeAdmin(String admin) { admins.remove(admin.toLowerCase()); }
	public Vector<String> getAdmins() { return admins; }
	public void loadAdmins(String fileName) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(
			new InputStreamReader(new FileInputStream(fileName)));
			String admin;
		    while ((admin = br.readLine()) != null) { addAdmin(admin); }
		} 
		catch (IOException e) { e.printStackTrace(); }
		try { br.close(); } catch (IOException fark) {}
	}
	
	public void joinChan(String chan) { 
		if (!channels.contains(chan)) { channels.add(chan); joinChannel(chan); } 
	}
	public void partChan(String chan) { channels.remove(chan); partChannel(chan); }
	
	public void reply(String chan, String sender, String response, boolean whisper) {
		if (whisper) whisper(sender,response); else tch(response,chan);
	}
	public void tch(String msg) { for (String chan: channels) tch(msg,chan); }
	public void tch(String msg, String chan) {
		if (msg == null || msg.length() < 1) return;
		boolean overflow;
		try { do {
			overflow = msg.length() > MAX_CHARS;
			if (overflow) {
				int lastCR = msg.substring(0,MAX_CHARS).lastIndexOf(CR);
				sendMessage(chan,msg.substring(0,lastCR));
				msg = msg.substring(lastCR,msg.length());
			}
			else sendMessage(chan,msg);
		} while (overflow); }
		catch (Exception augh) { sendMessage(chan,"Augh: " + augh.getMessage()); }
	}

	public void mTell(String target, String msg) {
		String[] tokens = msg.split(CR); for (int i=0;i<tokens.length;i++) tell(target,tokens[i]);
	}
	public void tell(String target, String msg) { whisper(target,msg); } //TODO: ergh
	public void whisper(String target, String msg) {
		sendRawLineViaQueue("PRIVMSG #jtv :/w " + target + " " + msg);
	}
	
	//whisper kludge
	protected void onUnknown(String msg) {
		log("Unknown: " + msg);
		String[] tokens = msg.split(" ");
		if (tokens.length > 1 && tokens[1].equalsIgnoreCase("WHISPER")) {
			String sender = tokens[0].substring(1,tokens[0].indexOf("!"));
			String host = tokens[0].substring(tokens[0].indexOf("!")+1);
			String message = msg.substring(msg.substring(1).indexOf(":")+2);
			newMessage("",sender,tokens[2],host,message,true);
		}
	}
	
	protected void onMessage(String chan, String sender, String login, String hostname, String message) {
		newMessage(chan, sender, login, hostname, message, false);	
	}
	
	private void newMessage(String chan, String sender, 
	String login, String hostname, String message, boolean whisper) {
		//log("New message: " + chan + "," + sender + "," + login + "," + hostname + "," + message);
		String adminStr = getName().toLowerCase() + " ! "; //TODO: WTF
		if (message.toLowerCase().startsWith(adminStr) && 
		message.length() > adminStr.length() && admins.contains(sender.toLowerCase())) {
			String adminMsg = message.substring(adminStr.length());
			StringTokenizer tokens = new StringTokenizer(adminMsg);
			String cmd = tokens.nextToken();
			if (tokens.countTokens() > 1 && cmd.equalsIgnoreCase("spoof")) {
				String from = tokens.nextToken(); String msg = tokens.nextToken(); 
				while (tokens.hasMoreTokens()) msg += " " + tokens.nextToken();
				handleMsg(chan,from,msg,whisper);
			}
			else adminCmd(adminMsg,whisper); 
		}
		else handleMsg(chan,sender,message,whisper);
	}
		
	public abstract void adminCmd(String cmd, boolean whisper);
	public abstract void handleMsg(String chan, String sender, String msg, boolean whisper);
}
