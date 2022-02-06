package ru.herobrine1st.e621.api.model;

import androidx.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties({"has_active_children"})
public class Relationships {
    @Nullable
    public Integer getParentId() {
        return parentId;
    }

    public boolean isHasChildren() {
        return hasChildren;
    }

    public List<Integer> getChildren() {
        return children;
    }

    @Nullable
    Integer parentId;
    boolean hasChildren;
    List<Integer> children;
}
