package com.example.searchengine.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "site_table")
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime statusTime;

    @NotBlank(message = "URL cannot be blank.")
    @Column(nullable = false, unique = true, length = 255)
    private String url;

    @NotBlank(message = "Name cannot be blank.")
    @Column(nullable = false, length = 255)
    private String name;

    @NotBlank(message = "Domain cannot be blank.")
    @Column(name = "domain")
    private String domain;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "group_id")
    private Long groupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_site_id")
    private Site parentSite;

    @OneToMany(mappedBy = "site", cascade =
            CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Page> pages = new ArrayList<>();

    @Column(name = "robots_txt")
    private String robotsTxt;

    @Column(name = "`is_accessible`", columnDefinition = "TINYINT DEFAULT 0")
    private boolean isAccessible;

    @ElementCollection
    private Map<String, String> content;



    public Site() {
        this.content = new HashMap<>();
    }

    public Site(String name, String url, Status status) {
        this();
        setName(name);
        setUrl(url);
        setStatus(status);
        this.robotsTxt = "";
        this.isAccessible = false;
    }

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        if (status == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        this.status = status;
    }

    public LocalDateTime getStatusTime() {
        return statusTime;
    }

    public void setStatusTime(LocalDateTime statusTime) {
        this.statusTime = statusTime;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public List<Page> getPages() {
        return pages;
    }

    public void setPages(List<Page> pages) {
        if (pages == null) {
            throw new IllegalArgumentException("Pages list must not be null");
        }
        this.pages = pages;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL must not be null or empty");
        }
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name must not be null or empty");
        }
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Map<String, String> getContent() {
        return Collections.unmodifiableMap(content);
    }

    public Site getParentSite() {
        return parentSite;
    }

    public void setParentSite(Site parentSite) {
        this.parentSite = parentSite;
    }

    public boolean isAccessible() {
        return isAccessible;
    }

    public String getRobotsTxt() {
        return robotsTxt;
    }

    public void setRobotsTxt(String robotsTxt) {
        this.robotsTxt = robotsTxt;
    }

    public void setAccessible(boolean accessible) {
        this.isAccessible = accessible;
    }

    public void setContent(Map<String, String> content) {
        this.content = content;
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

