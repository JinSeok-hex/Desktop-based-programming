
package restaurant.menu;

public class Drink {
    private String nama;
    private int harga;
    private String kategori;

    public Drink(String nama, int harga) {
        this.nama = nama;
        this.harga = harga;
        this.kategori = "drink";
    }

    public void showMenu() {
        System.out.println(nama + " - Rp " + harga + " (" + kategori + ")");
    }

    public String getNama() { return nama; }
    public int getHarga() { return harga; }
}

