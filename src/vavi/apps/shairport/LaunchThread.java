package vavi.apps.shairport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import android.util.Log;


/**
 * LaunchThread class which starts services
 * 
 * @author bencall
 */
public class LaunchThread extends Thread{
	private BonjourEmitter emitter;
	private String name;
	private boolean stopThread = false;
	
	/**
	 * Constructor
	 * @param name
	 */
	public LaunchThread(String name){
		super();
		this.name = name;
	}
	
	private byte[] getHardwareAdress() {
		byte[] hwAddr = null;
		
		InetAddress local;
		try {
			local = InetAddress.getLocalHost();
			NetworkInterface ni = NetworkInterface.getByInetAddress(local);
			if (ni != null) {
                String[] as = ShairPortActivity.macAddress.split(":");
                hwAddr = new byte[as.length];
                int i = 0;
                for (String a : as) {
                    hwAddr[i++] = Integer.valueOf(a, 16).byteValue();
                }
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return hwAddr;
	}
	
	
	private String getStringHardwareAdress(byte[] hwAddr) {
	    StringBuilder sb = new StringBuilder();
	    for (byte b : hwAddr) {
	      sb.append(String.format("%02x", b));
	    }
Log.d("ShairPort", sb.toString());
	    return sb.toString();
	}
	
	
	public void run(){
		Log.d("ShairPort", "service started.");
		int port = 5001;
		
		ServerSocket servSock = null;
		try {
			// We listen for new connections
			try {
				servSock = new ServerSocket(port);
			} catch (IOException e) {
				servSock = new ServerSocket();
			}

			// DNS Emitter (Bonjour)
			byte[] hwAddr = getHardwareAdress();
			emitter = new BonjourEmitter(name, getStringHardwareAdress(hwAddr), port);
			
			servSock.setSoTimeout(1000);
			while (!stopThread) {
				try {
					Socket socket = servSock.accept();
					Log.d("ShairPort", "got connection from " + socket.toString());
					new RTSPResponder(hwAddr, socket).start();
				} catch(SocketTimeoutException e) {
					// ignore
				}
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
			
		} finally {
			try {
				servSock.close(); // will stop all RTSPResponders.
				emitter.stop(); 
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Log.d("ShairPort", "service stopped");
	}
	
	public synchronized void stopThread(){
		stopThread = true;
	}
}
