package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;

public record EventOrderCreationResult(BidmartOrder order, boolean created) {
}
