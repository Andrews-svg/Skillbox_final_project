package com.example.searchengine.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;


@Indexed
@Getter
@Setter
@RequiredArgsConstructor
@Entity
@Access(AccessType.FIELD)
@Table(name = "page", indexes = {
        @jakarta.persistence.Index(name = "idx_path", columnList = "path"),
        @jakarta.persistence.Index(name = "idx_url", columnList = "url")
})

@NamedQueries({
        @NamedQuery(name = "Page.findByStatus",
                query = "SELECT p FROM Page p WHERE p.status = :status"),
        @NamedQuery(name = "Page.findByUrl",
                query = "SELECT p FROM Page p WHERE p.url = :url"),
        @NamedQuery(name = "Page.findAll",
                query = "SELECT p FROM Page p")
})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Page {

    private static final Logger logger = LoggerFactory.getLogger(Page.class);


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotNull
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    @NotNull
    private String url;

    @Column(nullable = false, unique = true, length = 255)
    @NotNull
    private String uri;

    @Column(nullable = false, length = 255)
    @NotNull
    private String path;

    @Column(nullable = false)
    @NotNull
    private Integer code = 200;

    @Column(length = 255)
    @NotNull
    private String name;

    @FullTextField(analyzer = "standard")
    @Lob
    @Column(nullable = false)
    @NotNull
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    @NotNull
    private Site site;

    @Column(length = 255)
    @NotNull
    private String title;

    @Column(length = 255)
    @NotNull
    private String snippet;

    @Column(nullable = false)
    @NotNull
    private float relevance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private Status status;

    @Column(nullable = false)
    @NotNull
    private boolean available = true;



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Page page)) return false;
        return Float.compare(page.relevance, relevance) == 0 &&
                Objects.equals(id, page.id) &&
                Objects.equals(url, page.url) &&
                Objects.equals(path, page.path) &&
                Objects.equals(code, page.code) &&
                Objects.equals(title, page.title) &&
                Objects.equals(snippet, page.snippet) &&
                status == page.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, url, path, code,
                title, snippet, relevance, status);
    }

    @Override
    public String toString() {
        return "Page{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", path='" + path + '\'' +
                ", code=" + code +
                ", title='" + title + '\'' +
                ", snippet='" + snippet + '\'' +
                ", relevance=" + relevance +
                ", status=" + status +
                '}';
    }
}