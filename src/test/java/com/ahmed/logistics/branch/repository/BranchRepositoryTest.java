package com.ahmed.logistics.branch.repository;

import com.ahmed.logistics.branch.entity.Branch;
import com.ahmed.logistics.branch.entity.BranchStatus;
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
class BranchRepositoryTest {

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void saveBranch_persistsAndGeneratesId() {
        Branch branch = Branch.builder()
                .name("Dallas North Branch")
                .code("BR-DAL-001")
                .address("100 Northway")
                .city("Dallas")
                .postalCode("75201")
                .phone("+12145550155")
                .email("dallas-north@logistics.com")
                .status(BranchStatus.ACTIVE)
                .build();

        Branch saved = branchRepository.saveAndFlush(branch);

        assertNotNull(saved.getId());
        assertEquals("Dallas North Branch", saved.getName());
        assertEquals("BR-DAL-001", saved.getCode());
        assertEquals("100 Northway", saved.getAddress());
        assertEquals("Dallas", saved.getCity());
        assertEquals("75201", saved.getPostalCode());
        assertEquals("+12145550155", saved.getPhone());
        assertEquals("dallas-north@logistics.com", saved.getEmail());
        assertEquals(BranchStatus.ACTIVE, saved.getStatus());
    }

    @Test
    void findById_returnsBranchWhenExists() {
        Branch branch = Branch.builder()
                .name("Houston East Branch")
                .code("BR-HOU-001")
                .address("200 Eastway")
                .city("Houston")
                .postalCode("77001")
                .phone("+17135550166")
                .email("houston-east@logistics.com")
                .status(BranchStatus.ACTIVE)
                .build();
        Branch saved = branchRepository.saveAndFlush(branch);

        Optional<Branch> found = branchRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("BR-HOU-001", found.get().getCode());
    }

    @Test
    void findByCode_returnsBranchWhenExists() {
        Branch branch = Branch.builder()
                .name("San Antonio Branch")
                .code("BR-SAT-001")
                .address("300 Alamo Plaza")
                .city("San Antonio")
                .postalCode("78201")
                .phone("+12105550177")
                .email("sanantonio@logistics.com")
                .status(BranchStatus.ACTIVE)
                .build();
        branchRepository.saveAndFlush(branch);

        Optional<Branch> found = branchRepository.findByCode("BR-SAT-001");

        assertTrue(found.isPresent());
        assertEquals("San Antonio Branch", found.get().getName());
        assertEquals("BR-SAT-001", found.get().getCode());
    }

    @Test
    void findByCode_returnsEmptyWhenNotFound() {
        Optional<Branch> found = branchRepository.findByCode("NON-EXISTENT");
        assertTrue(found.isEmpty());
    }

    @Test
    void existsByCode_returnsTrueWhenExistsAndFalseOtherwise() {
        assertFalse(branchRepository.existsByCode("BR-ELP-001"));

        Branch branch = Branch.builder()
                .name("El Paso Branch")
                .code("BR-ELP-001")
                .address("400 Border St")
                .city("El Paso")
                .postalCode("79901")
                .phone("+19155550188")
                .email("elpaso@logistics.com")
                .status(BranchStatus.ACTIVE)
                .build();
        branchRepository.saveAndFlush(branch);

        assertTrue(branchRepository.existsByCode("BR-ELP-001"));
    }

    @Test
    void uniqueBranchCodeConstraintEnforced() {
        Branch branch1 = Branch.builder()
                .name("Branch 1")
                .code("DUPLICATE-CODE")
                .address("Address 1")
                .city("City 1")
                .postalCode("11111")
                .phone("+1111111111")
                .email("branch1@logistics.com")
                .status(BranchStatus.ACTIVE)
                .build();
        branchRepository.saveAndFlush(branch1);

        Branch branch2 = Branch.builder()
                .name("Branch 2")
                .code("DUPLICATE-CODE")
                .address("Address 2")
                .city("City 2")
                .postalCode("22222")
                .phone("+2222222222")
                .email("branch2@logistics.com")
                .status(BranchStatus.ACTIVE)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            branchRepository.saveAndFlush(branch2);
        });
    }

    @Test
    void statusPersistedAsStringInDatabase() {
        Branch branch = Branch.builder()
                .name("Fort Worth Branch")
                .code("BR-FTW-001")
                .address("500 Stockyards")
                .city("Fort Worth")
                .postalCode("76101")
                .phone("+18175550199")
                .email("fortworth@logistics.com")
                .status(BranchStatus.ACTIVE)
                .build();
        Branch saved = branchRepository.saveAndFlush(branch);

        Object rawStatus = entityManager.createNativeQuery(
                "SELECT status FROM branches WHERE id = :id"
        ).setParameter("id", saved.getId()).getSingleResult();

        assertEquals("ACTIVE", rawStatus.toString());
    }

    @Test
    void deleteBranch_removesRecordSuccessfully() {
        Branch branch = Branch.builder()
                .name("Temporary Branch")
                .code("BR-TMP-001")
                .address("600 Temp Way")
                .city("Arlington")
                .postalCode("76001")
                .phone("+18175550100")
                .email("temp@logistics.com")
                .status(BranchStatus.ACTIVE)
                .build();
        Branch saved = branchRepository.saveAndFlush(branch);

        branchRepository.delete(saved);
        branchRepository.flush();

        assertFalse(branchRepository.findById(saved.getId()).isPresent());
    }
}
