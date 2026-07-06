import bridge.*;
import model.*;

public class TestBill {
    public static void main(String[] args) {
        Equipment camera = new MediaEquipment(
                "MEDIA-001", "Canon DSLR", 30.0, 5000.0, new PercentLatePricing());

        User chong = new Student("S1002", "Chong", true);  // final-year, 10% discount

        Rental rental = new Rental(chong, camera, 14, 6, "MINOR");
        // 租14天, 迟还6天, 轻微损坏

        Bill bill = new Bill(rental);
        bill.printItemized();
    }
}