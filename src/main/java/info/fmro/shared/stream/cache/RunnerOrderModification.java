package info.fmro.shared.stream.cache;

import com.google.common.math.DoubleMath;
import info.fmro.shared.stream.enums.Side;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;

public class RunnerOrderModification
        implements Serializable {
    @Serial
    private static final long serialVersionUID = -5229377916669814958L;
    private final double price, size;
    @NotNull
    private final Side side;
    private final long timeStamp;

    @Contract(pure = true)
    public RunnerOrderModification(@NotNull final Side side, final double price, final double size) {
        this.side = side;
        this.price = price;
        this.size = size;
        this.timeStamp = System.currentTimeMillis();
    }

    @NotNull
    public Side getSide() {
        return this.side;
    }

    public double getPrice() {
        return this.price;
    }

    public double getSize() {
        return this.size;
    }

    public long getTimeStamp() {
        return this.timeStamp;
    }

    public boolean contains(@NotNull final RunnerOrderModification containedObject) {
        return this.side == containedObject.getSide() && DoubleMath.fuzzyEquals(this.price, containedObject.getPrice(), 0.002d) && DoubleMath.fuzzyCompare(this.size, containedObject.getSize(), 0.001d) >= 0;
    }
}
