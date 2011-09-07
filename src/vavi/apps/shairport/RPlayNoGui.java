package vavi.apps.shairport;

import android.util.Log;

public class RPlayNoGui {
	
	
	private static final void usage() {
		Log.d("ShairPort", "Java port of shairport.");
		Log.d("ShairPort", "usage : ");
		Log.d("ShairPort", "     java "+RPlayNoGui.class.getCanonicalName()+" <AP_name>");
	}

	public static void main(String[] args) {
		if (args.length != 1 && args[1].length()>1) {
			usage();
			System.exit(-1);
		}
		new LaunchThread(args[0]).start();
	}
	
}
