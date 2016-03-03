package io.ably.demo;

import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.java_websocket.util.Charsetfunctions;

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

    public void addItem(BaseMessage message) {
        this.items.add(message);
        this.sortItemsAndNotifyChange();
    }

    public void addItems(Iterable<? extends BaseMessage> newItems) {
        for (BaseMessage item : newItems) {
            items.add(item);
        }
        this.sortItemsAndNotifyChange();
    }

    private void sortItemsAndNotifyChange() {
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

            setupMessageView(position, convertView, formatter);
        } else {
            convertView = layoutInflater.inflate(R.layout.presenceitem, parent, false);

            setupPresenceView(position, convertView);
        }

        return convertView;
    }

    private void setupMessageView(int position, View convertView, SimpleDateFormat formatter) {
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
    }

    private void setupPresenceView(int position, View convertView) {
        PresenceMessage presenceMessage = (PresenceMessage) items.get(position);
        String actionToShow = "";
        if (presenceMessage.action.equals(PresenceMessage.Action.enter)) {
            ((TextView) convertView.findViewById(R.id.action)).setTextColor(Color.rgb(207, 207, 207));
            convertView.findViewById(R.id.action).setBackground(mainActivity.getResources().getDrawable(R.drawable.presencein));
            actionToShow = " has entered the channel";
        } else if (presenceMessage.action.equals(PresenceMessage.Action.leave)) {
            ((TextView) convertView.findViewById(R.id.action)).setTextColor(Color.rgb(102, 102, 102));
            convertView.findViewById(R.id.action).setBackground(mainActivity.getResources().getDrawable(R.drawable.presenceout));
            actionToShow = " has left the channel";
        }

        String actionText = this.createActionText(presenceMessage.clientId, presenceMessage.action, presenceMessage.timestamp);
        ((TextView) convertView.findViewById(R.id.action)).setText(actionText);
    }

    private String createActionText(String handle, PresenceMessage.Action action, long timestamp) {
        String actionText = "";
        switch(action) {
            case enter:
                actionText = "entered";
                break;
            case leave:
                actionText = "left";
                break;
            default:
                actionText = "";
                break;
        }
        String relativeDateText = DateUtils.getRelativeTimeSpanString(mainActivity.getApplicationContext(), timestamp).toString();
        return String.format("%s %s the channel %s", handle, actionText, relativeDateText);
    }
}
