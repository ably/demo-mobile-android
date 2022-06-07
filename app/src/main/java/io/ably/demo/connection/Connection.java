package io.ably.demo.connection;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.ably.lib.realtime.AblyRealtime;
import io.ably.lib.realtime.Channel;
import io.ably.lib.realtime.CompletionListener;
import io.ably.lib.realtime.ConnectionState;
import io.ably.lib.realtime.ConnectionStateListener;
import io.ably.lib.realtime.Presence;
import io.ably.lib.types.AblyException;
import io.ably.lib.types.ClientOptions;
import io.ably.lib.types.ErrorInfo;
import io.ably.lib.types.Message;
import io.ably.lib.types.PaginatedResult;
import io.ably.lib.types.Param;
import io.ably.lib.types.PresenceMessage;


public class Connection {

    private static Connection instance = new Connection();
    private final String ABLY_CHANNEL_NAME = "mobile:chat";
    private final String HISTORY_DIRECTION = "backwards";
    private final String HISTORY_LIMIT = "50";
    public String userName;
    private Channel sessionChannel;
    private AblyRealtime ablyRealtime;
    private Channel.MessageListener messageListener;
    private Presence.PresenceListener presenceListener;

    private Connection() {
    }

    public static Connection getInstance() {
        return instance;
    }

    public void establishConnectionForID(String userName, final ConnectionCallback callback) throws AblyException {
        this.userName = userName;

        ClientOptions clientOptions = new ClientOptions();

        clientOptions.authUrl = "https://www.ably.io/ably-auth/token-request/demos";
        clientOptions.logLevel = io.ably.lib.util.Log.VERBOSE;
        clientOptions.clientId = userName;

        ablyRealtime = new AblyRealtime(clientOptions);

        startConnection(ablyRealtime, callback);
    }

    public void establishConnectionForKey(String userName, String key, final ConnectionCallback callback) throws AblyException {
        this.userName = userName;

        ClientOptions clientOptions = new ClientOptions(key);
        clientOptions.clientId = userName;
        ablyRealtime = new AblyRealtime(clientOptions);

        startConnection(ablyRealtime, callback);
    }

    private void startConnection(AblyRealtime ablyRealtime, ConnectionCallback callback) {
        ablyRealtime.connection.on(new ConnectionStateListener() {
            @Override
            public void onConnectionStateChanged(ConnectionStateChange connectionStateChange) {
                switch (connectionStateChange.current) {
                    case closed:
                        Log.d("ConnectionStateChanged", "closed");
                        break;
                    case initialized:
                        Log.d("ConnectionStateChanged", "initialized");
                        break;
                    case connecting:
                        Log.d("ConnectionStateChanged", "connecting");
                        break;
                    case connected:
                        Log.d("ConnectionStateChanged", "connected");
                        sessionChannel = ablyRealtime.channels.get(ABLY_CHANNEL_NAME);

                        try {
                            sessionChannel.attach();
                            callback.onConnectionCallback(null);
                        } catch (AblyException e) {
                            e.printStackTrace();
                            callback.onConnectionCallback(e);
                            Log.e("ChannelAttach", "Something went wrong!");
                            return;
                        }
                        break;
                    case disconnected:
                        Log.d("ConnectionStateChanged", "disconnected");
                        callback.onConnectionCallback(new Exception("Ably connection was disconnected. We will retry connecting again in 30 seconds."));
                        break;
                    case suspended:
                        Log.d("ConnectionStateChanged", "suspended");
                        callback.onConnectionCallback(new Exception("Ably connection was suspended. We will retry connecting again in 60 seconds."));
                        break;
                    case closing:
                        Log.d("ConnectionStateChanged", "closing");
                        sessionChannel.unsubscribe(messageListener);
                        sessionChannel.presence.unsubscribe(presenceListener);
                        break;
                    case failed:
                        Log.d("ConnectionStateChanged", "failed");
                        callback.onConnectionCallback(new Exception("We're sorry, Ably connection failed. Please restart the app."));
                        break;
                }
            }
        });
    }

