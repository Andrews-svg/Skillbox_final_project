//package searchengine.services;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import searchengine.dao.FieldDao;
//import searchengine.exceptions.CustomNotFoundException;
//import searchengine.models.Field;
//import searchengine.repository.FieldRepository;
//
//import java.math.BigDecimal;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//public class FieldServiceTest {
//
//    @Mock
//    private FieldDao fieldDao;
//
//    @Mock
//    private FieldRepository fieldRepository;
//
//    @InjectMocks
//    private FieldService fieldService;
//
//    private Field field;
//
//    @BeforeEach
//    public void setUp() {
//        MockitoAnnotations.openMocks(this);
//        field = new Field();
//        field.setId(1L);
//        field.setName("title");
//        field.setSelector("title");
//        field.setWeight(BigDecimal.valueOf(1.0f));
//    }
//
//    @Test
//    public void testFindField_Success() {
//        when(fieldRepository.findById(1L)).thenReturn(Optional.of(field));
//
//        Optional<Field> foundField = fieldService.findField(1L);
//
//        assertTrue(foundField.isPresent());
//        assertEquals("title", foundField.get().getName());
//        verify(fieldRepository).findById(1L);
//    }
//
//    @Test
//    public void testFindField_NotFound() {
//        long nonExistentId = 1;
//        CustomNotFoundException exception = assertThrows(CustomNotFoundException.class, () -> {
//            fieldService.findField(nonExistentId);
//        });
//
//        assertEquals("Field not found with id 1", exception.getMessage());
//    }
//
//    @Test
//    public void testSaveField_Success() {
//        fieldService.saveField(field);
//
//        verify(fieldRepository).save(field);
//    }
//
//    @Test
//    public void testSaveField_InvalidField() {
//        Field invalidField = new Field();
//
//        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
//            invalidField.setWeight(BigDecimal.valueOf(-1.0f));
//        });
//
//        assertEquals("Weight must be non-negative", exception.getMessage());
//    }
//
//    @Test
//    public void testDeleteField_Success() {
//        fieldService.deleteField(field);
//
//        verify(fieldRepository).delete(field);
//    }
//
//    @Test
//    public void testUpdateField_Success() {
//        fieldService.updateField(field);
//
//        verify(fieldDao).update(field);
//    }
//
//    @Test
//    public void testDeleteAllFields_Success() {
//        fieldService.deleteAllFields();
//
//        verify(fieldRepository).deleteAll();
//    }
//
//    @Test
//    public void testInitializeFields() {
//        fieldService.initializeFields();
//
//        ArgumentCaptor<Field> fieldCaptor = ArgumentCaptor.forClass(Field.class);
//        verify(fieldRepository, times(2)).save(fieldCaptor.capture());
//
//        assertEquals("title", fieldCaptor.getAllValues().get(0).getName());
//        assertEquals("body", fieldCaptor.getAllValues().get(1).getName());
//    }
//}