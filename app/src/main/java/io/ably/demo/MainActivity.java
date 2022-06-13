package io.ably.demo;

import android.app.AlertDialog;
import android.content.Context;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;

import java.util.ArrayList;

import io.ably.demo.connection.Connection;
import io.ably.demo.connection.ConnectionCallback;
import io.ably.demo.connection.MessageHistoryRetrievedCallback;
import io.ably.demo.connection.PresenceHistoryRetrievedCallback;
import io.ably.demo.databinding.ActivityMainBinding;
import io.ably.lib.realtime.Channel;
import io.ably.lib.realtime.Presence;
import io.ably.lib.types.AblyException;
import io.ably.lib.types.Message;
import io.ably.lib.types.PresenceMessage;

public class MainActivity extends AppCompatActivity {

    //Enter your private key obtained from ably.com
    private static final String PRIVATE_KEY = "";

    private ActivityMainBinding binding;
    ChatScreenAdapter adapter;
    Channel.MessageListener messageListener = new Channel.MessageListener() {
        @Override
        public void onMessage(Message message) {
            adapter.addItem(message);
        }
    };
    private boolean activityPaused = false;
    private Handler isUserTypingHandler = new Handler();
    private boolean typingFlag = false;
    private ArrayList<String> usersCurrentlyTyping = new ArrayList<>();
    private ArrayList<String> presentUsers = new ArrayList<>();
    Presence.PresenceListener presenceListener = new Presence.PresenceListener() {
        @Override
        public void onPresenceMessage(final PresenceMessage presenceMessage) {
            switch (presenceMessage.action) {
                case enter:
                    adapter.addItem(presenceMessage);
                    presentUsers.add(presenceMessage.clientId);
                    updatePresentUsersBadge();
                    break;
                case leave:
                    adapter.addItem(presenceMessage);
                    presentUsers.remove(presenceMessage.clientId);
                    updatePresentUsersBadge();
                    break;
                case update:
                    if (!presenceMessage.clientId.equals(Connection.getInstance().userName)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                boolean isUserTyping = ((JsonObject) presenceMessage.data).get("isTyping").getAsBoolean();
                                if (isUserTyping) {
                                    usersCurrentlyTyping.add(presenceMessage.clientId);
                                } else {
                                    usersCurrentlyTyping.remove(presenceMessage.clientId);
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
                            }
                        });
                    }
                    break;
            }
        }
    };
    private String clientId, privateKey;
    private Runnable isUserTypingRunnable = new Runnable() {
        @Override
        public void run() {
            Connection.getInstance().userHasEndedTyping();
            typingFlag = false;
        }
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    binding.progressBar.setVisibility(View.GONE);
                    binding.chatLayout.setVisibility(View.VISIBLE);
                    binding.textET.removeTextChangedListener(isUserTypingTextWatcher);
                    binding.textET.addTextChangedListener(isUserTypingTextWatcher);
                }
            });
        }
    };
    private ConnectionCallback connectionCallback = new ConnectionCallback() {
        @Override
        public void onConnectionCallback(Exception ex) {
            if (ex != null) {
                showError("Unable to connect", ex);
                return;
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showChatScreen();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!PRIVATE_KEY.isEmpty()) {
            binding.keyET.setVisibility(View.GONE);
        }

        binding.joinBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onJoinClick();
            }
        });
        binding.mentionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMentionClick();
            }
        });
        binding.textET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    try {
                        CharSequence messageText = binding.textET.getText();

                        if (TextUtils.isEmpty(messageText)) {
                            return false;
                        }

                        Connection.getInstance().sendMessage(messageText.toString(), new ConnectionCallback() {
                            @Override
                            public void onConnectionCallback(Exception ex) {
                                if (ex != null) {
                                    showError("Unable to send message", ex);
                                    return;
                                }

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        binding.textET.setText("");
                                    }
                                });
                            }
                        });
                    } catch (AblyException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });
        binding.usernameET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    onJoinClick();
                }

                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (activityPaused) {
            Connection.getInstance().reconnectAbly();
            activityPaused = false;
        }
    }

    private void showChatScreen() {
        binding.loginLayout.setVisibility(View.GONE);

        adapter = new ChatScreenAdapter(this, this.clientId);
        binding.chatList.setAdapter(adapter);
        try {
            Connection.getInstance().init(messageListener, presenceListener, new ConnectionCallback() {
                @Override
                public void onConnectionCallback(Exception ex) {
                    if (ex != null) {
                        showError("Unable to connect", ex);
                        return;
                    }

                    chatInitializedCallback.onConnectionCallback(ex);

                    try {
                        Connection.getInstance().getMessagesHistory(MainActivity.this.getMessageHistoryCallback);
                        Connection.getInstance().getPresenceHistory(MainActivity.this.getPresenceHistoryCallback);
                    } catch (AblyException e) {
                        chatInitializedCallback.onConnectionCallback(e);
                        e.printStackTrace();
                    }
                }
            });
        } catch (AblyException e) {
            e.printStackTrace();
        }
    }

    private void addCurrentMembers() {
        for (PresenceMessage presenceMessage : Connection.getInstance().getPresentUsers()) {
            if (!presenceMessage.clientId.equals(Connection.getInstance().userName)) {
                presentUsers.add(presenceMessage.clientId);
            }
        }
        updatePresentUsersBadge();
    }

    private void onJoinClick() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        binding.loginLayout.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);

        try {
            this.clientId = binding.usernameET.getText().toString();
            if (PRIVATE_KEY.isEmpty()) {
                this.privateKey = binding.keyET.getText().toString();
            } else {
                this.privateKey = PRIVATE_KEY;
            }

            Connection.getInstance().establishConnectionForKey(this.clientId, this.privateKey, connectionCallback);
        } catch (AblyException e) {
            showError("Unable to connect", e);
            Log.e("AblyConnection", e.getMessage());
        }
    }

    private void onMentionClick() {
        AlertDialog.Builder adBuilder = new AlertDialog.Builder(this);
        adBuilder.setSingleChoiceItems(new PresenceAdapter(this, presentUsers, this.clientId), -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String textToAppend = String.format("@%s ", presentUsers.get(which));
                        binding.textET.append(textToAppend);
                        dialog.cancel();
                    }
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.textET.setText(presentUsers.get(resultCode));
            }
        });
    }

    private void updatePresentUsersBadge() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.presenceBadge.setText(String.valueOf(presentUsers.size()));
            }
        });
    }

    private void showError(final String title, final Exception ex) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
             /*   AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
                dialogBuilder.setTitle(title);
                dialogBuilder.setMessage(ex.getMessage());
                dialogBuilder.setCancelable(true);
                dialogBuilder.show();*/
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        activityPaused = true;
        Connection.getInstance().disconnectAbly();
    }
}
