package info.fmro.shared.javafx;

import javafx.scene.control.TreeItem;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/**
 * This interface can be used together with {@link SortableTreeItem} to sort the
 * items in the list.
 *
 * @param <T> element type
 */
@SuppressWarnings("JavadocReference")
@FunctionalInterface
public interface TreeItemComparator<T> {

    /**
     * Compare two tree items to do sorting
     *
     * @param parent the parent tree item of the elements or null if there is no parent
     * @param o1     the first element
     * @param o2     the second element
     * @return a negative integer, zero, or a positive integer as the
     * first argument is less than, equal to, or greater than the
     * second.
     */
    int compare(TreeItem<T> parent, T o1, T o2);

    /**
     * Utility method to create a TreeViewComparator from a given {@link Comparator}
     *
     * @param comparator the comparator
     * @return new TreeViewComparator
     */
    @NotNull
    @Contract(pure = true)
    static <T> TreeItemComparator<T> create(@SuppressWarnings("BoundedWildcard") final Comparator<T> comparator) {
        return (parent, o1, o2) -> comparator.compare(o1, o2);
    }
}
