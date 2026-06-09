package com.example.tennis_club.dao;

import com.example.tennis_club.entities.BaseEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

public abstract class BaseDao<T extends BaseEntity> {
    @PersistenceContext
    protected EntityManager entityManager;


    private final Class<T> entityClass;

    public BaseDao(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public T save(T entity) {
        if (entity.getId() == null) {
            entityManager.persist(entity);
            return entity;
        }
        return entityManager.merge(entity);
    }

    public Optional<T> findById(Long id) {
        return Optional.ofNullable(entityManager.find(entityClass, id));
    }

    public Optional<T> findActiveById(Long id) {
        String query = """
                select entity
                from %s entity
                where entity.id = :id
                  and entity.deleted = false
                """.formatted(entityClass.getSimpleName());

        return entityManager.createQuery(query, entityClass)
                .setParameter("id", id)
                .getResultStream()
                .findFirst();
    }

    public List<T> findAllActive() {
        String query = """
                select entity
                from %s entity
                where entity.deleted = false
                """.formatted(entityClass.getSimpleName());

        return entityManager.createQuery(query, entityClass)
                .getResultList();
    }

    public T softDelete(T entity) {
        entity.setDeleted(true);
        return save(entity);
    }

}
