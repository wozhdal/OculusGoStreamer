package net.kajos.Manager;

import net.kajos.AudioRecorder;
import net.kajos.Config;
import net.kajos.Server;
import org.json.JSONArray;
import org.json.JSONObject;
import org.webbitserver.BaseWebSocketHandler;
import org.webbitserver.WebSocketConnection;

import java.awt.*;

public class Manager extends BaseWebSocketHandler {
    private Viewer viewer = null;
    private WebSocketConnection connection;

    private Input input;
    private Server server;

    public Manager(Server server) throws AWTException {
        input = new Input();
        this.server = server;
    }

    public void sendData(byte[] data) {
        if (connection == null) return;

        connection.send(data);
    }

    public void sendEmptyImage(int framestamp) {
        byte[] data = new byte[]{0, 0, 0, 0, 0};
        data[1] = (byte) (framestamp & 0xff);
        framestamp >>= 8;
        data[2] = (byte) (framestamp & 0xff);
        framestamp >>= 8;
        data[3] = (byte) (framestamp & 0xff);
        framestamp >>= 8;
        data[4] = (byte) (framestamp & 0xff);
        sendData(data);
    }

    private void closeConnection(WebSocketConnection conn) {
        if (conn != null) {
            conn.close();
        }
        viewer = null;
        connection = null;
    }

    public void onOpen(WebSocketConnection conn) {
        viewer = new Viewer();
        connection = conn;
        System.out.println("Connection opened");
    }

    public void onClose(WebSocketConnection conn) {
        closeConnection(conn);

        System.out.println("Connection closed");
    }

    public Viewer getViewer() {
        return viewer;
    }

    @Override
    public void onMessage(WebSocketConnection connection, String message) {
        if (viewer == null) return;

        if (message.startsWith(">")) {
            viewer.frameUpdate(Integer.parseInt(message.substring(1)));
        } else {
            JSONObject obj = new JSONObject(message);
            if (obj.has("window")) {
                JSONArray ar = obj.getJSONArray("window");
                viewer.clientWidth = ar.getInt(0);
                viewer.clientHeight = ar.getInt(1);

                viewer.reset();
                server.resize(viewer.clientWidth, viewer.clientHeight);
            }
            if (obj.has("screenSwitch")) {
                int screen = Config.get().SELECTED_SCREEN;
                screen++;
                screen %= Config.get().SCREENS.length;

                System.out.println("Switch to screen: " + screen);

                viewer.reset();
                server.resize(screen, viewer.clientWidth, viewer.clientHeight);
            }
            input.parseInput(obj);
        }
    }
}
