package edu.ohsu.cmp.htnu18app.model;

import edu.ohsu.cmp.htnu18app.entity.app.AchievementStatus;
import edu.ohsu.cmp.htnu18app.entity.app.GoalHistory;
import edu.ohsu.cmp.htnu18app.entity.app.LifecycleStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class GoalHistoryModel implements Comparable<GoalHistoryModel> {
    private Long id;
    private Long goalId;
    private AchievementStatus achievementStatus;
    private LifecycleStatus lifecycleStatus;
    private Date createdDate;

    public GoalHistoryModel(GoalHistory gh) {
        this.id = gh.getId();
        this.goalId = gh.getGoalId();
        this.achievementStatus = gh.getAchievementStatus();
        this.lifecycleStatus = gh.getLifecycleStatus();
        this.createdDate = gh.getCreatedDate();
    }

    @Override
    public int compareTo(@NotNull GoalHistoryModel o) {
        return createdDate.compareTo(o.getCreatedDate());
    }

    public Long getId() {
        return id;
    }

    public Long getGoalId() {
        return goalId;
    }

    public AchievementStatus getAchievementStatus() {
        return achievementStatus;
    }

    public LifecycleStatus getLifecycleStatus() {
        return lifecycleStatus;
    }

    public Date getCreatedDate() {
        return createdDate;
    }
}
