package com.inventra.api.core.domain.alert;

import com.inventra.api.core.domain.alert.enums.AlertSeverity;
import com.inventra.api.core.domain.kitchen.Kitchen;
import com.inventra.api.core.domain.product.Product;
import com.inventra.api.core.domain.stock.StockBatch;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_alert")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@EntityListeners(AuditingEntityListener.class)
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alert")
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private AlertSeverity severity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_batch")
    @ToString.Exclude
    private StockBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_product")
    @ToString.Exclude
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_kitchen", nullable = false)
    @ToString.Exclude
    private Kitchen kitchen;

    @Column(name = "message", nullable = false, length = 255)
    private String message;

    @Column(name = "is_read", nullable = false)
    private Boolean read = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
