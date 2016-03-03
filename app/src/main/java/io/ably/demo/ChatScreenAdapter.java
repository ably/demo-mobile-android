package io.ably.demo;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import io.ably.demo.connection.Connection;
import io.ably.lib.types.BaseMessage;
import io.ably.lib.types.Message;
import io.ably.lib.types.PresenceMessage;

public class ChatScreenAdapter extends BaseAdapter {
    private MainActivity mainActivity;
    LayoutInflater layoutInflater;
    ArrayList<BaseMessage> items = new ArrayList<>();

    public ChatScreenAdapter(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.layoutInflater = mainActivity.getLayoutInflater();
    }

    public void addItems(BaseMessage[] newItems) {
        for (BaseMessage item : newItems) {
            items.add(item);
        }
        Collections.sort(items, new ItemsTimeComparator());
        notifyChange();
    }

    private void notifyChange() {
        mainActivity.runOnUiThread(new Runnable() {
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

        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");

        if (items.get(position) instanceof Message) {
            convertView = layoutInflater.inflate(R.layout.chatitem, parent, false);

            Message message = ((Message) items.get(position));
            String userName = message.clientId;

            if (userName == null) {
                ((TextView) convertView.findViewById(R.id.username)).setText(Connection.getInstance().userName);
                convertView.setBackground(mainActivity.getResources().getDrawable(R.drawable.outgoingmessage));
            } else {
                ((TextView) convertView.findViewById(R.id.username)).setText(userName);
            }
            String dateString = formatter.format(new Date(message.timestamp));
            ((TextView) convertView.findViewById(R.id.timestamp)).setText(dateString);
            ((TextView) convertView.findViewById(R.id.message)).setText(message.data.toString());
        } else {
            convertView = layoutInflater.inflate(R.layout.presenceitem, parent, false);

            PresenceMessage presenceMessage = ((PresenceMessage) items.get(position));
            String actionToShow = "";
            if (presenceMessage.action.equals(PresenceMessage.Action.enter)) {
                ((TextView) convertView.findViewById(R.id.action)).setTextColor(Color.rgb(255, 255, 255));
                convertView.findViewById(R.id.userInfo).setBackground(mainActivity.getResources().getDrawable(R.drawable.presencein));
                actionToShow = " has entered the channel";
            } else if (presenceMessage.action.equals(PresenceMessage.Action.leave)) {
                ((TextView) convertView.findViewById(R.id.action)).setTextColor(Color.rgb(11, 11, 11));
                convertView.findViewById(R.id.userInfo).setBackground(mainActivity.getResources().getDrawable(R.drawable.presenceout));
                actionToShow = " has left the channel";
            }
            ((TextView) convertView.findViewById(R.id.action)).setText(presenceMessage.clientId + actionToShow);
            String dateString = formatter.format(new Date(presenceMessage.timestamp));
            ((TextView) convertView.findViewById(R.id.presenceTimeStamp)).setText(dateString);
        }

        return convertView;
    }
}
