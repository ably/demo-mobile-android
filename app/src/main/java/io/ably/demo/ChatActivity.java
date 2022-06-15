package io.ably.demo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.JsonObject;

import java.util.ArrayList;

import io.ably.demo.connection.Connection;
import io.ably.demo.connection.ConnectionCallback;
import io.ably.demo.connection.MessageHistoryRetrievedCallback;
import io.ably.demo.connection.PresenceHistoryRetrievedCallback;
import io.ably.demo.databinding.ActivityChatBinding;
import io.ably.lib.realtime.Channel;
import io.ably.lib.realtime.Presence;
import io.ably.lib.types.AblyException;
import io.ably.lib.types.Message;
import io.ably.lib.types.PresenceMessage;

public class ChatActivity extends AppCompatActivity implements Presence.PresenceListener, Channel.MessageListener {

    private static final String TAG = "ChatActivity";
    public static final String EXTRA_CLIENT_ID = "CLIENT_ID";

    private ActivityChatBinding binding;

    private String clientId = "";
    private ChatScreenAdapter adapter;

    private Handler isUserTypingHandler = new Handler();
    private boolean typingFlag = false;
    private ArrayList<String> usersCurrentlyTyping = new ArrayList<>();
    private ArrayList<String> presentUsers = new ArrayList<>();

