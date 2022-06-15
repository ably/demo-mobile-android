package io.ably.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import io.ably.demo.connection.Connection;
import io.ably.demo.connection.ConnectionCallback;
import io.ably.demo.databinding.ActivityMainBinding;
import io.ably.lib.types.AblyException;

public class MainActivity extends AppCompatActivity implements ConnectionCallback {

    //Enter your private key obtained from ably.com
    private static final String PRIVATE_KEY = "";

    private ActivityMainBinding binding;

    private boolean isAblyConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!PRIVATE_KEY.isEmpty()) {
            binding.keyET.setVisibility(View.GONE);
        }

        binding.joinBtn.setOnClickListener(v -> onJoinClick());
        binding.usernameET.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                onJoinClick();
            }

            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Connection.getInstance().reconnectAbly();
    }

    private void onJoinClick() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        binding.progressBar.setVisibility(View.VISIBLE);

        try {
            String clientId = binding.usernameET.getText().toString();
            String privateKey = PRIVATE_KEY;
            if (PRIVATE_KEY.isEmpty()) {
                privateKey = binding.keyET.getText().toString();
            }

            Connection.getInstance().establishConnectionForKey(clientId, privateKey, this);
        } catch (AblyException e) {
            showError("Unable to connect", e);
            Log.e("AblyConnection", e.getMessage());
        }
    }

    private void showError(final String title, final Exception ex) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Connection.getInstance().disconnectAbly();
    }

    @Override
    public void onConnectionCallback(Exception ex) {
        if (ex != null) {
            showError("Unable to connect", ex);
            return;
        }
        if (isAblyConnected) {
            Log.d("MainActivity", "Ably already connected");
            return;
        }

        isAblyConnected = true;

        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CLIENT_ID, binding.usernameET.getText().toString());
        startActivity(intent);
        finish();
    }
}
