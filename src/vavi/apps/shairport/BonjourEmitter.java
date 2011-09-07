package vavi.apps.shairport;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.JmmDNSImpl;
import javax.jmdns.impl.NetworkTopologyEventImpl;


/**
 * Emetteur Bonjour pour qu'iTunes detecte la borne airport
 * Needs Bonjour for Windows (apple.com)
 * @author bencall
 *
 */

//
public class BonjourEmitter {
    JmmDNS dns;
	
	public BonjourEmitter(String name, String identifier, int port) throws IOException{
		    		    
		    // Il faut un serial bidon pour se connecter
		    if (identifier == null){
		    	identifier = "";
		    	for(int i=0; i<6; i++){
		    		identifier = identifier + Integer.toHexString((int) (Math.random()*255)).toUpperCase();
		    	}
		    }

		    // Zeroconf registration
		    ServiceInfo info = ServiceInfo.create(identifier + "@" + name + "._raop._tcp.local", identifier + "@" + name, port, "tp=UDP sm=false sv=false ek=1 et=0,1 cn=0,1 ch=2 ss=16 sr=44100 pw=false vn=3 txtvers=1");

		    dns = JmmDNS.Factory.getInstance();
		    ((JmmDNSImpl)dns).inetAddressAdded(new NetworkTopologyEventImpl(JmDNS.create(InetAddress.getByName("localhost")), InetAddress.getByName("localhost")));

		    try {
		        Thread.sleep(1000); // If this isn't done the Announcement sometimes doesn't go out on the local interface
		    } catch (InterruptedException e) {
		        e.printStackTrace(System.err);
		    }

		    dns.registerService(info);
	}

	/**
	 * Stop service publishing
	 */
	public void stop() throws IOException {
        dns.unregisterAllServices();
		dns.close();
	}
}

