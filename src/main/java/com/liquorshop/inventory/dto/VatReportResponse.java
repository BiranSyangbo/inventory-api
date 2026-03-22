package com.liquorshop.inventory.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
public class VatReportResponse {
    private BigDecimal totalPurchaseVat;
    private BigDecimal totalSalesVat;
    /** Sales VAT minus Purchase VAT — amount payable to tax authority */
    private BigDecimal netVatLiability;
    private List<VatPurchaseLine> purchaseLines;
    private List<VatSalesLine> salesLines;
}
