package io.ably.demo;

import android.graphics.Color;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;

import io.ably.demo.databinding.ChatMessageIncomingBinding;
import io.ably.demo.databinding.ChatMessageOutgoingBinding;
import io.ably.demo.databinding.PresenceMessageBinding;
import io.ably.lib.types.BaseMessage;
import io.ably.lib.types.Message;
import io.ably.lib.types.PresenceMessage;

public class ChatScreenAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ChatScreenAdapter";

    private static final int TYPE_PRESENCE = 0;
    private static final int TYPE_OUTGOING = 1;
    private static final int TYPE_INCOMING = 2;

    private final ArrayList<BaseMessage> items = new ArrayList<>();
    private final String ownClientId;

    public ChatScreenAdapter(String ownClientId) {
        this.ownClientId = ownClientId;
    }

    public void addItem(BaseMessage message) {
        if (items.contains(message)) {
            Log.d(TAG, "Duplicated message will not be added");
            return;
        }

        this.items.add(0, message);
        Collections.sort(items, new ItemsTimeComparator());
        notifyItemInserted(0);
        //notifyItemInserted(items.size());
        //notifyDataSetChanged();
    }

    public void addItems(Iterable<? extends BaseMessage> newItems) {
        int start = items.size();
        for (BaseMessage item : newItems) {
            items.add(item);
        }
        Collections.sort(items, new ItemsTimeComparator());
        //notifyDataSetChanged();
        notifyItemRangeInserted(start, items.size());
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_PRESENCE: {
                PresenceMessageBinding binding = PresenceMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new PresenceViewHolder(binding);
            }
            case TYPE_INCOMING: {
                ChatMessageIncomingBinding binding = ChatMessageIncomingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new IncomingViewHolder(binding);
            }
            case TYPE_OUTGOING: {
                ChatMessageOutgoingBinding binding = ChatMessageOutgoingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new OutgoingViewHolder(binding);
            }
            default: {
                return null;
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case TYPE_PRESENCE: {
                PresenceViewHolder vh = ((PresenceViewHolder) holder);
                vh.bind(((PresenceMessage) items.get(position)), ownClientId);
                break;
            }
            case TYPE_INCOMING: {
                IncomingViewHolder vh = ((IncomingViewHolder) holder);
                vh.bind(items.get(position));
                break;
            }
            case TYPE_OUTGOING: {
                OutgoingViewHolder vh = ((OutgoingViewHolder) holder);
                vh.bind(items.get(position));
                break;
            }
        }
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).timestamp;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof Message) {
            if (!ownClientId.equals(items.get(position).clientId)) {
                return TYPE_INCOMING;
            } else {
                return TYPE_OUTGOING;
            }
        } else {
            return TYPE_PRESENCE;
        }
    }

    public static class IncomingViewHolder extends RecyclerView.ViewHolder {
        private final ChatMessageIncomingBinding binding;

        public IncomingViewHolder(ChatMessageIncomingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(BaseMessage item) {
            String relativeDateText = DateUtils.getRelativeTimeSpanString(binding.getRoot().getContext(), item.timestamp).toString();

            binding.username.setText(item.clientId);
            binding.timestamp.setText(relativeDateText);
            binding.message.setText(item.data.toString());
        }
    }

    public static class OutgoingViewHolder extends RecyclerView.ViewHolder {
        private final ChatMessageOutgoingBinding binding;

        public OutgoingViewHolder(ChatMessageOutgoingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(BaseMessage item) {
            String relativeDateText = DateUtils.getRelativeTimeSpanString(binding.getRoot().getContext(), item.timestamp).toString();

            binding.timestamp.setText(relativeDateText);
            binding.message.setText(item.data.toString());
        }
    }

    public static class PresenceViewHolder extends RecyclerView.ViewHolder {
        private final PresenceMessageBinding binding;

        public PresenceViewHolder(PresenceMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(PresenceMessage item, String ownClientId) {
            if (item.action.equals(PresenceMessage.Action.enter)) {
                binding.action.setTextColor(Color.rgb(207, 207, 207));
                binding.action.setBackground(ContextCompat.getDrawable(binding.getRoot().getContext(), R.drawable.presence_in));
            } else if (item.action.equals(PresenceMessage.Action.leave)) {
                binding.action.setTextColor(Color.rgb(102, 102, 102));
                binding.action.setBackground(ContextCompat.getDrawable(binding.getRoot().getContext(), R.drawable.presence_out));
            }

            binding.action.setText(createActionText(item.clientId, ownClientId, item.action, item.timestamp));
        }

        private String createActionText(String clientId, String ownClientId, PresenceMessage.Action action, long timestamp) {
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

            String handle = clientId.equals(ownClientId) ? "You" : clientId;
            String relativeDateText = DateUtils.getRelativeTimeSpanString(binding.getRoot().getContext(), timestamp).toString();
            return String.format("%s %s the channel %s", handle, actionText, relativeDateText);
        }
    }

}
