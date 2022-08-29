package org.gtp.cocacolaproject.wifi;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import org.gtp.cocacolaproject.R;

import java.util.List;

@SuppressWarnings("ConstantConditions")
public class WifiConnectActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wifi_connect);

        findViewById(R.id.bt_wifi_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryToConnect();
            }
        });
    }

    private final int NETWORK_TYPE_WEP = 1;
    private final int NETWORK_TYPE_WPA = 2;
    private final int NETWORK_TYPE_OPEN = 3;
    private final int networkType =  NETWORK_TYPE_WPA;

    private void tryToConnect() {

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + WifiConstants.networkSSID + "\"";   // Please note the quotes. String should contain ssid in quotes

        switch (networkType) {
            case NETWORK_TYPE_WEP:
                //Then, for WEP network you need to do this:
                conf.wepKeys[0] = "\"" + WifiConstants.networkPass + "\"";
                conf.wepTxKeyIndex = 0;
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                break;
            case NETWORK_TYPE_WPA:
                //For WPA network you need to add passphrase like this:
                conf.preSharedKey = "\""+ WifiConstants.networkPass +"\"";
                break;
            case NETWORK_TYPE_OPEN:
                //For Open network you need to do this:
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                break;

                default: return;
        }

        //Then, you need to add it to Android wifi manager settings:
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.addNetwork(conf);

        // And finally, you might need to enable it, so Android connects to it:

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals("\"" + WifiConstants.networkSSID + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();
                break;
            }
        }
    }
}
