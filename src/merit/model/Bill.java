package merit.model;

/**
 * Itemised bill for one rental (owned by Member C).
 *
 * <p><b>Composition</b> of the four money components (base / discount / penalty / net):
 * they have no meaning independent of the bill (solid diamond). Pure value object — it
 * holds the numbers, it does not compute them ({@code BillGenerator} does).
 *
 * <p>Money is stored as {@code double} (academic scope) and rounded to 2 dp only for
 * display via {@link #toDetailedString()}.
 */
public class Bill {

    private final String billId;
    private final String rentalId;
    private final int quantity;
    private final double baseFee;
    private final double discount;
    private final double penalty;
    private final double netPayable;
    private final String createdAt;   // ISO timestamp

    public Bill(String billId, String rentalId, int quantity, double baseFee, double discount,
                double penalty, double netPayable, String createdAt) {
        this.billId = billId;
        this.rentalId = rentalId;
        this.quantity = quantity;
        this.baseFee = baseFee;
        this.discount = discount;
        this.penalty = penalty;
        this.netPayable = netPayable;
        this.createdAt = createdAt;
    }

    public String getBillId() { return billId; }
    public String getRentalId() { return rentalId; }
    public int getQuantity() { return quantity; }
    public double getBaseFee() { return baseFee; }
    public double getDiscount() { return discount; }
    public double getPenalty() { return penalty; }
    public double getNetPayable() { return netPayable; }
    public String getCreatedAt() { return createdAt; }

    /** Itemised breakdown — base / discount / penalty / net on separate lines. */
    public String toDetailedString() {
        return String.format(
                "Bill %s (rental %s)%n"
                        + "  Quantity : %d unit(s)%n"
                        + "  Base fee : RM %.2f%n"
                        + "  Discount : RM %.2f%n"
                        + "  Penalty  : RM %.2f%n"
                        + "  ------------------------%n"
                        + "  Net payable : RM %.2f",
                billId, rentalId, quantity, baseFee, discount, penalty, netPayable);
    }
}
