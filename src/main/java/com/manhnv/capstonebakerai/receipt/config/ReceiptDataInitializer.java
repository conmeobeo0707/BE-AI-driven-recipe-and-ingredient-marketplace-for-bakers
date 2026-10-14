package com.manhnv.capstonebakerai.receipt.config;

import com.manhnv.capstonebakerai.receipt.entity.ReceiptCategory;
import com.manhnv.capstonebakerai.receipt.enums.ReceiptCategoryType;
import com.manhnv.capstonebakerai.receipt.repository.ReceiptCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReceiptDataInitializer implements ApplicationRunner {

    private final ReceiptCategoryRepository categoryRepository;


    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        seed("Thu tiền bán hàng", ReceiptCategoryType.SALE);
        seed("Thu tiền đối soát COD", ReceiptCategoryType.SETTLEMENT);
        seed("Thu khác", ReceiptCategoryType.OTHER);
    }

    private void seed(String name, ReceiptCategoryType type) {
        if (!categoryRepository.existsByType(type)) {
            ReceiptCategory category = ReceiptCategory.builder()
                    .name(name)
                    .type(type)
                    .build();
            categoryRepository.save(category);
        }
    }
}
