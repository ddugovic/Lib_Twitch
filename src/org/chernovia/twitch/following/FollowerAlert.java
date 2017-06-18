package org.chernovia.twitch.following;

import java.io.*;
import java.net.URL;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;

import org.chernovia.twitch.gson.Follower;
import org.chernovia.twitch.gson.Followers;

import com.google.gson.Gson;

public class FollowerAlert {
	public static List<Follower> getLastFollows(String channel, int n) {
		try {
			URL url = new URL(
			"https://api.twitch.tv/kraken/channels/" + channel + 
			"/follows?limit=" + n + "&sortby=created_at");				
			HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			StringBuffer responseData = new StringBuffer();
			InputStreamReader in = new InputStreamReader((InputStream)con.getContent());
			BufferedReader br = new BufferedReader(in);
			String line;
			do {
			    line = br.readLine();
			    if (line != null) {
			        responseData.append(line);
			    }
			} while (line != null);
			Gson g = new Gson();
			Followers data = 
			g.fromJson(responseData.toString(), Followers.class);
			List<Follower> follows = data.getFollows();
			return follows;
		}
		catch (Exception augh) { augh.printStackTrace(); return null; }
	}
}
