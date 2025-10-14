package com.example.searchengine.models;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "field_table")
public class Field {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "selector", nullable = false)
    private String selector;

    @Column(name = "weight", nullable = false)
    private BigDecimal weight;

    @Column(name = "value")
    private String value;

    public Field() {
    }

    public Field(String name, String selector, BigDecimal weight) {
        setName(name);
        setSelector(selector);
        setWeight(weight);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String fieldValue) {
        if (fieldValue == null || fieldValue.isEmpty()) {
            throw new IllegalArgumentException("Field value must not be null or empty");
        }
        this.value = fieldValue;
    }

    public void setName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name must not be null or empty");
        }
        this.name = name;
    }

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        if (selector == null || selector.isEmpty()) {
            throw new IllegalArgumentException("Selector must not be null or empty");
        }
        this.selector = selector;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        if (weight.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Weight must be non-negative");
        }
        this.weight = weight;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Field field = (Field) obj;
        return id == field.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Field{id=" + id + ", name='" + name + "', " +
                "selector='" + selector + "', weight=" + weight + "}";
    }
}