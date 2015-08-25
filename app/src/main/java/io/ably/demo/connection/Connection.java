package io.ably.demo.connection;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import io.ably.demo.fragments.ChatFragment;
import io.ably.realtime.AblyRealtime;
import io.ably.realtime.Channel;
import io.ably.realtime.CompletionListener;
import io.ably.realtime.ConnectionStateListener;
import io.ably.realtime.Presence;
import io.ably.types.AblyException;
import io.ably.types.ClientOptions;
import io.ably.types.ErrorInfo;
import io.ably.types.Message;
import io.ably.types.PaginatedResult;
import io.ably.types.Param;
import io.ably.types.PresenceMessage;

public class Connection {
    //no message system needed for a simple reference between the files
    public ChatFragment.ChatScreenAdapter adapterReference;

    private final int historyLimit = 50;
    private final String authURL = "https://www.ably.io/ably-auth/token-request/demos";
    private final String ABLY_CHANNEL_NAME = "mobile:chat";
    private Channel sessionChannel;
    public String userName;
    private AblyRealtime ablyRealtime;

    private Connection() {
    }

    private static Connection instance = new Connection();

    public static Connection getInstance() {
        return instance;
    }

    public void establishConnectionForID(String userName) throws AblyException {
        this.userName = userName;
        //connecting with proper thingies
        ClientOptions clientOptions = new ClientOptions();
        //clientOptions.clientId = userID;
        //clientOptions.key = "xVLyHw.thZlGw:YMexFEbld2BPP_hK";
        //channel with persistance and history
        clientOptions.key = "UtITiw.ji1DsQ:sdl6lgqkJ7AQu0ow";
        //clientOptions.key = "I2E_JQ.1QRmxw:ftN1OHLeV4k9EEtQ";
        //clientOptions.authUrl = authURL;
        clientOptions.logLevel = io.ably.util.Log.VERBOSE;
        ablyRealtime = new AblyRealtime(clientOptions);
        ablyRealtime.connection.on(stateListener);
    }

    private void joinChannel(AblyRealtime ablyInstance) throws AblyException {
        //getting the channel instance and subscribing for messaging
        sessionChannel = ablyInstance.channels.get(ABLY_CHANNEL_NAME);
        sessionChannel.attach();
        sessionChannel.subscribe(messageListener);

        Presence presence = sessionChannel.presence;
        presence.subscribe(presenceListener);
        presence.enter(userName, new CompletionListener() {
            @Override
            public void onSuccess() {
                Log.d("PresenceSetting", "User successfully entered!!!");
            }

            @Override
            public void onError(ErrorInfo errorInfo) {
                Log.d("PresenceSetting", "Error on presence entering!!!");
            }
        });
    }

    private Presence.PresenceListener presenceListener = new Presence.PresenceListener() {
        @Override
        public void onPresenceMessage(PresenceMessage[] presenceMessages) {
            //presence in channel changed
            adapterReference.addUsers(presenceMessages);
        }
    };

    public void getHistory(String direction, int limit) throws AblyException {
        getPresenceHistory(direction, limit);
        getMessagesHistory(direction, limit);
    }

    public void getPresenceHistory(String direction, int limit) throws AblyException {
        Param param1 = new Param("limit", String.valueOf(limit));
        Param param2 = new Param("direction", direction);
        Param[] params = {param1, param2};
        PaginatedResult<PresenceMessage> messages = sessionChannel.presence.history(params);
        adapterReference.addUsers(messages.items());
    }

    public void getMessagesHistory(String direction, int limit) throws AblyException {
        Param param1 = new Param("limit", String.valueOf(limit));
        Param param2 = new Param("direction", direction);
        Param[] params = {param1, param2};
        PaginatedResult<Message> messages = sessionChannel.history(params);
        adapterReference.addMessages(messages.items());
    }

    public void sendMessage(String message) throws AblyException {
        sessionChannel.publish(userName, message, new CompletionListener() {
            @Override
            public void onSuccess() {
                Log.d("MessageSending", "Message sent!!!");
            }

            @Override
            public void onError(ErrorInfo errorInfo) {
                Log.e("MessageSending", errorInfo.message);
            }
        });
    }

    private Channel.MessageListener messageListener = new Channel.MessageListener() {
        @Override
        public void onMessage(Message[] messages) {
            adapterReference.addMessages(messages);
        }
    };

    private ConnectionStateListener stateListener = new ConnectionStateListener() {
        @Override
        public void onConnectionStateChanged(ConnectionStateChange connectionStateChange) {
            //code for handling connection state changes
            switch (connectionStateChange.current) {
                case closed:
                    break;
                case initialized:
                    break;
                case connecting:
                    break;
                case connected:
                    try {
                        joinChannel(ablyRealtime);
                        getHistory("backwards", 50);
                    } catch (AblyException e) {
                        e.printStackTrace();
                    }
                    break;
                case disconnected:
                    break;
                case suspended:
                    break;
                case closing:
                    break;
                case failed:
                    break;
            }//end of switch
        }
    };
}
