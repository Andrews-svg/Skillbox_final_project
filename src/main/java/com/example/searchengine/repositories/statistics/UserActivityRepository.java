package com.example.searchengine.repositories;

import com.example.searchengine.models.UserActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivityLog, Long> {


    List<UserActivityLog> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    List<UserActivityLog> findByUsernameOrderByTimestampDesc(String username, Pageable pageable);


    List<UserActivityLog> findByActionOrderByTimestampDesc(String action, Pageable pageable);


    List<UserActivityLog> findByIpAddressOrderByTimestampDesc(String ipAddress, Pageable pageable);


    List<UserActivityLog> findByTimestampBetweenOrderByTimestampDesc(Instant start, Instant end);


    List<UserActivityLog> findBySuccessFalseAndActionOrderByTimestampDesc(String action, Pageable pageable);


    @Query("SELECT FUNCTION('DATE', a.timestamp) as date, COUNT(a) as count " +
            "FROM UserActivityLog a " +
            "WHERE a.timestamp BETWEEN :start AND :end " +
            "GROUP BY FUNCTION('DATE', a.timestamp) " +
            "ORDER BY date")
    List<Map<String, Object>> getActivityCountByDay(@Param("start") Instant start,
                                                    @Param("end") Instant end);


    @Query("SELECT a.action as action, COUNT(a) as count " +
            "FROM UserActivityLog a " +
            "WHERE a.timestamp BETWEEN :start AND :end " +
            "GROUP BY a.action " +
            "ORDER BY count DESC")
    List<Map<String, Object>> getPopularActions(@Param("start") Instant start,
                                                @Param("end") Instant end);


    @Query("SELECT COUNT(DISTINCT a.ipAddress) FROM UserActivityLog a " +
            "WHERE a.timestamp BETWEEN :start AND :end")
    Long countUniqueIps(@Param("start") Instant start, @Param("end") Instant end);


    @Query("SELECT COUNT(DISTINCT a.userId) FROM UserActivityLog a " +
            "WHERE a.timestamp BETWEEN :start AND :end AND a.success = true")
    Long countActiveUsers(@Param("start") Instant start, @Param("end") Instant end);


    @Query("SELECT a.ipAddress, COUNT(a) as attempts " +
            "FROM UserActivityLog a " +
            "WHERE a.action LIKE '%FAILED%' AND a.timestamp BETWEEN :start AND :end " +
            "GROUP BY a.ipAddress " +
            "HAVING COUNT(a) > :threshold")
    List<Map<String, Object>> findSuspiciousIps(@Param("start") Instant start,
                                                @Param("end") Instant end,
                                                @Param("threshold") long threshold);
}