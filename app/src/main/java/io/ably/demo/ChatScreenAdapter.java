package io.ably.demo;

import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

import io.ably.lib.types.BaseMessage;
import io.ably.lib.types.Message;
import io.ably.lib.types.PresenceMessage;

public class ChatScreenAdapter extends BaseAdapter {
    LayoutInflater layoutInflater;
    ArrayList<BaseMessage> items = new ArrayList<>();
    private MainActivity mainActivity;
    private String ownClientId;

    public ChatScreenAdapter(MainActivity mainActivity, String ownClientId) {
        this.mainActivity = mainActivity;
        this.layoutInflater = mainActivity.getLayoutInflater();
        this.ownClientId = ownClientId;
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
        if (this.items.get(position) instanceof Message) {
            Message message = (Message) this.items.get(position);
            if (!this.ownClientId.equals(this.items.get(position).clientId)) {
                convertView = this.layoutInflater.inflate(R.layout.chat_message_incoming, parent, false);
                this.setupIncomingMessageView(message, convertView);
            } else {
                convertView = this.layoutInflater.inflate(R.layout.chat_message_outgoing, parent, false);
                this.setupOutgoingMessageView(message, convertView);
            }

        } else {
            convertView = this.layoutInflater.inflate(R.layout.presence_message, parent, false);

            this.setupPresenceView(position, convertView);
        }

        return convertView;
    }

    private void setupIncomingMessageView(Message message, View convertView) {
        String relativeDateText = DateUtils.getRelativeTimeSpanString(mainActivity.getApplicationContext(), message.timestamp).toString();

        ((TextView) convertView.findViewById(R.id.username)).setText(message.clientId);
        ((TextView) convertView.findViewById(R.id.timestamp)).setText(relativeDateText);
        ((TextView) convertView.findViewById(R.id.message)).setText(message.data.toString());
    }

    private void setupOutgoingMessageView(Message message, View convertView) {
        String relativeDateText = DateUtils.getRelativeTimeSpanString(mainActivity.getApplicationContext(), message.timestamp).toString();

        ((TextView) convertView.findViewById(R.id.timestamp)).setText(relativeDateText);
        ((TextView) convertView.findViewById(R.id.message)).setText(message.data.toString());
    }

    private void setupPresenceView(int position, View convertView) {
        PresenceMessage presenceMessage = (PresenceMessage) items.get(position);
        TextView actionView = (TextView) convertView.findViewById(R.id.action);

        if (presenceMessage.action.equals(PresenceMessage.Action.enter)) {
            actionView.setTextColor(Color.rgb(207, 207, 207));
            actionView.setBackground(mainActivity.getResources().getDrawable(R.drawable.presence_in));
        } else if (presenceMessage.action.equals(PresenceMessage.Action.leave)) {
            actionView.setTextColor(Color.rgb(102, 102, 102));
            actionView.setBackground(mainActivity.getResources().getDrawable(R.drawable.presence_out));
        }

        String actionText = this.createActionText(presenceMessage.clientId, presenceMessage.action, presenceMessage.timestamp);
        actionView.setText(actionText);
    }

    private String createActionText(String clientId, PresenceMessage.Action action, long timestamp) {
        String actionText = "";
        switch (action) {
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

        String handle = clientId.equals(this.ownClientId) ? "You" : clientId;
        String relativeDateText = DateUtils.getRelativeTimeSpanString(mainActivity.getApplicationContext(), timestamp).toString();
        return String.format("%s %s the channel %s", handle, actionText, relativeDateText);
    }
}
