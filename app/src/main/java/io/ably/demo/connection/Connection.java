package io.ably.demo.connection;

import android.os.AsyncTask;
import android.util.Log;

import io.ably.realtime.AblyRealtime;
import io.ably.realtime.Channel;
import io.ably.realtime.CompletionListener;
import io.ably.realtime.ConnectionStateListener;
import io.ably.realtime.Presence;
import io.ably.types.AblyException;
import io.ably.types.BaseMessage;
import io.ably.types.ClientOptions;
import io.ably.types.ErrorInfo;
import io.ably.types.Message;
import io.ably.types.PaginatedResult;
import io.ably.types.Param;
import io.ably.types.PresenceMessage;


public class Connection {

    private final String ABLY_CHANNEL_NAME = "mobile:chat";
    private final String HISTORY_DIRECTION = "backwards";
    private final String HISTORY_LIMIT = "50";
    private Channel sessionChannel;
    public String userName;
    private AblyRealtime ablyRealtime;

    private Connection() {}

    private static Connection instance = new Connection();

    public static Connection getInstance() {return instance;}

    public void establishConnectionForID(String userName,final ConnectionCallback callback) throws AblyException {
        //setting userName for future channels calls
        this.userName = userName;

        //region setting clientOption
        ClientOptions clientOptions = new ClientOptions();

        //some channel
        //clientOptions.key = "xVLyHw.thZlGw:YMexFEbld2BPP_hK";

        //one more channel
        //clientOptions.key = "I2E_JQ.1QRmxw:ftN1OHLeV4k9EEtQ";

        //channel with history and presence
        clientOptions.key = "UtITiw.ji1DsQ:sdl6lgqkJ7AQu0ow";
        clientOptions.logLevel = io.ably.util.Log.VERBOSE;
        clientOptions.clientId = userName;
        //endregion

        //creating an AblyRealtime instance
        ablyRealtime = new AblyRealtime(clientOptions);
        //setting listener for states, we want to execute the callback only after we are successfully connected
        ablyRealtime.connection.on(new ConnectionStateListener() {
            @Override
            public void onConnectionStateChanged(ConnectionStateChange connectionStateChange) {
                switch (connectionStateChange.current) {
                    case closed:
                        break;
                    case initialized:
                        break;
                    case connecting:
                        break;
                    case connected:
                        //after state "connected" we set the channel field and execute the callback
                        sessionChannel = ablyRealtime.channels.get(ABLY_CHANNEL_NAME);

                        try {
                            sessionChannel.attach();

                            callback.onConnectionCallback();
                        } catch (AblyException e) {
                            e.printStackTrace();
                            Log.e("ChannelAttach","Something went wrong!");
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
        });
    }

    public void getPresenceHistory(final ConnectionCallback callback) throws AblyException {
        AsyncTask getPresenceHistoryTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                Param param1 = new Param("limit",HISTORY_LIMIT);
                Param param2 = new Param("direction",HISTORY_DIRECTION);
                Param[] presenceHistoryParams = {param1,param2};
                try {
                    PaginatedResult<PresenceMessage> messages = sessionChannel.presence.history(presenceHistoryParams);
                    return messages.items();
                } catch (AblyException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object result) {
                if (result != null) {
                    try {
                        callback.onConnectionCallbackWithResult(((BaseMessage[]) result));
                    } catch (AblyException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        getPresenceHistoryTask.execute();

    }

    public void getMessagesHistory(final ConnectionCallback callback) throws AblyException {
        AsyncTask getMessageHistory = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                Param param1 = new Param("limit",HISTORY_LIMIT);
                Param param2 = new Param("direction",HISTORY_DIRECTION);
                Param[] historyCallParams = {param1,param2};
                final PaginatedResult<Message> messages;
                try {
                    messages = sessionChannel.history(historyCallParams);
                    return messages.items();
                } catch (AblyException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object result) {
                if (result != null) {
                    try {
                        callback.onConnectionCallbackWithResult(((BaseMessage[]) result));
                    } catch (AblyException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        getMessageHistory.execute();

    }

    //this registers for presence and sets presence and messages listeners
    public void init(Channel.MessageListener listener, Presence.PresenceListener presenceListener, final ConnectionCallback callback) throws AblyException {
        sessionChannel.subscribe(listener);
        sessionChannel.presence.subscribe(presenceListener);
        sessionChannel.presence.enter(null, new CompletionListener() {
            @Override
            public void onSuccess() {
                try {
                    callback.onConnectionCallback();
                } catch (AblyException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ErrorInfo errorInfo) {
                Log.e("PresenceRegistration","Something Went Wrong!");
            }
        });
    }

    public void sendMessage(String message) throws AblyException {
        sessionChannel.publish(userName,message, new CompletionListener() {
            @Override
            public void onSuccess() {
                Log.d("MessageSending","Message sent!!!");
            }

            @Override
            public void onError(ErrorInfo errorInfo) {
                Log.e("MessageSending",errorInfo.message);
            }
        });
    }

}
