package com.example.searchengine.dao;

import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import com.example.searchengine.models.Field;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Repository
public class FieldDao {

    private static final Logger logger = LoggerFactory.getLogger(FieldDao.class);

    @PersistenceContext
    private EntityManager entityManager;

    public FieldDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Long save(Field field) {
        return executeWithLogging(() -> {
            entityManager.persist(field);
            logger.info("Поле успешно сохранено: {}", field);
            return field.getId();
        }, "Ошибка сохранения поля");
    }

    public void update(Field field) {
        executeWithLogging(() -> {
            entityManager.merge(field);
            logger.info("Поле успешно обновлено: {}", field);
            return null;
        }, "Ошибка обновления поля");
    }

    public Optional<Field> findById(Long id) {
        return executeWithLogging(() -> {
            Field field = entityManager.find(Field.class, id);
            if (field != null) {
                logger.info("Найдено поле: {}", field);
            } else {
                logger.warn("Поле не найдено с id: {}", id);
            }
            return Optional.ofNullable(field);
        }, "Ошибка поиска поля по id");
    }

    public List<Field> findAll() {
        return findAll(-1);
    }

    public List<Field> findAll(int limit) {
        TypedQuery<Field> query = entityManager.createQuery("FROM Field", Field.class);
        if (limit >= 0) {
            query.setMaxResults(limit);
        }
        List<Field> fields = query.getResultList();
        logger.info("Получено {} полей.", fields.size());
        return fields;
    }

    public void deleteAll() {
        executeWithLogging(() -> {
            entityManager.createQuery("DELETE FROM Field").executeUpdate();
            logger.info("Все поля удалены успешно.");
            return null;
        }, "Ошибка удаления всех полей");
    }

    public void delete(Field field) {
        executeWithLogging(() -> {
            entityManager.remove(entityManager.contains(field) ? field :
                    entityManager.merge(field));
            logger.info("Поле успешно удалено: {}", field);
            return null;
        }, "Ошибка удаления поля");
    }

    public Long count() {
        return ((Number) entityManager.createQuery(
                "SELECT COUNT(f) FROM Field f").getSingleResult()).longValue();
    }

    private <T> T executeWithLogging(Supplier<T> action, String errorMessage) {
        try {
            return action.get();
        } catch (Exception e) {
            logger.error("{}: {}", errorMessage, e.getMessage());
            throw new RuntimeException(errorMessage, e);
        }
    }
}