//package searchengine.services;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import jakarta.persistence.EntityManager;
//import searchengine.services.IndexingHistoryService;
//import searchengine.indexing.IndexingHistoryRecord;
//
//import java.util.List;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class IndexingHistoryServiceTest {
//
//    @Mock
//    private EntityManager entityManager;
//
//    @InjectMocks
//    private IndexingHistoryService indexingHistoryService;
//
//    @BeforeEach
//    public void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    public void testStartIndexingSession() {
//        UUID sessionId = indexingHistoryService.startIndexingSession();
//        assertNotNull(sessionId, "Session ID should not be null");
//    }
//
//    @Test
//    public void testStartIndexingSessionIsUnique() {
//        UUID sessionId1 = indexingHistoryService.startIndexingSession();
//        UUID sessionId2 = indexingHistoryService.startIndexingSession();
//        assertNotEquals(sessionId1, sessionId2, "Each session ID should be unique");
//    }
//
//
//    @Test
//    public void testAddRecord() {
//        UUID sessionId = indexingHistoryService.startIndexingSession();
//        IndexingHistoryRecord record = new IndexingHistoryRecord();
//
//        indexingHistoryService.addRecord(sessionId, record);
//        List<IndexingHistoryRecord> history = indexingHistoryService.getHistory(sessionId);
//
//        assertEquals(1, history.size(), "History should contain one record");
//        assertEquals(record, history.get(0), "The record in history should match the added record");
//    }
//
//    @Test
//    public void testAddRecordToNonExistentSession() {
//        UUID nonExistentSessionId = UUID.randomUUID();
//        IndexingHistoryRecord record = new IndexingHistoryRecord();
//        indexingHistoryService.addRecord(nonExistentSessionId, record);
//        assertTrue(indexingHistoryService.getHistory(nonExistentSessionId).isEmpty(), "History should still be empty");
//    }
//
//    @Test
//    public void testGetHistoryWithNoRecords() {
//        UUID sessionId = UUID.randomUUID();
//        List<IndexingHistoryRecord> history = indexingHistoryService.getHistory(sessionId);
//        assertTrue(history.isEmpty(), "History should be empty for a new session");
//    }
//
//    @Test
//    public void testCompleteNonExistentSession() {
//        UUID nonExistentSessionId = UUID.randomUUID();
//        indexingHistoryService.completeIndexingSession(nonExistentSessionId, true);
//    }
//
//    @Test
//    public void testCompleteIndexingSessionAndPersistRecords() {
//        UUID sessionId = indexingHistoryService.startIndexingSession();
//        IndexingHistoryRecord record = new IndexingHistoryRecord();
//        indexingHistoryService.addRecord(sessionId, record);
//
//        indexingHistoryService.completeIndexingSession(sessionId, true);
//
//        verify(entityManager, times(1)).persist(record);
//        assertTrue(indexingHistoryService.getHistory(sessionId).isEmpty(), "History should be cleared after completion");
//    }
//
//    @Test
//    public void testCompleteIndexingSessionWithoutSaving() {
//        UUID sessionId = indexingHistoryService.startIndexingSession();
//        IndexingHistoryRecord record = new IndexingHistoryRecord();
//        indexingHistoryService.addRecord(sessionId, record);
//
//        indexingHistoryService.completeIndexingSession(sessionId, false);
//
//        verify(entityManager, never()).persist(any(IndexingHistoryRecord.class));
//        assertTrue(indexingHistoryService.getHistory(sessionId).isEmpty(), "History should be cleared after completion");
//    }
//}