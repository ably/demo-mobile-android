package io.ably.demo.connection;

import io.ably.lib.types.AblyException;
import io.ably.lib.types.BaseMessage;
import io.ably.lib.types.Message;
import io.ably.lib.types.PresenceMessage;

public interface ConnectionCallback {

    void onConnectionCallback() throws AblyException;
    void onConnectionCallbackWithResult(BaseMessage[] result) throws AblyException;
}

