package edu.ohsu.cmp.htnu18app.repository.app;

import edu.ohsu.cmp.htnu18app.entity.app.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    @Query("select g from Goal g where g.patId=:patId")
    List<Goal> findAllByPatId(@Param("patId") Long patId);

    Goal findOneByPatIdAndExtGoalId(@Param("patId") Long patId, @Param("extGoalId") String extGoalId);

    @Query("select g from Goal g where g.patId=:patId and g.systolicTarget is not null and g.diastolicTarget is not null")
    Goal findCurrentBPGoal(@Param("patId") Long patId);

    @Modifying
    @Transactional
    @Query("delete from Goal where extGoalId=:extGoalId and patId=:patId")
    void deleteByGoalIdForPatient(@Param("extGoalId") String extGoalId, @Param("patId") Long patId);

    @Modifying
    @Transactional
    @Query("delete from Goal where systolicTarget is not null and diastolicTarget is not null and patId=:patId")
    void deleteBPGoalForPatient(@Param("patId") Long patId);
}
