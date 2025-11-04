package com.example.searchengine.models;

import com.example.searchengine.config.Site;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Data
@Indexed
@Entity
@Access(AccessType.FIELD)
@Table(name = "page", indexes = {
        @jakarta.persistence.Index(name = "idx_path", columnList = "path(255)")
})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotNull
    private Integer id;

    @Column(columnDefinition = "TEXT", nullable = false)
    @NotNull
    private String path;

    @Column(nullable = false)
    @NotNull
    private Integer code;

    @FullTextField(analyzer = "standard")
    @Lob
    @Column(nullable = false)
    @NotNull
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    @NotNull
    private Site site;

    public Page() {}

    public Page(Integer id, String path, Integer code, String content, Site site) {
        this.id = id;
        this.path = path;
        this.code = code;
        this.content = content;
        this.site = site;
    }

    public Page(String path, Integer code, String content, Site site) {
        this.path = path;
        this.code = code;
        this.content = content;
        this.site = site;
    }
}