    private Runnable isUserTypingRunnable = () -> {
        Connection.getInstance().userHasEndedTyping();
        typingFlag = false;
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
            if (!typingFlag) {
                Connection.getInstance().userHasStartedTyping(new ConnectionCallback() {
                    @Override
                    public void onConnectionCallback(Exception ex) {
                        if (ex != null) {
                            showError("Unable to send typing notification", ex);
                        }
                    }
                });
                typingFlag = true;
            }
            isUserTypingHandler.removeCallbacks(isUserTypingRunnable);
            isUserTypingHandler.postDelayed(isUserTypingRunnable, 5000);
        }
    };
    private MessageHistoryRetrievedCallback getMessageHistoryCallback = new MessageHistoryRetrievedCallback() {
        @Override
        public void onMessageHistoryRetrieved(Iterable<Message> messages, Exception ex) {
            if (ex != null) {
                showError("Unable to retrieve message history", ex);
                return;
            }
            adapter.addItems(messages);
        }
    };
    private PresenceHistoryRetrievedCallback getPresenceHistoryCallback = new PresenceHistoryRetrievedCallback() {
        @Override
        public void onPresenceHistoryRetrieved(Iterable<PresenceMessage> presenceMessages) {
            ArrayList<PresenceMessage> messagesExceptUpdates = new ArrayList<PresenceMessage>();
            for (PresenceMessage message : presenceMessages) {
                if (message.action != PresenceMessage.Action.update) {
                    messagesExceptUpdates.add(message);
                }
            }

            adapter.addItems(messagesExceptUpdates);
        }
    };
    private ConnectionCallback chatInitializedCallback = new ConnectionCallback() {
        @Override
        public void onConnectionCallback(Exception ex) {
            if (ex != null) {
                showError("Unable to connect to Ably service", ex);
                return;
            }

            addCurrentMembers();
            runOnUiThread(() -> {
                binding.textET.removeTextChangedListener(isUserTypingTextWatcher);
                binding.textET.addTextChangedListener(isUserTypingTextWatcher);
            });
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        clientId = getIntent().getStringExtra(EXTRA_CLIENT_ID);

        binding.mentionBtn.setOnClickListener(v -> onMentionClick());
        binding.textET.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                try {
                    CharSequence messageText = binding.textET.getText();

                    if (TextUtils.isEmpty(messageText)) {
                        Log.d(TAG, "message is empty and cant be sent!");
                        return false;
                    }

                    Connection.getInstance().sendMessage(messageText.toString(), ex -> {
                        if (ex != null) {
                            showError("Unable to send message", ex);
                            return;
                        }

                        runOnUiThread(() -> binding.textET.setText(""));
                    });
                } catch (AblyException e) {
                    e.printStackTrace();
                }
            }
            return false;
        });

        adapter = new ChatScreenAdapter(this.clientId);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true);
        binding.chatList.setLayoutManager(linearLayoutManager);
        binding.chatList.setAdapter(adapter);

        try {
            Connection.getInstance().init(this, this, ex -> {
                if (ex != null) {
                    showError("Unable to connect", ex);
                    return;
                }

                chatInitializedCallback.onConnectionCallback(ex);

                try {
                    Connection.getInstance().getMessagesHistory(getMessageHistoryCallback);
                    Connection.getInstance().getPresenceHistory(getPresenceHistoryCallback);
                } catch (AblyException e) {
                    chatInitializedCallback.onConnectionCallback(e);
                    e.printStackTrace();
                }
            });
        } catch (AblyException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Connection.getInstance().reconnectAbly();
    }

    private void addCurrentMembers() {
        for (PresenceMessage presenceMessage : Connection.getInstance().getPresentUsers()) {
            if (!presenceMessage.clientId.equals(Connection.getInstance().userName)) {
                presentUsers.add(presenceMessage.clientId);
            }
        }
        updatePresentUsersBadge();
    }

    private void onMentionClick() {
        AlertDialog.Builder adBuilder = new AlertDialog.Builder(this);
        adBuilder.setSingleChoiceItems(new PresenceAdapter(getLayoutInflater(), presentUsers, this.clientId), -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                runOnUiThread(() -> {
                    String textToAppend = String.format("@%s ", presentUsers.get(which));
                    binding.textET.append(textToAppend);
                    dialog.cancel();
                });
            }
        });
        adBuilder.setTitle("Handles");
        adBuilder.setIcon(R.drawable.user_list_title_icon);
        adBuilder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, final int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        runOnUiThread(() -> binding.textET.setText(presentUsers.get(resultCode)));
    }

    private void updatePresentUsersBadge() {
        runOnUiThread(() -> binding.presenceBadge.setText(String.valueOf(presentUsers.size())));
    }

    private void showError(final String title, final Exception ex) {
        runOnUiThread(() -> {
            Log.d("ChatActivity", title + " - " + ex.getMessage());
            Toast.makeText(ChatActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Connection.getInstance().disconnectAbly();
    }

    @Override
    public void onPresenceMessage(PresenceMessage message) {
        switch (message.action) {
            case enter:
                adapter.addItem(message);
                presentUsers.add(message.clientId);
                updatePresentUsersBadge();
                break;
            case leave:
                adapter.addItem(message);
                presentUsers.remove(message.clientId);
                updatePresentUsersBadge();
                break;
            case update:
                if (!message.clientId.equals(Connection.getInstance().userName)) {
                    runOnUiThread(() -> {
                        boolean isUserTyping = ((JsonObject) message.data).get("isTyping").getAsBoolean();
                        if (isUserTyping) {
                            usersCurrentlyTyping.add(message.clientId);
                        } else {
                            usersCurrentlyTyping.remove(message.clientId);
                        }

                        if (usersCurrentlyTyping.size() > 0) {
                            StringBuilder messageToShow = new StringBuilder();
                            switch (usersCurrentlyTyping.size()) {
                                case 1:
                                    messageToShow.append(usersCurrentlyTyping.get(0) + " is typing");
                                    break;
                                case 2:
                                    messageToShow.append(usersCurrentlyTyping.get(0) + " and ");
                                    messageToShow.append(usersCurrentlyTyping.get(1) + " are typing");
                                    break;
                                default:
                                    if (usersCurrentlyTyping.size() > 4) {
                                        messageToShow.append(usersCurrentlyTyping.get(0) + ", ");
                                        messageToShow.append(usersCurrentlyTyping.get(1) + ", ");
                                        messageToShow.append(usersCurrentlyTyping.get(2) + " and other are typing");
                                    } else {
                                        int i;
                                        for (i = 0; i < usersCurrentlyTyping.size() - 1; ++i) {
                                            messageToShow.append(usersCurrentlyTyping.get(i) + ", ");
                                        }
                                        messageToShow.append(" and " + usersCurrentlyTyping.get(i) + " are typing");
                                    }
                            }

                            binding.isTyping.setText(messageToShow.toString());
                            binding.isTypingContainer.setVisibility(View.VISIBLE);
                        } else {
                            binding.isTypingContainer.setVisibility(View.GONE);
                        }
                    });
                }
                break;
        }
    }

    @Override
    public void onMessage(Message message) {
        adapter.addItem(message);
    }
}
