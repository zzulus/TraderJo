package jo.replay;

public class Position {
    private int quantity;
    private double avgPrice;

    public void addPosition(int newQuantity, double newPrice) {
        if (quantity == 0) {
            quantity = newQuantity;
            avgPrice = newPrice;
        } else {
            quantity += newQuantity;
            // avgPrice = ;
        }
    }

}
