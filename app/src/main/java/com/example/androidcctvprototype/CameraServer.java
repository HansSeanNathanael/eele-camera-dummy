package com.example.androidcctvprototype;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class CameraServer extends WebSocketServer {

    private final Object mutex = new Object();
    private Bitmap image = null;

    public CameraServer(InetSocketAddress inetSocketAddress) {
        super(inetSocketAddress);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {

    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {

    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Log.d("message", message);
        if (message.compareTo("REQUEST_IMAGE") == 0) {
            synchronized (this.mutex) {
                if (this.image == null) {
                    conn.send("");
                    return;
                }

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                this.image.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream .toByteArray();

                try {
                    byteArrayOutputStream.close();
                } catch (IOException ignored) {}

                conn.send(Base64.encode(byteArray, Base64.DEFAULT));
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

    }

    @Override
    public void onStart() {

    }

    public void setImage(Bitmap bitmap) {
        synchronized (this.mutex) {
            if (this.image != null) {
                this.image.recycle();
            }
            this.image = bitmap.copy(bitmap.getConfig(), true);
        }
    }
}
