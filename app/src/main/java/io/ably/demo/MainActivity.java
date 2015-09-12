package io.ably.demo;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import io.ably.demo.connection.Connection;
import io.ably.demo.connection.ConnectionCallback;
import io.ably.realtime.Channel;
import io.ably.realtime.Presence;
import io.ably.types.AblyException;
import io.ably.types.BaseMessage;
import io.ably.types.Message;
import io.ably.types.PresenceMessage;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    ChatScreenAdapter adapter;
    private boolean activityPaused = false;
    private Handler isUserTypingHandler = new Handler();
    private boolean typingFlag = false;
    private ArrayList<String> usersCurrentlyTyping = new ArrayList();

    private Runnable isUserTypingRunnable = new Runnable() {
        @Override
        public void run() {
            //Toast.makeText(getApplicationContext(), "User has stopped writing", Toast.LENGTH_SHORT).show();
            Connection.getInstance().userHasEndedTyping();
            typingFlag = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.joinBtn).setOnClickListener(this);
        findViewById(R.id.sendBtn).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (activityPaused)
        {
            Connection.getInstance().reconnectAbly();
            activityPaused = false;
        }
    }

    private void showChatScreen(){
        findViewById(R.id.loginLayout).setVisibility(View.GONE);

        adapter = new ChatScreenAdapter();
        ((ListView) findViewById(R.id.chatList)).setAdapter(adapter);
        try {
            Connection.getInstance().init(messageListener,presenceListener,chatInitializedCallback);
        } catch (AblyException e) {
            e.printStackTrace();
        }
    }

    private ConnectionCallback connectionCallback = new ConnectionCallback() {
        @Override
        public void onConnectionCallback() throws AblyException {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showChatScreen();
                }
            });
        }

        @Override
        public void onConnectionCallbackWithResult(BaseMessage[] result) throws AblyException {

        }
    };


    private TextWatcher isUserTypingTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (!typingFlag)
            {
                //Toast.makeText(getApplicationContext(),"User strted typing",Toast.LENGTH_SHORT).show();
                Connection.getInstance().userHasStartedTyping();
                typingFlag = true;
            }
            isUserTypingHandler.removeCallbacks(isUserTypingRunnable);
            isUserTypingHandler.postDelayed(isUserTypingRunnable, 5000);
        }
    };

    private ConnectionCallback chatInitializedCallback = new ConnectionCallback() {
        @Override
        public void onConnectionCallback() throws AblyException {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                    findViewById(R.id.chatLayout).setVisibility(View.VISIBLE);
                    ((EditText) findViewById(R.id.textET)).addTextChangedListener(isUserTypingTextWatcher);
                }
            });

        }

        @Override
        public void onConnectionCallbackWithResult(BaseMessage[] result) throws AblyException {

        }
    };

    @Override
    public void onClick(View v) {
        //showing the chat scree
        if (v.getId() == R.id.joinBtn) {
            findViewById(R.id.loginLayout).setVisibility(View.GONE);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            //establishing connection with ably service
            try {
                String clientId = ((TextView) findViewById(R.id.usernameET)).getText().toString();

                //execute fragment transaction only after successful connection
                Connection.getInstance().establishConnectionForID(clientId, connectionCallback);
            } catch (AblyException e) {
                Toast.makeText(this,R.string.unabletoconnet,Toast.LENGTH_LONG).show();
                Log.e("AblyConnection", e.getMessage());
            }
        } else {
            try {
                Connection.getInstance().sendMessage(((EditText) findViewById(R.id.textET)).getText().toString());
                ((EditText) findViewById(R.id.textET)).setText("");
            } catch (AblyException e) {
                e.printStackTrace();
            }
        }

    }

    Channel.MessageListener messageListener = new Channel.MessageListener() {
        @Override
        public void onMessage(Message[] messages) {
            adapter.addItems(messages);
        }
    };

    Presence.PresenceListener presenceListener = new Presence.PresenceListener() {
        @Override
        public void onPresenceMessage(PresenceMessage[] presenceMessages) {
            for (final PresenceMessage presenceMessage:presenceMessages) {
                if (presenceMessage.action.equals(PresenceMessage.Action.ENTER) || presenceMessage.action.equals(PresenceMessage.Action.LEAVE) ) {
                    adapter.addItems(presenceMessages);
                }
                if (presenceMessage.action.equals(PresenceMessage.Action.UPDATE)) {
                    //handling of update user presence
                    if (!presenceMessage.clientId.equals(Connection.getInstance().userName)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (presenceMessage.data.equals("{isTyping:true}")) {
                                    usersCurrentlyTyping.add(presenceMessage.clientId);

                                } else if (presenceMessage.data.equals("{isTyping:false}")) {
                                    usersCurrentlyTyping.remove(presenceMessage.clientId);
                                }
                                if (usersCurrentlyTyping.size() > 0)
                                {
                                    StringBuilder messageToShow = new StringBuilder();
                                    switch (usersCurrentlyTyping.size())
                                    {
                                        case 1:
                                            messageToShow.append( usersCurrentlyTyping.get(0) + " is typing");
                                            break;
                                        case 2:
                                            messageToShow.append( usersCurrentlyTyping.get(0) + " & ");
                                            messageToShow.append( usersCurrentlyTyping.get(1) + " are typing");
                                            break;
                                        default :
                                            if (usersCurrentlyTyping.size() > 4) {
                                                messageToShow.append(usersCurrentlyTyping.get(0) + ", ");
                                                messageToShow.append(usersCurrentlyTyping.get(1) + ", ");
                                                messageToShow.append(usersCurrentlyTyping.get(2) + " & other are typing");
                                            } else {
                                                int i;
                                                for (i=0; i < usersCurrentlyTyping.size()-1; ++i) {
                                                    messageToShow.append(usersCurrentlyTyping.get(i) + ", ");
                                                }
                                                messageToShow.append(" & " + usersCurrentlyTyping.get(i) + " are typing");
                                            }
                                    }//end of switch

                                    ((TextView) findViewById(R.id.isTyping)).setText(messageToShow.toString());
                                    findViewById(R.id.isTyping).setVisibility(View.VISIBLE);
                                } else {
                                    findViewById(R.id.isTyping).setVisibility(View.GONE);
                                }
                            }
                        });

                    }
                }
            }
        }
    };

    public class ChatScreenAdapter extends BaseAdapter
    {
        LayoutInflater layoutInflater = getLayoutInflater();
        ArrayList<BaseMessage> items = new ArrayList<>();

        public void addItems(BaseMessage[] newItems)
        {
            for (BaseMessage item:newItems)
            {
                items.add(item);
            }

            notifyChange();
        }

        private void notifyChange()
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;

            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");

            if (items.get(position) instanceof Message) {
                //case for messages
                if (view == null)
                {
                    view = layoutInflater.inflate(R.layout.chatitem, parent,false);
                }
                Message message = ((Message) items.get(position));
                String userName = message.clientId;

                if (userName==null)
                {
                    ((TextView) view.findViewById(R.id.username)).setText(Connection.getInstance().userName);
                    view.setBackgroundResource(R.drawable.outgoingmessage);
                }
                else
                {
                    ((TextView) view.findViewById(R.id.username)).setText(userName);
                    view.setBackgroundResource(R.drawable.incommingmessage);
                }
                String dateString = formatter.format(new Date(message.timestamp));
                ((TextView) view.findViewById(R.id.timestamp)).setText(dateString);
                ((TextView) view.findViewById(R.id.message)).setText(message.data.toString());
            } else {
                //case for presence item
                if (view == null)
                {
                    view = layoutInflater.inflate(R.layout.presenceitem, parent,false);
                }
                PresenceMessage presenceMessage = ((PresenceMessage) items.get(position));
                String actionToShow = "";
                if (presenceMessage.action.equals(PresenceMessage.Action.ENTER)) {
                    actionToShow = " has entered the channel";
                } else if (presenceMessage.action.equals(PresenceMessage.Action.LEAVE)) {
                        actionToShow = " has left the channel";
                    }
                ((TextView) view.findViewById(R.id.action)).setText(presenceMessage.clientId + actionToShow);
                String dateString = formatter.format(new Date(presenceMessage.timestamp));
                ((TextView) view.findViewById(R.id.presenceTimeStamp)).setText(dateString);
            }

            return view;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        activityPaused = true;
        Connection.getInstance().disconnectAbly();
    }
}
