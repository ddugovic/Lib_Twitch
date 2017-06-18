package org.chernovia.twitch;

public interface ChatListener {
	public void newMessage(String channel, String sender, 
	String login, String hostname, String message);
	
}
