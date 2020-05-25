package info.fmro.shared.javafx;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TreeView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScrollBarState {
    private static final Logger logger = LoggerFactory.getLogger(ScrollBarState.class);
    private final TreeView<?> tree;
    private final Orientation orientation;
    private final String logMessage;
    private @Nullable ScrollBar scrollBar;

    private double min;
    private double max;
    private double value;
    private double blockIncrement;
    private double unitIncrement;

    public ScrollBarState(@NotNull final TreeView<?> tree, @NotNull final Orientation orientation, final String logMessage) {
        this.tree = tree;
        this.orientation = orientation;
        this.logMessage = logMessage;
    }

    public void reset() {
        this.scrollBar = null;
    }

    public void save(final boolean isVisible) {
        if (isVisible) {
            if (this.scrollBar == null && !setScrollBar()) {
                logger.error("unable to save scrollBar: {}", this.logMessage);
            } else {
                this.min = this.scrollBar.getMin();
                this.max = this.scrollBar.getMax();
                this.value = this.scrollBar.getValue();
                this.blockIncrement = this.scrollBar.getBlockIncrement();
                this.unitIncrement = this.scrollBar.getUnitIncrement();
            }
        } else { // won't do anything if not visible
        }
    }

    public void restore(final boolean isVisible) {
        if (isVisible) {
            if (this.scrollBar == null) {
                logger.error("trying to restore null scrollBar: {}", this.logMessage);
            } else {
                this.scrollBar.setMin(this.min);
                this.scrollBar.setMax(this.max);
                this.scrollBar.setValue(this.value);
                this.scrollBar.setUnitIncrement(this.unitIncrement);
                this.scrollBar.setBlockIncrement(this.blockIncrement);
            }
        } else { // won't do anything if not visible
        }
    }

    private boolean setScrollBar() {
        boolean foundScrollBar = false;
        for (final Node node : this.tree.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar && ((ScrollBar) node).getOrientation() == this.orientation) {
                this.scrollBar = (ScrollBar) node;
                foundScrollBar = true;
                break;
            } else { // not the node I look for, nothing to be done
            }
        }
        if (foundScrollBar) { // good, no error
        } else {
            logger.error("unable to setScrollBar: {}", this.logMessage);
        }
        return foundScrollBar;
    }
}
