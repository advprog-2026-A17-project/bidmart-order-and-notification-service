package id.ac.ui.cs.advprog.bidmartordernotificationservice.service.order;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class OrderMoney {

    private OrderMoney() {
    }

    public static long toRupiah(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
    }

    public static long toSellerEscrowRupiah(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.setScale(0, RoundingMode.CEILING).longValueExact();
    }
}
