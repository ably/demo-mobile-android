package io.ably.demo.connection;

import io.ably.lib.types.AblyException;
import io.ably.lib.types.Message;

public interface MessageHistoryRetrievedCallback {
    void onMessageHistoryRetrieved(Iterable<Message> messages, Exception ex);
}
