//package searchengine.indexing;
//
//import com.example.searchengine.indexing.IndexingService;
//import com.example.searchengine.indexing.SiteManager;
//import com.example.searchengine.models.Status;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import java.util.Arrays;
//import java.util.concurrent.BrokenBarrierException;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.CyclicBarrier;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//
//@ExtendWith(MockitoExtension.class)
//public class IndexingServiceTest {
//
//    @InjectMocks
//    private IndexingService indexingService;
//
//    @Mock
//    private SiteManager siteManager;
//
//    private final AtomicInteger activeIndexingThreads = new AtomicInteger(0);
//
//    @BeforeEach
//    public void setup() {
//
//    }
//
//    @Test
//    public void testFinishIndexingSuccess() {
//        indexingService.startIndexing(1L, false);
//        indexingService.finishIndexing(true);
//
//        assertEquals(Status.INDEXED, indexingService.getCurrentStatus());
//    }
//
//
//    @Test
//    public void testFinishIndexingFailure() {
//        indexingService.startIndexing(1L, false);
//        sleep(500);
//        indexingService.finishIndexing(false);
//
//        assertEquals(Status.FAILED, indexingService.getCurrentStatus());
//    }
//
//    private void sleep(int millis) {
//        try {
//            Thread.sleep(millis);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }
//
//
//    @Test
//    public void testParallelIndexing() throws InterruptedException {
//        Thread[] threads = new Thread[10];
//        for (int i = 0; i < threads.length; i++) {
//            int finalI = i;
//            threads[i] = new Thread(() -> indexingService.startIndexing(finalI + 1L, false));
//        }
//
//        Arrays.stream(threads).forEach(Thread::start);
//        Arrays.stream(threads).forEach(t -> {
//            try {
//                t.join();
//            } catch (InterruptedException ignored) {}
//        });
//
//        assertTrue(activeIndexingThreads.get() == 0);
//    }
//
//
//    @Test
//    public void testStopIndexingWhenActive() {
//        indexingService.startIndexing(1L, false);
//        indexingService.stopIndexing(1L);
//
//        assertEquals(Status.INDEXED, indexingService.getCurrentStatus());
//    }
//
//    @Test
//    public void testStopIndexingWhenInactive() {
//        indexingService.stopIndexing(1L);
//
//        assertEquals(Status.PENDING, indexingService.getCurrentStatus());
//    }
//
//
//    @Test
//    public void testMultiThreadedIndexing() throws InterruptedException {
//        CountDownLatch latch = new CountDownLatch(10);
//        CyclicBarrier barrier = new CyclicBarrier(10);
//
//        Thread[] threads = new Thread[10];
//        for (int i = 0; i < threads.length; i++) {
//            int finalI = i;
//            threads[i] = new Thread(() -> {
//                try {
//                    barrier.await();
//                } catch (BrokenBarrierException | InterruptedException e) {
//                    e.printStackTrace();
//                }
//                indexingService.startIndexing((long)finalI, false);
//                latch.countDown();
//            });
//        }
//        for (Thread thread : threads) {
//            thread.start();
//        }
//        latch.await();
//        indexingService.finishIndexing(true);
//        assertEquals(Status.INDEXED, indexingService.getCurrentStatus());
//    }
//}
