package com.outr.net.communicator.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Timer;
import com.outr.net.communicator.client.connection.ConnectionManager;
import com.outr.net.communicator.client.connection.Message;
import com.outr.net.communicator.client.connection.convert.AJAXResponseConverter;
import com.outr.net.communicator.client.connection.convert.MessageConverter;
import com.outr.net.communicator.client.connection.convert.MessageReceiveFailureConverter;
import com.outr.net.communicator.client.event.Listenable;
import com.outr.net.communicator.client.event.Listener;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Matt Hicks <matt@outr.com>
 */
public class GWTCommunicator implements EntryPoint {
    private JavaScriptObject jsonSettings;
    private Map<String, Object> settings;
    private final ConnectionManager connectionManager;
    public final ErrorDialog error;

    public final Listenable<Message> received = new Listenable<Message>();
    private final Timer updater;

    static {
        JSONConverter.add(new MessageConverter());
        JSONConverter.add(new AJAXResponseConverter());
        JSONConverter.add(new MessageReceiveFailureConverter());
    }

    public String getAJAXURL() {
        return setting("ajaxURL", "/Communicator/connection");
    }

    public int reconnectDelay() {
        return setting("reconnectDelay", 10000);
    }

    public JSONValue createData() {
        return JSONConverter.js2JSON(get(jsonSettings, "createData"));
    }

    public JSONValue connectData() {
        return JSONConverter.js2JSON(get(jsonSettings, "connectData"));
    }

    public GWTCommunicator() {
        connectionManager = new ConnectionManager(this);
        error = new ErrorDialog(this);
        updater = new Timer() {
            private long previous = System.currentTimeMillis();

            public void run() {
                long current = System.currentTimeMillis();
                int delta = (int)(current - previous);
                previous = current;
                update(delta);
            }
        };
    }

    @Override
    public void onModuleLoad() {
        initialize();
        error.init();
        connectionManager.init();
        updater.scheduleRepeating(250);
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                notifyHostPage();
            }
        });
    }

    public void update(int delta) {
        connectionManager.update(delta);
        error.update(delta);
    }

    public <T> T setting(String name, T def) {
        T value = (T)settings.get(name);
        if (value == null) {
            return def;
        } else {
            return value;
        }
    }

    public boolean connect(JavaScriptObject json) {
        try {
            if (connectionManager.hasConnection()) {
                return false;
            }
            jsonSettings = json;
            settings = (Map<String, Object>)JSONConverter.fromJSONValue(new JSONObject(json));
            for (Map.Entry<String, JavaScriptObject> entry : setting("on", new HashMap<String, JavaScriptObject>()).entrySet()) {
                on(entry.getKey(), get(json, "on." + entry.getKey()));
            }

            connectionManager.connect();
            return true;
        } catch(Throwable t) {
            log("Exception thrown: " + t.getMessage());
            return false;
        }
    }

    public void on(final String event, final JavaScriptObject f) {
        received.on(new Listener<Message>() {
            @Override
            public void process(Message message) {
                if (event.equalsIgnoreCase(message.event) || "received".equalsIgnoreCase(event)) {
                    Listenable.call(f, message.content);
                }
            }
        });
    }

    public void send(String event, JavaScriptObject data) {
        Object obj = JSONConverter.fromJavaScriptObject(data);
        connectionManager.send(event, obj);
    }

    public void reload(boolean showReloading, boolean force, int delay) {
        if (showReloading) {
            error.show("Reloading Page", "The page will now be reloaded. If the browser doesn't reload on it's own please press the reload button in your browser.");
        }
        updater.cancel();       // Stop the timer so nothing else happens while we work
        reloadBrowser(force, delay);
    }

    public static native void log(String message) /*-{
        $wnd.console.log(message);
    }-*/;

    public static native void reloadBrowser(boolean force, int delay) /*-{
        var f = function() {
            $wnd.location.reload(force);
        };
        setTimeout(f, delay);
    }-*/;

    private native void initialize() /*-{
        var c = this;
        $wnd.GWTCommunicator = {
            connect: function(settings) {
                c.@com.outr.net.communicator.client.GWTCommunicator::connect(Lcom/google/gwt/core/client/JavaScriptObject;)(settings);
            },
            on: function(event, listener) {
                c.@com.outr.net.communicator.client.GWTCommunicator::on(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(event, listener);
            },
            send: function(event, data) {
                c.@com.outr.net.communicator.client.GWTCommunicator::send(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(event, data);
            }
        };
    }-*/;

    private native void notifyHostPage() /*-{
        $wnd.communicatorReady();
    }-*/;

    public static native JavaScriptObject parse(String s) /*-{
        return JSON.parse(s);
    }-*/;

    public static JavaScriptObject get(JavaScriptObject obj, String lookup) {
        int index = lookup.indexOf('.');
        if (index != -1) {
            String key = lookup.substring(0, index);
            String next = lookup.substring(index + 1);
            return get(getInternal(obj, key), next);
        }
        return getInternal(obj, lookup);
    }

    private static native JavaScriptObject getInternal(JavaScriptObject obj, String lookup) /*-{
        return obj[lookup];
    }-*/;
}
