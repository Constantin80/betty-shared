package info.fmro.shared.stream.objects;

import java.io.Serializable;

@SuppressWarnings("InterfaceMayBeAnnotatedFunctional")
public interface StreamObjectInterface
        extends Serializable {
    StreamObjectInterface getCopy();
}
