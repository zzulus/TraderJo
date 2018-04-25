package jo.tech;

import java.util.ArrayList;
import java.util.List;

public class ChangeList {
    private List<Change> changes = new ArrayList<>();

    public ChangeList() {
    }

    public void add(Change change) {
        changes.add(change);
    }

    public boolean allPositive() {
        return changes.stream().map(Change::getChange).allMatch(d -> d != null && d > 0);
    }

    public boolean allNegative() {
        return changes.stream().map(Change::getChange).allMatch(d -> d != null && d < 0);
    }

    public static ChangeList of(Change... changes) {
        ChangeList changeList = new ChangeList();
        for (Change change : changes) {
            changeList.add(change);
        }
        return changeList;
    }
}
