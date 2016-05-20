package io.ably.demo.connection;

import io.ably.lib.types.AblyException;
import io.ably.lib.types.PresenceMessage;

public interface PresenceHistoryRetrievedCallback {
    void onPresenceHistoryRetrieved(Iterable<PresenceMessage> presenceMessages);
}