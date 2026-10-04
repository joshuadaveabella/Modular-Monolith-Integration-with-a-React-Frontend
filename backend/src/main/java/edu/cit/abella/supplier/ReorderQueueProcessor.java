package edu.cit.abella.supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class ReorderQueueProcessor {

    private static final Logger log = LoggerFactory.getLogger(ReorderQueueProcessor.class);

    private final SupplierOrderRepository orderRepository;
    private final SupplierSkuMappingRepository skuMappingRepository;
    private final SupplierOrderServiceImpl supplierOrderService;

    ReorderQueueProcessor(SupplierOrderRepository orderRepository,
                          SupplierSkuMappingRepository skuMappingRepository,
                          SupplierOrderServiceImpl supplierOrderService) {
        this.orderRepository = orderRepository;
        this.skuMappingRepository = skuMappingRepository;
        this.supplierOrderService = supplierOrderService;
    }

    @Scheduled(fixedDelayString = "${legacysupply.reorder-queue-interval-ms:30000}")
    void processPendingReorders() {
        List<SupplierOrderEntity> pending = orderRepository.findAllByStatus(SupplierOrderStatus.PENDING);
        if (pending.isEmpty()) return;

        log.info("Reorder queue: {} order(s) still PENDING, attempting resend", pending.size());

        for (SupplierOrderEntity order : pending) {
            skuMappingRepository.findByProductId(order.getProductId()).ifPresentOrElse(
                    mapping -> supplierOrderService.attemptSubmission(order, mapping.getSupplierSku()),
                    () -> log.error("Reorder {} has no SKU mapping for product {} - cannot send",
                            order.getBuyerRef(), order.getProductId())
            );
        }
    }
}
