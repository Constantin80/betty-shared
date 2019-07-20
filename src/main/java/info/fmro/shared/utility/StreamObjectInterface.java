package info.fmro.shared.utility;

import java.io.Serializable;

public interface StreamObjectInterface
        extends Serializable {
    int runAfterReceive();

    void runBeforeSend();
}
