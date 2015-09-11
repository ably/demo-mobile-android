package io.ably.demo.connection;

import io.ably.types.AblyException;
import io.ably.types.BaseMessage;

public interface ConnectionCallback {

    void onConnectionCallback() throws AblyException;
    void onConnectionCallbackWithResult(BaseMessage[] result) throws AblyException;
}
