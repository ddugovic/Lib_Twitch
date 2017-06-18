package org.chernovia.twitch;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.jibble.pircbot.PircBot;

public class TwitchMessageQueue extends Thread {
	
	class TwitchMsg implements Delayed {
		String msg; String chan; long delay;
		public TwitchMsg(String c, String m, long d) {
			chan = c; msg = m; delay = d;
		}
		@Override
		public int compareTo(Delayed arg0) { return 0; }
		@Override
		public long getDelay(TimeUnit unit) { return delay; }
	}
	
	long delay;
	PircBot bot;
	DelayQueue<TwitchMsg> MsgQueue;
	
	public TwitchMessageQueue(PircBot b, long d) {
		bot = b; delay = d; MsgQueue = new DelayQueue<TwitchMsg>();
	}
	
	public void addMsg(String chan, String msg, long d) {
		MsgQueue.add(new TwitchMsg(chan,msg,d));
	}
	
	public void run() {
		while (true) {
			try { 
				Thread.sleep(delay); 
				TwitchMsg msg = MsgQueue.take(); 
				if (msg != null) {
					bot.sendMessage(msg.chan, msg.msg);
					System.out.println("Sending message: " + msg.msg);
				}
			} catch (InterruptedException augh) {}
		}
	}
}
