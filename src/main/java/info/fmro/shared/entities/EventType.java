package info.fmro.shared.entities;

import org.jetbrains.annotations.Contract;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class EventType
        implements Serializable {
    @Serial
    private static final long serialVersionUID = -2158475045630703415L;
    private final String id;
    private final String name;

    @Contract(pure = true)
    public EventType(final String id, final String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final EventType eventType = (EventType) obj;
        return Objects.equals(this.id, eventType.id) &&
               Objects.equals(this.name, eventType.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.name);
    }
}
