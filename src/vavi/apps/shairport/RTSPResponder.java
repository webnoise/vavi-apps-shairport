package vavi.apps.shairport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;

import android.util.Base64;
import android.util.Log;


/**
 * An primitive RTSP responder for replying iTunes
 * 
 * @author bencall
 */
public class RTSPResponder extends Thread{

	private Socket socket;					// Connected socket
	private int[] fmtp;
	private byte[] aesiv, aeskey;			// ANNOUNCE request infos
	private AudioServer serv; 				// Audio listener
	byte[] hwAddr;
	private BufferedReader in;
	private static final Pattern completedPacket = Pattern.compile("(.*)\r\n\r\n");

	public RTSPResponder(byte[] hwAddr, Socket socket) throws IOException {
		this.hwAddr = hwAddr;
		this.socket = socket;
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}


	public RTSPResponse handlePacket(RTSPPacket packet){
		// We init the response holder
		RTSPResponse response = new RTSPResponse("RTSP/1.0 200 OK");
		response.append("Audio-Jack-Status", "connected; type=analog");
		response.append("CSeq", packet.valueOfHeader("CSeq"));

		// Apple Challenge-Response field if needed
    	String challenge;
    	if( (challenge = packet.valueOfHeader("Apple-Challenge")) != null){
    		// BASE64 DECODE
    		byte[] decoded = Base64.decode(challenge, Base64.DEFAULT);

    		// IP byte array
    		//byte[] ip = socket.getLocalAddress().getAddress();
    		SocketAddress localAddress = socket.getLocalSocketAddress(); //.getRemoteSocketAddress();
    		    		
    		byte[] ip =  ((InetSocketAddress) localAddress).getAddress().getAddress();
    		
    		ByteArrayOutputStream out = new ByteArrayOutputStream();
    		// Challenge
    		try {
				out.write(decoded);
				// IP-Address
				out.write(ip);
				// HW-Addr
				out.write(hwAddr);

				// Pad to 32 Bytes
				int padLen = 32 - out.size();
				for(int i = 0; i < padLen; ++i) {
					out.write(0x00);
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

    		 
    		// RSA
    		byte[] crypted = this.encryptRSA(out.toByteArray());
    		
    		// Encode64
    		String ret = Base64.encodeToString(crypted, Base64.DEFAULT);
    		
    		// On retire les ==
	        ret = ret.replace("=", "").replace("\r", "").replace("\n", "");

    		// Write
        	response.append("Apple-Response", ret);
    	} 
    	
		// Paquet request
		String REQ = packet.getReq();
        if(REQ.contentEquals("OPTIONS")){
        	// The response field
        	response.append("Public", "ANNOUNCE, SETUP, RECORD, PAUSE, FLUSH, TEARDOWN, OPTIONS, GET_PARAMETER, SET_PARAMETER");

        } else if (REQ.contentEquals("ANNOUNCE")){
        	// Nothing to do here. Juste get the keys and values
        	Pattern p = Pattern.compile("^a=([^:]+):(.+)", Pattern.MULTILINE);
        	Matcher m = p.matcher(packet.getContent());
        	while(m.find()){
        		if(m.group(1).contentEquals("fmtp")){
        			// Parse FMTP as array
        			String[] temp = m.group(2).split(" ");
        			fmtp = new int[temp.length];
        			for (int i = 0; i< temp.length; i++){
        				fmtp[i] = Integer.valueOf(temp[i]);
        			}
        			
        		} else if(m.group(1).contentEquals("rsaaeskey")){
        			aeskey = this.decryptRSA(Base64.decode(m.group(2), Base64.DEFAULT));
        		} else if(m.group(1).contentEquals("aesiv")){
        			aesiv = Base64.decode(m.group(2), Base64.DEFAULT);
        		}
        	}
        	
        } else if (REQ.contentEquals("SETUP")){
        	int controlPort = 0;
        	int timingPort = 0;
        	
        	String value = packet.valueOfHeader("Transport");        	
        	
        	// Control port
        	Pattern p = Pattern.compile(";control_port=(\\d+)");
        	Matcher m = p.matcher(value);
        	if(m.find()){
        		controlPort =  Integer.valueOf(m.group(1));
        	}
        	
        	// Timing port
        	p = Pattern.compile(";timing_port=(\\d+)");
        	m = p.matcher(value);
        	if(m.find()){
        		timingPort =  Integer.valueOf(m.group(1));
        	}
            
        	// Launching audioserver
			serv = new AudioServer(new AudioSession(aesiv, aeskey, fmtp, controlPort, timingPort));

        	response.append("Transport", packet.valueOfHeader("Transport") + ";server_port=" + serv.getServerPort());
        			
        	// ??? Why ???
        	response.append("Session", "DEADBEEF");
        } else if (REQ.contentEquals("RECORD")){
//        	Headers	
//        	Range: ntp=0-
//        	RTP-Info: seq={Note 1};rtptime={Note 2}
//        	Note 1: Initial value for the RTP Sequence Number, random 16 bit value
//        	Note 2: Initial value for the RTP Timestamps, random 32 bit value

        } else if (REQ.contentEquals("FLUSH")){
        	serv.flush();
        
        } else if (REQ.contentEquals("TEARDOWN")){
        	response.append("Connection", "close");
        	
        } else if (REQ.contentEquals("SET_PARAMETER")){
        	// Timing port
        	Pattern p = Pattern.compile("volume: (.+)");
        	Matcher m = p.matcher(packet.getContent());
        	if(m.find()){
                double volume = Math.pow(10.0,0.05*Double.parseDouble(m.group(1)));
                serv.setVolume(65536.0 * volume);
        	}
        	
        } else {
        	Log.d("ShairPort", "REQUEST(" + REQ + "): Not Supported Yet!");
        	Log.d("ShairPort", packet.getRawPacket());
        }
        
    	// We close the response
    	response.finalize();
    	return response;
	}

	/**
	 * Crypts with private key
	 * @param array	data to encrypt
	 * @return encrypted data
	 */
	public byte[] encryptRSA(byte[] array){
		try{

	        // Encrypt
	        Cipher cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding"); 
	        cipher.init(Cipher.ENCRYPT_MODE, ShairPortActivity.pk);
	        return cipher.doFinal(array);

		}catch(Exception e){
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Decrypt with RSA priv key
	 * @param array
	 * @return
	 */
	public byte[] decryptRSA(byte[] array){
		try{
			// La clef RSA

	        // Encrypt
	        Cipher cipher = Cipher.getInstance("RSA/NONE/OAEPPadding"); 
	        cipher.init(Cipher.DECRYPT_MODE, ShairPortActivity.pk);
	        return cipher.doFinal(array);

		}catch(Exception e){
			e.printStackTrace();
		}

		return null;
	}

    /**
     * Thread to listen packets
     */
	public void run() {
		try {
			do {
				Log.d("ShairPort", "listening packets ... ");
				// feed buffer until packet completed
				StringBuffer packet = new StringBuffer();
				int ret = 0;
				do {
					char[] buffer = new char[4096];
					ret = in.read(buffer);
					packet.append(new String(buffer));
				} while (ret!=-1 && !completedPacket.matcher(packet.toString()).find());
				
				if (ret!=-1) {
					// We handle the packet
					RTSPPacket request = new RTSPPacket(packet.toString());
					RTSPResponse response = this.handlePacket(request);		
					Log.d("ShairPort", request.toString());	
					Log.d("ShairPort", response.toString());
		
			    	// Write the response to the wire
			    	try {			
			    		BufferedWriter oStream = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			    		oStream.write(response.getRawPacket());
			    		oStream.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
		    		if("TEARDOWN".equals(request.getReq())){
		    			socket.close();
		    			socket = null;
		    		}
				} else {
	    			socket.close();
	    			socket = null;
				}
			} while (socket!=null);
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} finally {
			try {
				if (in!=null) in.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (socket!=null) socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		Log.d("ShairPort", "connection ended.");
	}
		
}