package com.example.tennis_club.daos;

import com.example.tennis_club.entities.SurfaceType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class SurfaceTypeDao extends BaseDao<SurfaceType> {

    public SurfaceTypeDao() {
        super(SurfaceType.class);
    }

    public Optional<SurfaceType> findActiveByName(String name) {
        return entityManager.createQuery("""
                        select surfaceType
                        from SurfaceType surfaceType
                        where lower(surfaceType.name) = lower(:name)
                          and surfaceType.deleted = false
                        """, SurfaceType.class)
                .setParameter("name", name)
                .getResultStream()
                .findFirst();
    }
}