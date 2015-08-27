package io.ably.demo.fragments;

import android.os.AsyncTask;
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

import java.net.ConnectException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import io.ably.demo.R;
import io.ably.demo.connection.Connection;
import io.ably.types.AblyException;
import io.ably.types.Message;

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
        mainViewRef.findViewById(R.id.sendBtn).setOnClickListener(this);
        adapter = new ChatScreenAdapter();
        Connection.getInstance().adapterReference = adapter;

        return mainViewRef;
    }


    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ListView listView = ((ListView) mainViewRef.findViewById(R.id.chatList));
        listView.setAdapter(adapter);
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
        ArrayList<Message> items = new ArrayList<>();

        public void addItems(Message[] newItems)
        {
            for (Message item:newItems)
            {
                items.add(item);
            }

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
            if (view == null)
            {
                view = layoutInflater.inflate(R.layout.chatitem, parent,false);
            }

            String userName = items.get(position).name;
            ((TextView) view.findViewById(R.id.username)).setText(userName);
            if (userName.equals(Connection.getInstance().userName))
            {
                view.setBackgroundResource(R.drawable.outgoingmessage);
            }
            else
            {
                view.setBackgroundResource(R.drawable.incommingmessage);
            }
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            String dateString = formatter.format(new Date(items.get(position).timestamp));
            ((TextView) view.findViewById(R.id.timestamp)).setText(dateString);
            ((TextView) view.findViewById(R.id.message)).setText(items.get(position).data.toString());
            return view;
        }
    }
}
