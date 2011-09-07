
package vavi.apps.shairport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

import android.app.Activity;
import android.content.res.Resources;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


/**
 * @author bencall
 */
public class ShairPortActivity extends Activity {

//    static String deviceId;
    static String macAddress;
    static PrivateKey pk;
    
    private boolean on = false;

    private Button bouton;
    private EditText nameField;
    private LaunchThread t;

    WifiManager.MulticastLock lock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DebugUtil.setDebug(this);

        WifiManager wifi = (WifiManager) getSystemService(android.content.Context.WIFI_SERVICE);
        lock = wifi.createMulticastLock("ShairPort");
        lock.setReferenceCounted(true);
        lock.acquire();

        WifiInfo wifiInfo = wifi.getConnectionInfo();
        macAddress = wifiInfo.getMacAddress();
Log.d("ShairPort", "mac addr: " + macAddress);

//        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        try {
            Resources resources = this.getResources();  
            InputStream is = resources.openRawResource(R.raw.key);        
            pk = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(getByteArrayFromStream(is)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

//Log.d("ShairPort", "buf: " + AudioTrack.getMinBufferSize(44100,
//                                                         AudioFormat.CHANNEL_OUT_STEREO,
//                                                         AudioFormat.ENCODING_PCM_16BIT));

        setContentView(R.layout.main);

        nameField = (EditText) findViewById(R.id.name);
        nameField.setText("android");
        bouton = (Button) findViewById(R.id.start);

        bouton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!on) {
                    on = true;

                    t = new LaunchThread(nameField.getText().toString());
                    t.start();
                    bouton.setText("Stop Airport Express");
                } else {
                    on = false;
                    t.stopThread();
                    bouton.setText("Start Airport Express");
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (lock != null) {
            lock.release();
        }
    }

    static byte[] getByteArrayFromStream(InputStream is) throws IOException {
        byte[] b = new byte[10000];
        int read;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((read = is.read(b, 0, b.length)) > 0) {
            out.write(b, 0, read);
        }
        return out.toByteArray();
    }
}
