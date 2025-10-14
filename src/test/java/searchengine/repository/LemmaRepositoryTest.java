//package searchengine.repository;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.dao.DataIntegrityViolationException;
//import searchengine.models.Lemma;
//import searchengine.models.Status;
//
//@DataJpaTest
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//public class LemmaRepositoryTest {
//
//    @Autowired
//    private LemmaRepository lemmaRepository;
//
//    private Lemma lemma;
//
//    @BeforeEach
//    public void setUp() {
//        lemma = new Lemma("testLemma", 1L, 0L, Status.INDEXING);
//    }
//
//    @Test
//    public void testSaveLemma() {
//        Lemma savedLemma = lemmaRepository.save(lemma);
//
//        assertThat(savedLemma).isNotNull();
//        assertThat(savedLemma.getId()).isNotNull();
//        assertThat(savedLemma.getLemma()).isEqualTo("testLemma");
//        assertThat(savedLemma.getSiteId()).isEqualTo(1L);
//        assertThat(savedLemma.getStatus()).isEqualTo(Status.INDEXING);
//    }
//
//    @Test
//    public void testSaveLemmaWithManualId() {
//        Lemma lemmaWithManualId = new Lemma("example", 10L, 1L, Status.INDEXING);
//
//        assertThatThrownBy(() -> lemmaRepository.save(lemmaWithManualId))
//                .isInstanceOf(DataIntegrityViolationException.class);
//    }
//
//    @Test
//    public void testFindAllByLemma() {
//        lemmaRepository.save(lemma);
//
//        assertThat(lemmaRepository.findAllByLemma("testLemma"))
//                .extracting(Lemma::getLemma)
//                .containsOnly("testLemma");
//    }
//
//    @Test
//    public void testFindAllBySiteId() {
//        lemmaRepository.save(lemma);
//
//        assertThat(lemmaRepository.findAllBySiteId(1L))
//                .extracting(Lemma::getSiteId)
//                .containsOnly(1L);
//    }
//
//    @Test
//    public void testFindByLemmaAndSiteId() {
//        lemmaRepository.save(lemma);
//        assertThat(lemmaRepository.findByLemmaAndSiteId("testLemma", 1L))
//                .extracting(Lemma::getLemma, Lemma::getSiteId)
//                .containsExactly(
//                        org.assertj.core.groups.Tuple.tuple("testLemma", 1L)
//                );
//    }
//
//    @Test
//    public void testFindByNonExistentLemma() {
//        assertThat(lemmaRepository.findByLemmaAndSiteId("nonExistentLemma", 1L)).isEmpty();
//    }
//
//    @Test
//    public void testSaveDuplicateLemma() {
//        lemmaRepository.save(lemma);
//
//        Lemma duplicateLemma = new Lemma("testLemma", 1L, 0L, Status.INDEXING);
//
//        assertThatThrownBy(() -> lemmaRepository.save(duplicateLemma))
//                .isInstanceOf(DataIntegrityViolationException.class);
//    }
//}