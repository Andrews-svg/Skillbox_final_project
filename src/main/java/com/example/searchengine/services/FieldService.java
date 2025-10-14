package com.example.searchengine.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.example.searchengine.dao.FieldDao;
import com.example.searchengine.exceptions.CustomNotFoundException;
import com.example.searchengine.exceptions.CustomServiceException;
import com.example.searchengine.models.Field;
import com.example.searchengine.repository.FieldRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class FieldService {

    private static final Logger logger = LoggerFactory.getLogger(FieldService.class);
    private final FieldDao fieldDao;
    private final FieldRepository fieldRepository;

    public FieldService(FieldDao fieldDao, FieldRepository fieldRepository) {
        this.fieldDao = fieldDao;
        this.fieldRepository = fieldRepository;
    }

    public Optional<Field> findField(long id) {
        try {
            Optional<Field> field = fieldRepository.findById(id);
            if (field.isPresent()) {
                logger.info("Field found with id {}: {}", id, field.get());
                return field;
            } else {
                logger.warn("No field found with id {}", id);
                throw new CustomNotFoundException("Field not found with id " + id);
            }
        } catch (Exception e) {
            logger.error("Error finding field with id {}: {}", id, e.getMessage());
            throw new CustomServiceException("Error finding field with id " + id, e);
        }
    }

    public void saveField(Field field) {
        validateField(field);
        logger.info("Saving field: {}", field);
        fieldRepository.save(field);
    }

    public void deleteField(Field field) {
        try {
            logger.info("Deleting field: {}", field);
            fieldRepository.delete(field);
        } catch (Exception e) {
            logger.error("Error deleting field: {}", e.getMessage());
            throw new CustomServiceException("Error deleting field", e);
        }
    }

    public void updateField(Field field) {
        validateField(field);
        logger.info("Updating field: {}", field);
        fieldDao.update(field);
    }

    public List<Field> findAllFields() {
        return (List<Field>) fieldRepository.findAll();
    }

    public void deleteAllFields() {
        try {
            logger.info("Deleting all fields");
            fieldRepository.deleteAll();
        } catch (Exception e) {
            logger.error("Error deleting all fields: {}", e.getMessage());
            throw new CustomServiceException("Error deleting all fields", e);
        }
    }

    public void initializeFields() {
        deleteAllFields();

        Field title = new Field();
        title.setName("title");
        title.setSelector("title");
        title.setWeight(BigDecimal.valueOf(1.0));
        saveField(title);

        Field body = new Field();
        body.setName("body");
        body.setSelector("body");
        body.setWeight(BigDecimal.valueOf(0.8));
        saveField(body);
    }

    private void validateField(Field field) {
        if (field.getName() == null || field.getName().isEmpty()) {
            throw new IllegalArgumentException("Field name must not be null or empty");
        }
        if (field.getWeight() == null || field.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Field weight must be positive");
        }
    }
}