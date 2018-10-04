package io.particle.android.sdk.devicesetup.commands;

import android.annotation.TargetApi;
import android.net.Network;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

import io.particle.android.sdk.utils.SSID;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.WifiFacade;

/**
 * Factory for Sockets which binds communication to a particular {@link android.net.Network}
 */
public class NetworkBindingSocketFactory extends SocketFactory {

    private static final TLog log = TLog.get(NetworkBindingSocketFactory.class);

    private final WifiFacade wifiFacade;
    private final SSID softAPSSID;
    // used as connection timeout and read timeout
    private final int timeoutMillis;

    public NetworkBindingSocketFactory(WifiFacade wifiFacade, SSID softAPSSID, int timeoutMillis) {
        this.wifiFacade = wifiFacade;
        this.softAPSSID = softAPSSID;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public Socket createSocket() throws IOException {
        return buildSocket();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        Socket socket = buildSocket();
        socket.connect(new InetSocketAddress(host, port), timeoutMillis);
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
            throws IOException, UnknownHostException {
        throw new UnsupportedOperationException(
                "Specifying a localHost or localPort arg is not supported.");
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        Socket socket = buildSocket();
        socket.connect(new InetSocketAddress(host, port), timeoutMillis);
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress,
                               int localPort) throws IOException {
        throw new UnsupportedOperationException(
                "Specifying a localHost or localPort arg is not supported.");
    }


    private Socket buildSocket() throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(timeoutMillis);

        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            bindSocketToSoftAp(socket);
        }

        return socket;
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    private void bindSocketToSoftAp(Socket socket) throws IOException {
        Network softAp = wifiFacade.getNetworkObjectForCurrentWifiConnection();

        if (softAp == null) {
            // If this ever fails, fail VERY LOUDLY to make sure we hear about it...
            // FIXME: report this error via analytics
            throw new SocketBindingException("Could not find Network for SSID " + softAPSSID);
        }

        softAp.bindSocket(socket);
    }


    private static class SocketBindingException extends IOException {

        SocketBindingException(String msg) {
            super(msg);
        }
    }

}
