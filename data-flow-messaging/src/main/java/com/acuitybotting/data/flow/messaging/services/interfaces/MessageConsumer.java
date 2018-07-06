package com.acuitybotting.data.flow.messaging.services.interfaces;

import com.acuitybotting.data.flow.messaging.services.Message;
import com.amazonaws.services.iot.client.AWSIotException;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by Zachary Herridge on 7/2/2018.
 */
public interface MessageConsumer {

    List<Consumer<Message>> getMessageCallbacks();

    default MessageConsumer withCallback(Consumer<Message> callback){
        getMessageCallbacks().add(callback);
        return this;
    }

    MessageConsumer start() throws Exception;

    MessageConsumer cancel() throws Exception;
}
