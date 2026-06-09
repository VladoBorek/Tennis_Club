package com.example.tennis_club.dao;

import com.example.tennis_club.entities.Court;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CourtDao extends BaseDao<Court> {
    public CourtDao() {
        super(Court.class);
    }

    public Optional<Court> findActiveByCourtNumber(String courtNumber) {
        return entityManager.createQuery("""
                        select court
                        from Court court
                        join fetch court.surfaceType
                        where court.courtNumber = :courtNumber
                          and court.deleted = false
                        """, Court.class).
                setParameter("courtNumber", courtNumber).
                getResultList()
                .stream().
                findFirst();
    }

    public boolean existsActiveBySurfaceTypeId(Long surfaceTypeId) {
        Long count = entityManager.createQuery("""
                        select count(court)
                        from Court court
                        where court.surfaceType.id = :surfaceTypeId
                          and court.deleted = false
                        """, Long.class)
                .setParameter("surfaceTypeId", surfaceTypeId)
                .getSingleResult();

        return count > 0;
    }
}
