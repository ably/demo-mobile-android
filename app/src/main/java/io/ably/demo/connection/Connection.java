package io.ably.demo.connection;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import io.ably.demo.fragments.ChatFragment;
import io.ably.realtime.AblyRealtime;
import io.ably.realtime.Channel;
import io.ably.realtime.CompletionListener;
import io.ably.realtime.ConnectionStateListener;
import io.ably.types.AblyException;
import io.ably.types.ClientOptions;
import io.ably.types.ErrorInfo;
import io.ably.types.Message;
import io.ably.types.PaginatedResult;
import io.ably.types.Param;


public class Connection {

    //no message system needed for a simple reference between the files
    public ChatFragment.ChatScreenAdapter adapterReference;

    private final int historyLimit = 50;
    private final String authURL = "https://www.ably.io/ably-auth/token-request/demos";
    private final String ABLY_CHANNEL_NAME = "mobile:chat";
    private Channel sessionChannel;
    public String userName;

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
        clientOptions.clientId = userName;
        clientOptions.key = "xVLyHw.qJrb6Q:8GpMViic3JgoqBKG";
        clientOptions.logLevel = 10;
        AblyRealtime ablyRealtime = new AblyRealtime(clientOptions);
        ablyRealtime.connection.on(stateListener);


        sessionChannel = ablyRealtime.channels.get(ABLY_CHANNEL_NAME);
        sessionChannel.attach();
        sessionChannel.subscribe(messageListener);
    }

    public void getHistory(String direction, int limit) throws AblyException {
        Param param1 = new Param("limit", String.valueOf(limit));
        Param param2 = new Param("direction", direction);
        Param[] params = {param1, param2};
        PaginatedResult<Message> messages = sessionChannel.history(params);
        adapterReference.addItems(messages.items());
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
            adapterReference.addItems(messages);
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
