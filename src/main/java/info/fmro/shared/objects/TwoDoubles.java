package info.fmro.shared.objects;

import org.jetbrains.annotations.Contract;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class TwoDoubles
        implements Serializable {
    @Serial
    private static final long serialVersionUID = -5306509572507853826L;
    private final double firstDouble, secondDouble;

    public TwoDoubles(final double firstDouble, final double secondDouble) {
        this.firstDouble = firstDouble;
        this.secondDouble = secondDouble;
    }

    public double getFirstDouble() {
        return this.firstDouble;
    }

    public double getSecondDouble() {
        return this.secondDouble;
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final TwoDoubles that = (TwoDoubles) obj;
        return Double.compare(that.firstDouble, this.firstDouble) == 0 &&
               Double.compare(that.secondDouble, this.secondDouble) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.firstDouble, this.secondDouble);
    }
}