    public PresenceMessage[] getPresentUsers() {
        try {
            return sessionChannel.presence.get();
        } catch (AblyException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void getPresenceHistory(final PresenceHistoryRetrievedCallback callback) throws AblyException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            @Override
            public void run() {
                List<PresenceMessage> result = new ArrayList<>();
                try {
                    Param limitParameter = new Param("limit", HISTORY_LIMIT);
                    Param directionParameter = new Param("direction", HISTORY_DIRECTION);
                    Param untilAttachParameter = new Param("untilAttach", "true");
                    Param[] presenceHistoryParams = {limitParameter, directionParameter, untilAttachParameter};
                    PaginatedResult<PresenceMessage> messages = sessionChannel.presence.history(presenceHistoryParams);
                    result.addAll(Arrays.asList(messages.items()));
                } catch (AblyException e) {
                    e.printStackTrace();
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!result.isEmpty()) {
                            callback.onPresenceHistoryRetrieved(result);
                        }
                    }
                });
            }
        });
    }

    public void getMessagesHistory(final MessageHistoryRetrievedCallback callback) throws AblyException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            @Override
            public void run() {
                List<Message> result = new ArrayList<>();
                try {
                    Param limitParameter = new Param("limit", HISTORY_LIMIT);
                    Param directionParameter = new Param("direction", HISTORY_DIRECTION);
                    Param untilAttachParameter = new Param("untilAttach", "true");
                    Param[] historyCallParams = {limitParameter, directionParameter, untilAttachParameter};
                    PaginatedResult<Message> messages = sessionChannel.history(historyCallParams);
                    result.addAll(Arrays.asList(messages.items()));
                } catch (AblyException e) {
                    e.printStackTrace();
                    callback.onMessageHistoryRetrieved(Arrays.asList(new Message[]{}), e);
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!result.isEmpty()) {
                            callback.onMessageHistoryRetrieved(result, null);
                        }
                    }
                });
            }
        });
    }

    public void init(Channel.MessageListener listener, Presence.PresenceListener presenceListener, final ConnectionCallback callback) throws AblyException {
        sessionChannel.subscribe(listener);
        messageListener = listener;
        sessionChannel.presence.subscribe(presenceListener);
        this.presenceListener = presenceListener;
        sessionChannel.presence.enter(null, new CompletionListener() {
            @Override
            public void onSuccess() {
                callback.onConnectionCallback(null);
            }

            @Override
            public void onError(ErrorInfo errorInfo) {
                callback.onConnectionCallback(new Exception(errorInfo.message));
                Log.e("PresenceRegistration", errorInfo.message);
            }
        });
    }

    public void sendMessage(String message, final ConnectionCallback callback) throws AblyException {
        sessionChannel.publish(userName, message, new CompletionListener() {
            @Override
            public void onSuccess() {
                callback.onConnectionCallback(null);
                Log.d("MessageSending", "Message sent!!!");
            }

            @Override
            public void onError(ErrorInfo errorInfo) {
                callback.onConnectionCallback(new Exception(errorInfo.message));
            }
        });
    }

    public void reconnectAbly() {
        if (ablyRealtime != null) {
            ablyRealtime.connection.connect();
        }
    }

    public void disconnectAbly() {
        if (ablyRealtime != null) {
            ablyRealtime.close();
        }
    }

    public void userHasStartedTyping(final ConnectionCallback callback) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("isTyping", true);

            sessionChannel.presence.update(payload, new CompletionListener() {
                @Override
                public void onSuccess() {
                    callback.onConnectionCallback(null);
                }

                @Override
                public void onError(ErrorInfo errorInfo) {
                    callback.onConnectionCallback(new Exception(errorInfo.message));
                }
            });
        } catch (AblyException e) {
            e.printStackTrace();
        }
    }

    public void userHasEndedTyping() {
        if (this.ablyRealtime.connection.state != ConnectionState.connected) {
            return;
        }

        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("isTyping", false);
            sessionChannel.presence.update(payload, null);
        } catch (AblyException e) {
            e.printStackTrace();
        }
    }

    private static final String ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm";

    private static String getRandomString(final int sizeOfRandomString) {
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }
}