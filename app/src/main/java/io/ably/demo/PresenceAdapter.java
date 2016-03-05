package io.ably.demo;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class PresenceAdapter extends BaseAdapter {

    private final MainActivity mainActivity;
    private final String ownHandle;
    ArrayList<String> items;

    public PresenceAdapter(MainActivity mainActivity, ArrayList<String> items, String ownHandle) {
        this.mainActivity = mainActivity;
        this.items = items;
        this.ownHandle = ownHandle;
    }

    @Override
    public int getCount() {
        return this.items.size();
    }

    @Override
    public Object getItem(int position) {
        return this.items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mainActivity.getLayoutInflater().inflate(R.layout.user_list_item, parent, false);
        }

        TextView handleView = (TextView) convertView.findViewById(R.id.handle);
        String handle = this.items.get(position);

        if (handle.equals(this.ownHandle)) {
            handleView.setText(String.format("@%s (me)", handle));
        } else {
            handleView.setText(String.format("@%s", handle));
        }

        return convertView;
    }
}
