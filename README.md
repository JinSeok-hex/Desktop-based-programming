# 🍱 K-Delights — Java Desktop Restaurant App (CLI + JavaFX)

> Mode **CLI (terminal)** dan **GUI (JavaFX)** tersedia dalam satu proyek.

---

## 📦 Struktur Proyek

```bash
Desktop-based-programming/
├── README.md
└── src
    ├── class
    │   └── restaurant
    │       └── menu
    ├── coupons.txt
    ├── Main.java
    └── restaurant
        └── menu
            ├── Drink.java
            └── Food.java

7 directories, 5 files
```
---
### 🧩 Requirements

- Java JDK 25

- JavaFX SDK 26+
- 👉 Download di sini

---

## 🚀 RUN CLI MODE (TERM)
```bash
# masuk ke folder src
cd src

# compile ke folder class
javac -d class Main.java restaurant/menu/Food.java restaurant/menu/Drink.java

# jalankan program
java -cp class Main

```
---

## 🪟 RUN GUI MODE (JAVAFX)
```bash 

# masuk ke folder GUI app
> cd src/.AppsVersions

# compile dengan JavaFX modules
> javac --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml KDelightsApp.java

# run aplikasi GUI
> java --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml KDelightsApp

```

---
> 💡 CATATAN PENTING
> • Semua struk otomatis tersimpan di file `struk.txt`
> • Pembelian di atas Rp50.000 akan menghasilkan kupon gratis
> • Kupon di-generate sebagai hash SHA-256 unik per transaksi
> • Jika pelanggan belum pernah beli, kupon belum diaktifkan
> • CLI menampilkan prompt interaktif dengan emoji responsif
---

## 👨‍💻 Author

| Info       | Detail                        |
|------------|-------------------------------|
| Developer  | JinSeok / arpan               |
| Project    | Desktop-based-programming     |
| Language   | Java 25 + JavaFX 26           |
| Year       | 2025                          |
