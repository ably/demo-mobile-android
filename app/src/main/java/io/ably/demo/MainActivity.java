package io.ably.demo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import io.ably.demo.connection.Connection;
import io.ably.demo.fragments.ChatFragment;
import io.ably.types.AblyException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.joinBtn).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        //showing the chat scree
        ChatFragment chatFragment = new ChatFragment();

        //establishing connection with ably service
        try {
            String clientId = ((TextView) findViewById(R.id.usernameET)).getText().toString();
            Connection.getInstance().establishConnectionForID(clientId);
        } catch (AblyException e) {
            Toast.makeText(this,R.string.unabletoconnet,Toast.LENGTH_LONG).show();
            Log.e("AblyConnection", e.getMessage());
        }

        //quick workaround because it is too late
        findViewById(R.id.usernameET).setVisibility(View.GONE);
        findViewById(R.id.joinBtn).setVisibility(View.GONE);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainer,chatFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();

    }
}
