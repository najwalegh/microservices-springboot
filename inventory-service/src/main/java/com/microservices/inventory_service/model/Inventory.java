package com.microservices.inventory_service.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name="t_inventory")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Inventory {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;
    private String skuCode;
    private Integer quantity;

    public Long getId() {
        return id;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public Integer getQuantity() {
        return quantity;
    }
}
