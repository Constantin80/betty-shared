package info.fmro.shared.javafx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Objects;

public class StringTreeData
        implements HierarchyData<StringTreeData>, Comparable<StringTreeData> {
    public static final int BEFORE = -1, EQUAL = 0, AFTER = 1;
    private final String data;
    private final ObservableList<StringTreeData> children = FXCollections.observableArrayList();

    public StringTreeData(final String data) {
        this.data = data;
    }

    @Override
    public ObservableList<StringTreeData> getChildren() {
        return this.children;
    }

    @Override
    public String toString() {
        return this.data;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final StringTreeData that = (StringTreeData) obj;
        return Objects.equals(this.data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.data);
    }

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    @Override
    public int compareTo(final StringTreeData o) {
        if (o == null) {
            return AFTER;
        }
        if (this == o) {
            return EQUAL;
        }

        if (this.getClass() != o.getClass()) {
            return this.getClass().hashCode() < o.getClass().hashCode() ? BEFORE : AFTER;
        }
        if (!Objects.equals(this.data, o.data)) {
            if (this.data == null) {
                return BEFORE;
            }
            if (o.data == null) {
                return AFTER;
            }
            return this.data.compareTo(o.data);
        }

        return EQUAL;
    }
}
