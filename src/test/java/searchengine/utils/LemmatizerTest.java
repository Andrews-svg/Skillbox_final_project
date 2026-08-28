package searchengine.utils;

import com.example.searchengine.utils.Lemmatizer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LemmatizerTest {

    @Test
    public void testGetLemmasFrequency() {
        Lemmatizer lemmatizer = new Lemmatizer();
        Map<String, Integer> freq = lemmatizer.getLemmasFrequency("Мама мыла раму");
        System.out.println(freq);
        assertTrue(freq.containsKey("мама"));
        assertTrue(freq.containsKey("мыть"));
        assertTrue(freq.containsKey("рама"));
        assertEquals(1, freq.get("мама"));
    }
}