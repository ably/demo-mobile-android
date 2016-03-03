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
    ArrayList<String> items;

    public PresenceAdapter(MainActivity mainActivity, ArrayList<String> items) {
        this.mainActivity = mainActivity;
        this.items = items;
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
        TextView view = new TextView(this.mainActivity.getApplicationContext());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 0, 10, 5);
        view.setLayoutParams(layoutParams);
        view.setText(String.format("@%s", this.items.get(position)));
        view.setTextColor(Color.rgb(0, 0, 0));
        return view;
    }
}
