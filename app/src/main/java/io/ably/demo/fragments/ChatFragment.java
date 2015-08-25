package io.ably.demo.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import io.ably.demo.R;
import io.ably.demo.connection.Connection;
import io.ably.demo.connection.ConnectionCallback;
import io.ably.realtime.Channel;
import io.ably.realtime.Presence;
import io.ably.types.AblyException;
import io.ably.types.BaseMessage;
import io.ably.types.Message;
import io.ably.types.PresenceMessage;

public class ChatFragment extends Fragment implements View.OnClickListener {

    private View mainViewRef;
    public ChatScreenAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainViewRef = inflater.inflate(R.layout.chatscreen,container,false);

        return mainViewRef;
    }


    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainViewRef.findViewById(R.id.sendBtn).setOnClickListener(this);
        adapter = new ChatScreenAdapter();
        ListView listView = ((ListView) mainViewRef.findViewById(R.id.chatList));
        listView.setAdapter(adapter);
        view.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);

        try {
            Connection.getInstance().init(messageListener, presenceListener, new ConnectionCallback() {
                @Override
                public void onConnectionCallback() throws AblyException {
                    Connection.getInstance().getMessagesHistory(new ConnectionCallback() {

                        @Override
                        public void onConnectionCallback() {

                        }

                        @Override
                        public void onConnectionCallbackWithResult(BaseMessage[] result) throws AblyException {
                            adapter.addItems(result);
                            Connection.getInstance().getPresenceHistory(new ConnectionCallback() {
                                @Override
                                public void onConnectionCallback() {

                                }

                                @Override
                                public void onConnectionCallbackWithResult(BaseMessage[] result) throws AblyException {
                                    adapter.addItems(result);

                                    mainViewRef.findViewById(R.id.progressBar).setVisibility(View.GONE);
                                }
                            });
                        }
                    });
                }

                @Override
                public void onConnectionCallbackWithResult(BaseMessage[] result) throws AblyException {

                }
            });
        } catch (AblyException e) {
            e.printStackTrace();
        }
    }

    private Channel.MessageListener messageListener = new Channel.MessageListener() {
        @Override
        public void onMessage(Message[] messages) {
            adapter.addItems(messages);
        }
    };

    private Presence.PresenceListener presenceListener = new Presence.PresenceListener() {
        @Override
        public void onPresenceMessage(PresenceMessage[] presenceMessages) {
            //handling different cases - joined and left

            adapter.notify();
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.sendBtn:
                try {
                    Connection.getInstance().sendMessage(((EditText) mainViewRef.findViewById(R.id.textET)).getText().toString());
                    ((EditText) mainViewRef.findViewById(R.id.textET)).setText("");
                } catch (AblyException e) {
                    Toast.makeText(getActivity(),R.string.unabletosendmessage, Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.mentionBtn:
                break;
        }//end of switch
    }

    public class ChatScreenAdapter extends BaseAdapter
    {
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
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
            ChatFragment.this.getActivity().runOnUiThread(new Runnable() {
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
                String userName = message.name;
                ((TextView) view.findViewById(R.id.username)).setText(userName);
                if (userName.equals(Connection.getInstance().userName))
                {
                    view.setBackgroundResource(R.drawable.outgoingmessage);
                }
                else
                {
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
                ((TextView) view.findViewById(R.id.action)).setText(presenceMessage.clientId + " has entered the channel");
                String dateString = formatter.format(new Date(presenceMessage.timestamp));
                ((TextView) view.findViewById(R.id.timestamp)).setText(dateString);
            }

            return view;
        }
    }
}
