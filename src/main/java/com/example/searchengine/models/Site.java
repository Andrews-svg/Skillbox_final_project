package com.example.searchengine.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import java.time.LocalDateTime;
import java.util.*;


@Indexed
@Getter
@Setter
@RequiredArgsConstructor
@Entity
@Table(name = "site")
@NamedQueries({
        @NamedQuery(name = "Site.findByStatus",
                query = "SELECT s FROM Site s WHERE s.status = :status"),
        @NamedQuery(name = "Site.findByUrl",
                query = "SELECT s FROM Site s WHERE s.url = :url"),
        @NamedQuery(name = "Site.findAll",
                query = "SELECT s FROM Site s")
})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Site {

    @NotNull
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Status status;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime statusTime;

    @NotBlank(message = "URL cannot be blank.")
    @Column(nullable = false, unique = true, length = 255)
    private String url;

    @FullTextField(analyzer = "russian")
    @KeywordField(name = "name_keyword")
    @NotBlank(message = "Name cannot be blank.")
    @Column(nullable = false, length = 255)
    private String name;

    @NotBlank(message = "Domain cannot be blank.")
    @Column(name = "domain")
    private String domain;

    @NotNull
    @Column(name = "last_error")
    private String lastError;

    @NotNull
    @Column(name = "group_id")
    private Long groupId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_site_id")
    private Site parentSite;

    @NotNull
    @OneToMany(mappedBy = "site", cascade =
            CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Page> pages;

    @NotNull
    @Column(name = "robots_txt")
    private String robotsTxt;

    @NotNull
    @Column(name = "`is_accessible`", columnDefinition = "TINYINT DEFAULT 0")
    private boolean isAccessible;

    @NotNull
    @ElementCollection
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    @FullTextField(analyzer = "russian")
    private Map<String, String> contentRus;

    @NotNull
    @ElementCollection
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    @FullTextField(analyzer = "english")
    private Map<String, String> contentEng;




    public void addPage(Page page) {
        if (!pages.contains(page)) {
            pages.add(page);
            page.setSite(this);
        }
    }


    @PrePersist
    protected void onCreate() {
        if (this.statusTime == null) {
            this.statusTime = LocalDateTime.now();
        }
    }


    public void updateStatusTime() {
        this.statusTime = LocalDateTime.now();
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Site other)) return false;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }


    @Override
    public String toString() {
        return "Site{" +
                "id=" + id +
                ", status=" + (status != null ? status : "N/A") +
                ", statusTime=" + statusTime +
                ", lastError='" + lastError + '\'' +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", parentSiteId=" + (parentSite != null ? parentSite.getId() : "N/A") +
                ", accessible=" + isAccessible +
                '}';
    }
}

