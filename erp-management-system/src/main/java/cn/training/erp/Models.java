package cn.training.erp;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class Models {
    private Models() {}

    public record ProductInput(
        @NotBlank @Size(max=40) @Pattern(regexp="[A-Za-z0-9_-]+", message="编码只能包含字母、数字、下划线和短横线") String sku,
        @NotBlank @Size(max=120) String name,
        @NotBlank @Size(max=40) String category,
        @NotBlank @Size(max=20) String unit,
        @NotNull @DecimalMin("0") @Digits(integer=10, fraction=2) BigDecimal purchasePrice,
        @NotNull @DecimalMin("0") @Digits(integer=10, fraction=2) BigDecimal salePrice,
        @NotNull @DecimalMin("0") @Digits(integer=10, fraction=3) BigDecimal minStock) {}

    public record PartnerInput(
        @NotBlank @Size(max=100) String name,
        @Pattern(regexp="SUPPLIER|CUSTOMER") @NotNull String type,
        @NotNull @Size(max=60) String contact,
        @NotNull @Size(max=40) String phone) {}

    public record LineInput(
        @NotNull @Positive Long productId,
        @NotNull @DecimalMin("0.001") @Digits(integer=9, fraction=3) BigDecimal quantity,
        @NotNull @DecimalMin("0") @Digits(integer=10, fraction=2) BigDecimal unitPrice) {}

    public record OrderInput(
        @NotNull @Pattern(regexp="PURCHASE|SALE") String type,
        @NotNull @Positive Long partnerId,
        @NotNull LocalDate businessDate,
        @NotNull @Size(max=500) String note,
        @NotEmpty @Size(max=100) List<@Valid LineInput> lines) {}
}
