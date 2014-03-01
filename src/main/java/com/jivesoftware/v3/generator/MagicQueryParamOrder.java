package com.jivesoftware.v3.generator;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ed.venaglia on 3/1/14.
 */
public class MagicQueryParamOrder implements Comparator<String> {

    enum Group {
        FIELDS,
        PAGINATION_START,
        PAGINATION_SIZE,
        BEFORE,
        AFTER,
        DEFAULT,
        FILTERS
    }

    private final Map<String,Group> groupings = new HashMap<>();

    public MagicQueryParamOrder() {
        groupings.put("fields", Group.FIELDS);
        groupings.put("startIndex", Group.PAGINATION_START);
        groupings.put("count", Group.PAGINATION_SIZE);
        groupings.put("before", Group.BEFORE);
        groupings.put("after", Group.AFTER);
        groupings.put("filter", Group.FILTERS);
    }

    @Override
    public int compare(String o1, String o2) {
        Group g1 = getGroup(o1);
        Group g2 = getGroup(o2);
        return g1 == g2 ? o1.compareToIgnoreCase(o2) : g1.compareTo(g2);
    }

    private Group getGroup(String key) {
        Group group = groupings.get(key);
        return group == null ? Group.DEFAULT : group;
    }
}
