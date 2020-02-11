package info.fmro.shared.stream.objects;

import java.io.Serializable;
import java.util.Date;

public interface EventInterface
        extends Serializable {
    String getId();

    String getName();

    Date getOpenDate();
}
