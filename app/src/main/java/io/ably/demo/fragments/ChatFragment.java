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
import io.ably.types.AblyException;
import io.ably.types.Message;
import io.ably.types.PresenceMessage;

public class ChatFragment extends Fragment implements View.OnClickListener {

    public ChatScreenAdapter adapter;
    private View mainViewRef;

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainViewRef = view;
        view.findViewById(R.id.sendBtn).setOnClickListener(this);
        adapter = new ChatScreenAdapter();
        Connection.getInstance().adapterReference = adapter;
        view.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        ((ListView) view.findViewById(R.id.chatList)).setAdapter(adapter);
    }

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
        ArrayList<Object> items = new ArrayList<>();

        public void addMessages(Message[] newItems)
        {
            for (Message item:newItems)
            {
                items.add(item);
            }

            notifyChanges();
        }

        public void addUsers(PresenceMessage[] newUsers)
        {
            for (PresenceMessage item:newUsers)
            {
                items.add(item);
            }

            notifyChanges();
        }

        private void sortArrayList()
        {
            //here we will be sorting the adapter by timestamp
        }

        private void notifyChanges()
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

            if (items.get(position) instanceof Message) {
                //case for a message
                if (view == null) {
                    view = layoutInflater.inflate(R.layout.chatitem, parent, false);
                }
                Message message = ((Message) items.get(position));
                String userName = message.name;
                ((TextView) view.findViewById(R.id.username)).setText(userName);
                if (userName.equals(Connection.getInstance().userName)) {
                    view.setBackgroundResource(R.drawable.outgoingmessage);
                } else {
                    view.setBackgroundResource(R.drawable.incommingmessage);
                }
                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
                String dateString = formatter.format(new Date(message.timestamp));
                ((TextView) view.findViewById(R.id.timestamp)).setText(dateString);
                ((TextView) view.findViewById(R.id.message)).setText(message.data.toString());
            }
            else
            {
                //case of a presence element
                if (view == null) {
                    view = layoutInflater.inflate(R.layout.presenceitem, parent, false);
                }
                PresenceMessage message = ((PresenceMessage) items.get(position));
                String userName = message.clientId;
                ((TextView) view.findViewById(R.id.action)).setText(userName + " has entered the channel");
                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
                String dateString = formatter.format(new Date(message.timestamp));
                ((TextView) view.findViewById(R.id.presenceTimeStamp)).setText(dateString);
            }

            return view;
        }
    }
}
