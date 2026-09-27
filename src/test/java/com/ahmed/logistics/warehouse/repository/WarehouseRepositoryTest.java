package com.ahmed.logistics.warehouse.repository;

import com.ahmed.logistics.warehouse.entity.Warehouse;
import com.ahmed.logistics.warehouse.entity.WarehouseStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class WarehouseRepositoryTest {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void saveWarehouse_persistsAndGeneratesId() {
        Warehouse warehouse = Warehouse.builder()
                .name("Dallas Regional Warehouse")
                .address("500 Industrial Blvd")
                .city("Dallas")
                .postalCode("75201")
                .phone("+12145550100")
                .email("dallas-wh@logistics.com")
                .status(WarehouseStatus.ACTIVE)
                .build();

        Warehouse saved = warehouseRepository.saveAndFlush(warehouse);

        assertNotNull(saved.getId());
        assertEquals("Dallas Regional Warehouse", saved.getName());
        assertEquals("500 Industrial Blvd", saved.getAddress());
        assertEquals("Dallas", saved.getCity());
        assertEquals("75201", saved.getPostalCode());
        assertEquals("+12145550100", saved.getPhone());
        assertEquals("dallas-wh@logistics.com", saved.getEmail());
        assertEquals(WarehouseStatus.ACTIVE, saved.getStatus());
    }

    @Test
    void findByName_returnsWarehouseWhenExists() {
        Warehouse warehouse = Warehouse.builder()
                .name("Austin Hub")
                .address("123 Tech Ridge")
                .city("Austin")
                .postalCode("78701")
                .phone("+15125550123")
                .email("austin-wh@logistics.com")
                .status(WarehouseStatus.ACTIVE)
                .build();
        warehouseRepository.saveAndFlush(warehouse);

        Optional<Warehouse> found = warehouseRepository.findByName("Austin Hub");

        assertTrue(found.isPresent());
        assertEquals("Austin Hub", found.get().getName());
        assertEquals("Austin", found.get().getCity());
    }

    @Test
    void findByName_returnsEmptyWhenNotFound() {
        Optional<Warehouse> found = warehouseRepository.findByName("NonExistentWarehouse");
        assertTrue(found.isEmpty());
    }

    @Test
    void existsByName_returnsTrueWhenExistsAndFalseOtherwise() {
        assertFalse(warehouseRepository.existsByName("Houston Central"));

        Warehouse warehouse = Warehouse.builder()
                .name("Houston Central")
                .address("900 Port Way")
                .city("Houston")
                .postalCode("77001")
                .phone("+17135550188")
                .email("houston-wh@logistics.com")
                .status(WarehouseStatus.ACTIVE)
                .build();
        warehouseRepository.saveAndFlush(warehouse);

        assertTrue(warehouseRepository.existsByName("Houston Central"));
    }

    @Test
    void uniqueWarehouseNameConstraintEnforced() {
        Warehouse wh1 = Warehouse.builder()
                .name("Duplicate Name Test")
                .address("100 Main St")
                .city("Miami")
                .postalCode("33101")
                .phone("+13055550111")
                .email("miami1@logistics.com")
                .status(WarehouseStatus.ACTIVE)
                .build();
        warehouseRepository.saveAndFlush(wh1);

        Warehouse wh2 = Warehouse.builder()
                .name("Duplicate Name Test")
                .address("200 Ocean Dr")
                .city("Miami")
                .postalCode("33139")
                .phone("+13055550222")
                .email("miami2@logistics.com")
                .status(WarehouseStatus.ACTIVE)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            warehouseRepository.saveAndFlush(wh2);
        });
    }

    @Test
    void statusPersistedAsStringInDatabase() {
        Warehouse warehouse = Warehouse.builder()
                .name("Seattle Sorting Center")
                .address("770 Pine St")
                .city("Seattle")
                .postalCode("98101")
                .phone("+12065550144")
                .email("seattle-wh@logistics.com")
                .status(WarehouseStatus.ACTIVE)
                .build();
        Warehouse saved = warehouseRepository.saveAndFlush(warehouse);

        Object rawStatus = entityManager.createNativeQuery(
                "SELECT status FROM warehouses WHERE id = :id"
        ).setParameter("id", saved.getId()).getSingleResult();

        assertEquals("ACTIVE", rawStatus.toString());
    }

    @Test
    void deleteWarehouse_removesRecordSuccessfully() {
        Warehouse warehouse = Warehouse.builder()
                .name("Temporary Storage")
                .address("800 Temp Rd")
                .city("Denver")
                .postalCode("80201")
                .phone("+13035550166")
                .email("denver-temp@logistics.com")
                .status(WarehouseStatus.ACTIVE)
                .build();
        Warehouse saved = warehouseRepository.saveAndFlush(warehouse);

        warehouseRepository.delete(saved);
        warehouseRepository.flush();

        assertFalse(warehouseRepository.findById(saved.getId()).isPresent());
    }
}
