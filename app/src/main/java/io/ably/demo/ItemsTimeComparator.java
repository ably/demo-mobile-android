package io.ably.demo;

import java.util.Comparator;

import io.ably.lib.types.BaseMessage;

public class ItemsTimeComparator implements Comparator<BaseMessage> {
    public int compare(BaseMessage left, BaseMessage right) {
        int res = (int) (left.timestamp - right.timestamp);
        return res;
    }
}
