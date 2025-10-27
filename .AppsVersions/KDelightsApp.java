import javafx.application.Application;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.input.KeyCode;
import javafx.beans.property.ReadOnlyStringWrapper;
import java.awt.Desktop;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class KDelightsApp extends Application {

    // ---------- Helper & Data Models ----------
    static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private boolean isDrink(String name){
    if(name == null) return false;
    name = name.trim().toLowerCase();
    return List.of("soju","makgeolli","sikhye","omija tea").contains(name);
}


    static class MenuItemModel {
        String name;
        int price;
        String category; 
        MenuItemModel(String n, int p, String c){ name=n; price=p; category=c; }
        public String toString(){ return name + " - Rp " + String.format("%,d", price); }
    }

    static class CartLine {
        MenuItemModel item;
        Integer qty;
        CartLine(MenuItemModel it, Integer q){ item = it; qty = q; }
        public String getName(){ return item.name; }
        public Integer getQty(){ return qty; }
        public Integer getPrice(){ return item.price; }
        public Integer getTotal(){ return item.price * qty; }
    }

    static class Wallet {
        String name;
        long balance;
        String passwordHash;
        Wallet(String name, long balance, String plainPwd){
            this.name = name; this.balance = balance; this.passwordHash = sha256(plainPwd);
        }
        boolean verify(String pwd){ return sha256(pwd).equalsIgnoreCase(passwordHash); }
        boolean canPay(long amt){ return balance >= amt; }
        void deduct(long amt){ balance -= amt; }
    }

    // ---------- Persistence ----------
    final File couponFile = new File("coupons.txt");
    Map<String, Long> coupons = new HashMap<>();

    Map<String, Long> loadCoupons(File f){
        Map<String, Long> map = new HashMap<>();
        if (!f.exists()) return map;
        try(BufferedReader r = new BufferedReader(new FileReader(f))){
            String line;
            while((line = r.readLine()) != null){
                line = line.trim();
                if(line.isEmpty()) continue;
                String[] parts = line.split(",",2);
                if(parts.length==2) map.put(parts[0], Long.parseLong(parts[1]));
            }
        } catch(Exception e){
            System.out.println("Warning: failed to read coupons: "+e.getMessage());
        }
        return map;
    }

    void saveCoupon(File f, String hash, long value){
        try(FileWriter fw = new FileWriter(f,true)){
            fw.write(hash + "," + value + System.lineSeparator());
            coupons.put(hash, value);
        } catch(Exception e){
            System.out.println("Warning: save coupon failed: "+e.getMessage());
        }
    }

    void saveStrukToFile(String text){
        try(FileWriter fw = new FileWriter("struk.txt", false)){
            fw.write(text);
        } catch(Exception e){
            System.out.println("Warning: save struk failed: "+e.getMessage());
        }
    }

    // ---------- App Data ----------
    ObservableList<MenuItemModel> foodList = FXCollections.observableArrayList();
    ObservableList<MenuItemModel> drinkList = FXCollections.observableArrayList();
    ObservableList<CartLine> cart = FXCollections.observableArrayList();

    Wallet[] wallets;

    // ---------- UI Controls ----------
    ListView<MenuItemModel> lvFoods, lvDrinks;
    TableView<CartLine> tvCart;
    Label lblSubtotal, lblDiscount, lblTax, lblService, lblTotal;
    TextField tfQty, tfCommand;

    // ---------- Business constants ----------
    final double TAX_RATE = 0.10;
    final long SERVICE_FEE = 20_000L;

    // ---------- Utility ----------
    long computeSubtotal(){
        long s = 0;
        for(CartLine c : cart) s += c.getTotal();
        return s;
    }

    // ---------- App start ----------
    @Override
    public void start(Stage primaryStage) {
        // Load persisted coupons
        coupons = loadCoupons(couponFile);

        // Init data (4 foods, 4 drinks)
        foodList.addAll(
            new MenuItemModel("Bibimbap", 30000, "Makanan"),
            new MenuItemModel("Kimchi", 12000, "Makanan"),
            new MenuItemModel("Tteokbokki", 25000, "Makanan"),
            new MenuItemModel("Bulgogi", 35000, "Makanan")
        );
        drinkList.addAll(
            new MenuItemModel("Soju", 35000, "Minuman"),
            new MenuItemModel("Makgeolli", 30000, "Minuman"),
            new MenuItemModel("Sikhye", 15000, "Minuman"),
            new MenuItemModel("Omija Tea", 20000, "Minuman")
        );

        wallets = new Wallet[]{
            new Wallet("myaccount", 5_000_000L, "myaccount"),
            new Wallet("mybank", 12_000_000L, "mybank"),
            new Wallet("mysecret", 100_000_000L, "mysecret")
        };

        // Top: ASCII welcome (multiline)
        Label ascii = new Label(
" _  __     ____       _ _       _     _       \n" +
"| |/ /    |  _ \\  ___| (_) __ _| |__ | |_ ___ \n" +
"| ' /_____| | | |/ _ \\ | |/ _` | '_ \\| __/ __|\n" +
"| . \\_____| |_| |  __/ | | (_| | | | | |_\\__ \\\n" +
"|_|\\_\\    |____/ \\___|_|_|\\__, |_| |_|\\__|___/\n" +
"                          |___/               \n" +
"                                             restaurant\n"
        );
        ascii.setFont(Font.font("Monospaced", 12));

        // Left: Menus
        lvFoods = new ListView<>(foodList);
        lvDrinks = new ListView<>(drinkList);
        lvFoods.setPrefWidth(240); lvDrinks.setPrefWidth(240);
        VBox vbFoods = new VBox(new Label("🍲 Menu Makanan"), lvFoods);
        VBox vbDrinks = new VBox(new Label("🥤 Menu Minuman"), lvDrinks);
        vbFoods.setSpacing(6); vbDrinks.setSpacing(6);

        // Center: Cart Table
        tvCart = new TableView<>(cart);
        TableColumn<CartLine, String> colName = new TableColumn<>("Menu");
        colName.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getName()));
        colName.setPrefWidth(180);
        TableColumn<CartLine, Integer> colQty = new TableColumn<>("Qty");
        colQty.setCellValueFactory(new PropertyValueFactory<>("qty"));
        colQty.setPrefWidth(60);
        TableColumn<CartLine, Integer> colPrice = new TableColumn<>("Harga");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colPrice.setCellFactory(tc -> new TableCell<CartLine,Integer>(){
            @Override protected void updateItem(Integer v, boolean empty){
                super.updateItem(v, empty);
                setText(empty ? null : String.format("Rp %,d", v));
            }
        });
        colPrice.setPrefWidth(110);
        TableColumn<CartLine, Integer> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(tc -> new TableCell<CartLine,Integer>(){
            @Override protected void updateItem(Integer v, boolean empty){
                super.updateItem(v, empty);
                setText(empty ? null : String.format("Rp %,d", v));
            }
        });
        colTotal.setPrefWidth(140);
        tvCart.getColumns().addAll(colName, colQty, colPrice, colTotal);
        tvCart.setPrefHeight(240);

        // Controls to add item
        tfQty = new TextField("1"); tfQty.setPrefWidth(60);
        Button btnAddFood = new Button("Tambah Makanan");
        Button btnAddDrink = new Button("Tambah Minuman");
        HBox hbAdd = new HBox(8, new Label("Qty:"), tfQty, btnAddFood, btnAddDrink);
        hbAdd.setAlignment(Pos.CENTER_LEFT);

        btnAddFood.setOnAction(e -> addSelectedItemToCart(lvFoods));
        btnAddDrink.setOnAction(e -> addSelectedItemToCart(lvDrinks));

        // Summary labels
        lblSubtotal = new Label("Subtotal: Rp 0");
        lblDiscount = new Label("Diskon: Rp 0");
        lblTax = new Label("Pajak (10%): Rp 0");
        lblService = new Label("Biaya layanan: Rp " + String.format("%,d", SERVICE_FEE));
        lblTotal = new Label("TOTAL: Rp 0");
        lblTotal.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        VBox vbSummary = new VBox(6, lblSubtotal, lblDiscount, lblTax, lblService, lblTotal);
        vbSummary.setPadding(new Insets(10));

        // Payment controls
        ComboBox<String> cbWallets = new ComboBox<>();
        for(Wallet w : wallets) cbWallets.getItems().add(w.name + " (Saldo: Rp " + String.format("%,d", w.balance) + ")");
        cbWallets.getSelectionModel().selectFirst();
        PasswordField pfPassword = new PasswordField();
        pfPassword.setPromptText("Masukkan kata kunci wallet");
        Button btnPay = new Button("Bayar Sekarang");
        Button btnUseCoupon = new Button("Gunakan Kupon");

        VBox vbPay = new VBox(8, new Label("Metode Pembayaran"), cbWallets, pfPassword, btnPay, btnUseCoupon);
        vbPay.setPadding(new Insets(10));

        // Command bar (free text)
        tfCommand = new TextField();
        tfCommand.setPromptText("Perintah bebas (contoh: add kimchi 2  |  pay  |  remove 1  |  clear )");
        tfCommand.setOnKeyPressed(ev -> {
            if(ev.getCode() == KeyCode.ENTER) {
                handleCommand(tfCommand.getText().trim(), cbWallets, pfPassword);
                tfCommand.clear();
            }
        });
        Button btnExec = new Button("Exec");
        btnExec.setOnAction(e -> { handleCommand(tfCommand.getText().trim(), cbWallets, pfPassword); tfCommand.clear(); });

        HBox hbCmd = new HBox(8, tfCommand, btnExec);
        hbCmd.setPadding(new Insets(8));

        // Layout assemble
        VBox left = new VBox(10, vbFoods, vbDrinks);
        left.setPadding(new Insets(12));

        VBox center = new VBox(10, tvCart, hbAdd, vbSummary);
        center.setPadding(new Insets(12));

        VBox right = new VBox(10, vbPay, new Label("Perintah cepat:"), hbCmd);
        right.setPadding(new Insets(12));

        BorderPane root = new BorderPane();
        root.setTop(ascii);
        BorderPane.setAlignment(ascii, Pos.CENTER);
        root.setLeft(left);
        root.setCenter(center);
        root.setRight(right);

        // Button actions
        btnPay.setOnAction(e -> doPayment(cbWallets, pfPassword));
        btnUseCoupon.setOnAction(e -> {
            TextInputDialog td = new TextInputDialog();
            td.setTitle("Gunakan Kupon");
            td.setHeaderText("Masukkan hash kupon Anda");
            td.setContentText("Kupon:");
            Optional<String> res = td.showAndWait();
            res.ifPresent(h -> {
                if(coupons.containsKey(h.trim())){
                    long v = coupons.get(h.trim());
                    // apply coupon to cart by storing it temporarily as discount in UI
                    applyCouponValue(v);
                    showAlert(Alert.AlertType.INFORMATION, "Kupon diterima", "Kupon valid. Nilai: Rp " + String.format("%,d", v));
                } else showAlert(Alert.AlertType.ERROR, "Kupon tidak valid", "Hash kupon tidak ditemukan.");
            });
        });

        // Scene & show
        Scene scene = new Scene(root, 1100, 640);
        primaryStage.setScene(scene);
        primaryStage.setTitle("K-Delights (Desktop)");
        primaryStage.show();

        // initial refresh
        refreshSummary();
    }

    // ---------- UI Actions ----------
    void addSelectedItemToCart(ListView<MenuItemModel> lv){
        MenuItemModel sel = lv.getSelectionModel().getSelectedItem();
        if(sel == null){
            showAlert(Alert.AlertType.WARNING, "Tidak ada item dipilih", "Silakan pilih menu terlebih dahulu.");
            return;
        }
        int q;
        try { q = Math.max(1, Integer.parseInt(tfQty.getText().trim())); } catch(Exception e){ q = 1; }
        // if same item exists in cart, increase qty
        for(CartLine line : cart){
            if(line.item.name.equalsIgnoreCase(sel.name)){
                line.qty = line.qty + q;
                tvCart.refresh();
                refreshSummary();
                return;
            }
        }
        cart.add(new CartLine(sel, q));
        refreshSummary();
    }

    void refreshSummary(){
        long subtotal = computeSubtotal();
        long discount = 0;
        // discount (10%) if subtotal > 100k
        if(subtotal > 100_000) discount = Math.round(subtotal * 0.10);
        double after = subtotal - discount;
        double tax = after * TAX_RATE;
        double total = after + tax + SERVICE_FEE;
        lblSubtotal.setText("Subtotal: Rp " + String.format("%,d", subtotal));
        lblDiscount.setText("Diskon: Rp " + String.format("%,d", discount));
        lblTax.setText("Pajak (10%): Rp " + String.format("%,d", Math.round(tax)));
        lblService.setText("Biaya layanan: Rp " + String.format("%,d", SERVICE_FEE));
        lblTotal.setText("TOTAL: Rp " + String.format("%,d", Math.round(total)));
    }

    void applyCouponValue(long value){
        // naive: subtract coupon value from final display by writing temporary label change
        // For simplicity we'll subtract from subtotal when finalizing payment (track as special flag is better)
        // Here we show a note and adjust labels by reducing subtotal temporarily.
        // (In production you'd keep a state; keep this simple)
        long subtotal = computeSubtotal();
        long discount = 0;
        if(subtotal > 100_000) discount = Math.round(subtotal * 0.10);
        double after = subtotal - discount - value;
        if(after < 0) after = 0;
        double tax = after * TAX_RATE;
        double total = after + tax + SERVICE_FEE;
        lblDiscount.setText("Diskon: Rp " + String.format("%,d", discount) + " (dengan kupon)");
        lblTotal.setText("TOTAL: Rp " + String.format("%,d", Math.round(total)));
    }

    void doPayment(ComboBox<String> cbWallets, PasswordField pfPassword){
        if(cart.isEmpty()){
            showAlert(Alert.AlertType.WARNING, "Keranjang kosong", "Tambahkan item terlebih dahulu.");
            return;
        }
        long subtotal = computeSubtotal();
        long discount = 0;
        if(subtotal > 100_000) discount = Math.round(subtotal * 0.10);
        double after = subtotal - discount;
        double tax = after * TAX_RATE;
        long totalPay = Math.round(after + tax + SERVICE_FEE);

        // show simple confirmation with possibility for coupon input
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Konfirmasi Pembayaran");
        VBox vb = new VBox(8);
        vb.setPadding(new Insets(10));
        vb.getChildren().add(new Label("Ringkasan pembayaran:"));
        vb.getChildren().add(new Label("Subtotal: Rp " + String.format("%,d", subtotal)));
        if(discount>0) vb.getChildren().add(new Label("Diskon 10%: -Rp " + String.format("%,d", discount)));
        vb.getChildren().add(new Label("Pajak 10%: Rp " + String.format("%,d", Math.round(tax))));
        vb.getChildren().add(new Label("Biaya layanan: Rp " + String.format("%,d", SERVICE_FEE)));
        vb.getChildren().add(new Label("TOTAL BAYAR: Rp " + String.format("%,d", totalPay)));
        vb.getChildren().add(new Label("Masukkan kupon jika ada (kosongkan jika tidak):"));
        TextField tfCoupon = new TextField();
        vb.getChildren().add(tfCoupon);

        dialog.getDialogPane().setContent(vb);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> res = dialog.showAndWait();

        if(!res.isPresent() || res.get() != ButtonType.OK) return;

        String inputCoupon = tfCoupon.getText().trim();
        long couponValue = 0;
        String usedCouponHash = null;
        if(!inputCoupon.isEmpty()){
            if(coupons.containsKey(inputCoupon)){
                couponValue = coupons.get(inputCoupon);
                usedCouponHash = inputCoupon;
                totalPay -= couponValue;
                if(totalPay < 0) totalPay = 0;
            } else {
                showAlert(Alert.AlertType.ERROR, "Kupon tidak valid", "Kupon tidak ditemukan. Lanjut tanpa kupon.");
            }
        }

        // choose wallet selection index
        int idx = cbWallets.getSelectionModel().getSelectedIndex();
        Wallet chosen = wallets[idx];

        // verify password dialog
        TextInputDialog pwdDialog = new TextInputDialog();
        pwdDialog.setTitle("Verifikasi Wallet");
        pwdDialog.setHeaderText("Masukkan kata kunci untuk " + chosen.name);
        Optional<String> pwdRes = pwdDialog.showAndWait();
        if(!pwdRes.isPresent()) return;
        String pwd = pwdRes.get().trim();
        if(!chosen.verify(pwd)){
            showAlert(Alert.AlertType.ERROR, "Verifikasi gagal", "Kata kunci salah. Pembayaran dibatalkan.");
            return;
        }

        if(!chosen.canPay(totalPay)){
            showAlert(Alert.AlertType.ERROR, "Saldo tidak cukup", "Saldo pada wallet terpilih tidak mencukupi.");
            return;
        }

        // proceed payment
        chosen.deduct(totalPay);
        String transactionHash = sha256(chosen.name + "|" + totalPay + "|" + System.currentTimeMillis());

        // generate 1 coupon for next visit (keep hidden? user requested to display full — we'll display and save)
        String coupon = generateSingleCoupon();
        saveCoupon(couponFile, coupon, 50_000L); // default value 50k

        // assemble struk text
        StringBuilder sb = new StringBuilder();
        sb.append("====== STRUK K-DELIGHTS ======\n");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        sb.append("Waktu: ").append(dtf.format(LocalDateTime.now())).append("\n\n");
        int i=1;
        for(CartLine cl : cart){
            sb.append(String.format("%d) %s x%d -> Rp %,d\n", i++, cl.getName(), cl.getQty(), cl.getTotal()));
            if(isDrink(cl.getName()) && subtotal > 50_000){ // show promo note for first drink found
                sb.append("   (Promo: +1 " + cl.getName() + " GRATIS!)\n");
            }
        }
        sb.append("--------------------------------\n");
        sb.append(String.format("Subtotal        : Rp %,d\n", subtotal));
        if(discount>0) sb.append(String.format("Diskon 10%%      : -Rp %,d\n", discount));
        if(usedCouponHash != null) sb.append(String.format("Kupon digunakan : -Rp %,d (hash: %s)\n", couponValue, usedCouponHash));
        sb.append(String.format("Pajak 10%%       : Rp %,d\n", Math.round(tax)));
        sb.append(String.format("Biaya layanan   : Rp %,d\n", SERVICE_FEE));
        sb.append("--------------------------------\n");
        sb.append(String.format("TOTAL BAYAR     : Rp %,d\n", totalPay));
        sb.append("Dibayar via     : " + chosen.name + "\n");
        sb.append("Transaksi ID    : " + transactionHash + "\n");
        sb.append(String.format("Sisa saldo %s : Rp %,d\n", chosen.name, chosen.balance));
        sb.append("\n🎟 Kupon untuk kunjungan berikutnya (hash):\n");
        sb.append(coupon + "\n");

        // save struk
        saveStrukToFile(sb.toString());


// show success dialog with struk preview
TextArea ta = new TextArea(sb.toString());
ta.setEditable(false);
ta.setWrapText(true);
ta.setPrefSize(600, 400);
Stage st = new Stage();
st.initModality(Modality.APPLICATION_MODAL);
st.setTitle("Pembayaran Berhasil - Struk");

// ganti nama vb jadi vbSuccess
VBox vbSuccess = new VBox(8, new Label("Pembayaran sukses ✅"), ta, new Button("OK"));
vbSuccess.setPadding(new Insets(8));

Button ok = (Button) vbSuccess.getChildren().get(2);
ok.setOnAction(ae -> st.close());

st.setScene(new Scene(vbSuccess));
st.showAndWait();
        // clear cart after success
        cart.clear();
        refreshSummary();
    }

    String generateSingleCoupon(){
        String[] types = {"EMAS","PERUNGU","PERAK","PLATINUM","DIAMOND","SAPPHIRE"};
        Random rnd = new Random();
        String type = types[rnd.nextInt(types.length)];
        String raw = type + System.currentTimeMillis() + rnd.nextInt(9999);
        return sha256(raw);
    }

    // Command parser — "free command" support
    void handleCommand(String cmd, ComboBox<String> cbWallets, PasswordField pfPassword){
        if(cmd.isBlank()) return;
        String lc = cmd.toLowerCase();
        try{
            if(lc.startsWith("add ")){
                // format: add <name> <qty>
                String[] parts = cmd.split("\\s+");
                if(parts.length >= 2){
                    String name = parts[1];
                    int qty = parts.length >= 3 ? Integer.parseInt(parts[2]) : 1;
                    // find in foods or drinks
                    for(MenuItemModel m : foodList){
                        if(m.name.equalsIgnoreCase(name)){
                            addToCartByName(m, qty); return;
                        }
                    }
                    for(MenuItemModel m : drinkList){
                        if(m.name.equalsIgnoreCase(name)){
                            addToCartByName(m, qty); return;
                        }
                    }
                    showAlert(Alert.AlertType.WARNING, "Tidak ditemukan", "Menu '"+name+"' tidak ditemukan.");
                }
            } else if(lc.equals("pay")){
                doPayment(cbWallets, pfPassword);
            } else if(lc.startsWith("remove ")){
                // remove <index>  (1-based)
                String[] p = cmd.split("\\s+");
                if(p.length >= 2){
                    int idx = Integer.parseInt(p[1]) - 1;
                    if(idx >= 0 && idx < cart.size()){
                        cart.remove(idx);
                        refreshSummary();
                    }
                }
            } else if(lc.equals("clear")){
                cart.clear(); refreshSummary();
            } else if(lc.equals("struk")){
                try{ Desktop.getDesktop().open(new File("struk.txt")); } catch(Exception e){ showAlert(Alert.AlertType.INFORMATION,"Struk","File struk.txt mungkin belum ada atau tidak bisa dibuka."); }
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Perintah", "Perintah tidak dikenali: " + cmd);
            }
        } catch(Exception ex){
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal memproses perintah: " + ex.getMessage());
        }
    }

    void addToCartByName(MenuItemModel m, int qty){
        for(CartLine cl : cart){
            if(cl.item.name.equalsIgnoreCase(m.name)){
                cl.qty = cl.qty + qty; tvCart.refresh(); refreshSummary(); return;
            }
        }
        cart.add(new CartLine(m, qty)); refreshSummary();
    }

    // Utilities
    void showAlert(Alert.AlertType t, String title, String body){
        Alert a = new Alert(t);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(body);
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

