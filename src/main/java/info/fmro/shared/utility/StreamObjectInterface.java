package info.fmro.shared.utility;

import java.io.Serializable;

public interface StreamObjectInterface
        extends Serializable {
    StreamObjectInterface getCopy();

    int runAfterReceive();

    void runBeforeSend();
}
