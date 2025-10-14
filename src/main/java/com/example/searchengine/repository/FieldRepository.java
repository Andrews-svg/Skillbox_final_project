package com.example.searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.searchengine.models.Field;

@Repository
public interface FieldRepository extends JpaRepository<Field, Long> {

}
