package info.fmro.shared.stream.objects;

import java.io.Serializable;

public interface StreamObjectInterface
        extends Serializable {
    StreamObjectInterface getCopy();

    int runAfterReceive();

    void runBeforeSend();
}
