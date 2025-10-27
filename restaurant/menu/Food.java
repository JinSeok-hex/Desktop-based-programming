
package restaurant.menu;

public class Food {
    private String nama;
    private int harga;
    private String kategori;

    public Food(String nama, int harga) {
        this.nama = nama;
        this.harga = harga;
        this.kategori = "food";
    }

    public void showMenu() {
        System.out.println(nama + " - Rp " + harga + " (" + kategori + ")");
    }

    public String getNama() { return nama; }
    public int getHarga() { return harga; }
}

