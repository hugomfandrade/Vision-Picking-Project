package org.gtp.cocacolaproject.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

public final class NetworkUtils {


    /**
     * String used in logging output.
     */
    @SuppressWarnings("unused")
    private static final String TAG = NetworkUtils.class.getSimpleName();

    /**
     * Ensure this class is only used as a utility.
     */
    private NetworkUtils() {
        throw new AssertionError();
    }


    public static String getIpAddress(android.content.Context context) {

        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wm == null) return null;

        return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }

    public static String getLocalIpAddress() {

        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface i = en.nextElement();

                for (Enumeration<InetAddress> enumIpAddr = i.getInetAddresses(); enumIpAddr.hasMoreElements();) {

                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, ex.toString());
        }

        return null;
    }

    public static boolean isPortOpen(final String ip, final int port, final int timeout) {

        try {
            //Socket socket = new Socket(ip, port);
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.close();
            return true;
        }

        catch(ConnectException ce){
            //ce.printStackTrace();
            return false;
        }
        catch (Exception ex) {
            //ex.printStackTrace();
            return false;
        }
    }

    public static boolean isReachable(String host, int timeout) {

        try {
            return InetAddress.getByName(host).isReachable(timeout);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String buildUrl(String localIPAddress, int port) {

        return "ws://" + localIPAddress + ":" + port;
    }

    public static boolean isNetworkAvailable(Context context) {
        try {
            if (context == null) return false;

            final ConnectivityManager connMgr = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connMgr == null)
                return false;

            NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();
            boolean wifiAvailability = false;
            boolean mobileAvailability = false;
            if (activeNetwork != null) { // connected to the internet
                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                    wifiAvailability = true;
                } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                    mobileAvailability = true;
                }
            }

            return wifiAvailability || mobileAvailability;

        } catch (NullPointerException e) {
            return false;
        }
    }
}
