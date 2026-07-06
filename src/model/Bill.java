package model;

public class Bill {
    private double baseFee;
    private double discount;
    private double lateFee;
    private double damageFee;
    private double netPayable;

    public Bill(Rental rental) {
        Equipment eq = rental.getEquipment();
        User user = rental.getUser();

        this.baseFee = eq.calculateBaseFee(rental.getDays());
        this.discount = baseFee * user.getDiscountRate();
        this.lateFee = eq.calculateLateFee(rental.getDaysLate());
        this.damageFee = eq.calculateDamageFee(rental.getDamageLevel());
        this.netPayable = baseFee - discount + lateFee + damageFee;
    }

    public void printItemized() {
        System.out.println("---- BILL ----");
        System.out.println("Base fee:    RM" + baseFee);
        System.out.println("Discount:   -RM" + discount);
        System.out.println("Late fee:   +RM" + lateFee);
        System.out.println("Damage fee: +RM" + damageFee);
        System.out.println("Net payable: RM" + netPayable);
    }
        public double getBaseFee() { return baseFee; }
        public double getDiscount() { return discount; }
        public double getLateFee() { return lateFee; }
        public double getDamageFee() { return damageFee; }
        public double getNetPayable() { return netPayable; }
}