package info.fmro.shared.entities;

import org.jetbrains.annotations.Contract;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class Competition
        implements Serializable {
    @Serial
    private static final long serialVersionUID = -3187113086865598702L;
    private final String id;
    private final String name;

    @Contract(pure = true)
    public Competition(final String id, final String name) {
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
        final Competition that = (Competition) obj;
        return Objects.equals(this.id, that.id) &&
               Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.name);
    }
}
