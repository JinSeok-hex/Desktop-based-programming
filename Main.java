import java.util.*;
import java.io.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

import restaurant.menu.Food;
import restaurant.menu.Drink;

public class Main {

    // --=== [SHA256 Helper] ===--
    static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // --=== [Wallet Class] ===--
    static class Wallet {
        String name;
        long balance;
        String passwordHash;
        Wallet(String name, long balance, String plainPwd) {
            this.name = name;
            this.balance = balance;
            this.passwordHash = sha256(plainPwd);
        }
        boolean verify(String pwd) { return sha256(pwd).equalsIgnoreCase(passwordHash); }
        boolean canPay(long amt) { return balance >= amt; }
        void deduct(long amt) { balance -= amt; }
    }

    // --=== [Load & Save Coupons for customer] ===--
    static Map<String, Long> loadCoupons(File f) {
        Map<String, Long> map = new HashMap<>();
        if (!f.exists()) return map;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", 2);
                if (parts.length == 2) map.put(parts[0], Long.parseLong(parts[1]));
            }
        } catch (Exception e) { System.out.println("⚠️ Gagal membaca file kupon: " + e.getMessage()); }
        return map;
    }

    static void saveCoupon(File f, String hash, long value) {
        try (FileWriter fw = new FileWriter(f, true)) {
            fw.write(hash + "," + value + System.lineSeparator());
        } catch (Exception e) { System.out.println("⚠️ Gagal menyimpan kupon: " + e.getMessage()); }
    }

    // --=== [Generate Single Random Coupon] ===--
    static String generateRandomCoupon() {
        String[] types = {"EMAS", "PERUNGU", "PERAK", "PLATINUM", "DIAMOND", "SAPPHIRE"};
        Random rnd = new Random();
        String type = types[rnd.nextInt(types.length)];
        String raw = type + System.currentTimeMillis() + rnd.nextInt(1000);
        return sha256(raw);
    }

    // --=== [Menu Helper] ===--
    static int getPrice(String name) {
        if (name == null) return 0;
        name = name.trim().toLowerCase();
        switch (name) {
            case "bibimbap": return 30000;
            case "kimchi": return 12000;
            case "tteokbokki": return 25000;
            case "bulgogi": return 35000;
            case "soju": return 35000;
            case "makgeolli": return 30000;
            case "sikhye": return 15000;
            case "omija tea": return 20000;
            default: return 0;
        }
    }

    static boolean isDrink(String name) {
        if (name == null) return false;
        name = name.trim().toLowerCase();
        return Arrays.asList("soju","makgeolli","sikhye","omija tea").contains(name);
    }

    // --=== [Main Program] ===--
    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner sc = new Scanner(System.in);
        File couponFile = new File("coupons.txt");
        Map<String, Long> coupons = loadCoupons(couponFile);

        // --=== [Welcome ASCII Art] ===--
        try { ProcessBuilder pb = new ProcessBuilder("figlet", "Welcome to K-Delights"); Process p = pb.start(); p.waitFor(); } 
        catch (Exception e) { System.out.println("Figlet tidak tersedia: " + e.getMessage()); }

        Food[] foods = {
            new Food("Bibimbap", 30000), new Food("Kimchi", 12000), new Food("Tteokbokki", 25000),
            new Food("Bulgogi", 35000)
        };
        Drink[] drinks = {
            new Drink("Soju", 35000), new Drink("Makgeolli", 30000), new Drink("Sikhye", 15000),
           new Drink("Omija Tea", 20000)
        };
        Wallet[] wallets = {
            new Wallet("myaccount", 5_000_000L, "myaccount"),
            new Wallet("mybank", 12_000_000L, "mybank"),
            new Wallet("mysecret", 100_000_000L, "mysecret")
        };

        // --=== [Greeting] ===--
System.out.println(
" _  __     ____       _ _       _     _       \n" +
"| |/ /    |  _ \\  ___| (_) __ _| |__ | |_ ___ \n" +
"| ' /_____| | | |/ _ \\ | |/ _` | '_ \\| __/ __|\n" +
"| . \\_____| |_| |  __/ | | (_| | | | | |_\\__ \\\n" +
"|_|\\_\\    |____/ \\___|_|_|\\__, |_| |_|\\__|___/\n" +
"                          |___/               \n" +
"                                             restaurant\n"
);

        System.out.println("1) Saya mau pesan 🍲");
        System.out.println("2) Saya gak jadi pesen ❌");
        System.out.print("Pilihan: ");
        String choice = sc.nextLine().trim();
        if (!choice.equals("1")) { System.out.println("😎 Terima kasih sudah mampir. Sampai jumpa!"); sc.close(); return; }

        // --=== [Display Menu] ===--
        System.out.println("\n📜 Menu Makanan:");
        for (int i = 0; i < foods.length; i++) System.out.printf("%d) %s - Rp %,d\n", i+1, foods[i].getNama(), foods[i].getHarga());
        System.out.println("\n🥤 Menu Minuman:");
        for (int i = 0; i < drinks.length; i++) System.out.printf("%d) %s - Rp %,d\n", i+1, drinks[i].getNama(), drinks[i].getHarga());

        // --=== [Order Input Loop] ===--
        final int MAX = 4;
        String[] orderNames = new String[MAX];
        int[] orderQty = new int[MAX];
        int orders = 0;

        while (orders < MAX) {
            System.out.printf("\n📝 Pesan ke-%d (0 selesai):\n", orders+1);
            System.out.print("Kategori (1=Makanan,2=Minuman,0=selesai): ");
            String cat = sc.nextLine().trim();
            if (cat.equals("0")) break;
            if (!(cat.equals("1") || cat.equals("2"))) { System.out.println("⚠️ Input kategori tidak valid!"); continue; }

            if (cat.equals("1")) {
                System.out.print("🍲 Masukkan nomor makanan: ");
                int idx = -1; try { idx=Integer.parseInt(sc.nextLine().trim())-1; } catch(Exception e){ idx=-1; }
                if(idx<0 || idx>=foods.length){System.out.println("⚠️ Nomor tidak valid."); continue;}
                System.out.print("Jumlah: "); int q; try { q=Integer.parseInt(sc.nextLine().trim()); } catch(Exception e){q=1;}
                if(q<1) q=1; orderNames[orders]=foods[idx].getNama(); orderQty[orders]=q;
                System.out.println("✅ Ditambahkan: "+orderNames[orders]+" x"+q);
            } else {
                System.out.print("🥤 Masukkan nomor minuman: ");
                int idx = -1; try { idx=Integer.parseInt(sc.nextLine().trim())-1; } catch(Exception e){ idx=-1; }
                if(idx<0 || idx>=drinks.length){System.out.println("⚠️ Nomor tidak valid."); continue;}
                System.out.print("Jumlah: "); int q; try { q=Integer.parseInt(sc.nextLine().trim()); } catch(Exception e){q=1;}
                if(q<1) q=1; orderNames[orders]=drinks[idx].getNama(); orderQty[orders]=q;
                System.out.println("✅ Ditambahkan: "+orderNames[orders]+" x"+q);
            }
            orders++;
            if(orders<MAX){ System.out.print("Mau pesan lagi? (y/n): "); String more=sc.nextLine().trim(); if(!more.equalsIgnoreCase("y")) break; }
        }

        if(orders==0){ System.out.println("⚠️ Tidak ada pesanan. Keluar."); sc.close(); return; }

        // --=== [Subtotal, Discount, Tax, Service] ===--
        int[] pricePerSlot = new int[orders]; long subtotal=0; boolean hasDrink=false;
        for(int i=0;i<orders;i++){ pricePerSlot[i]=getPrice(orderNames[i]); long slotTotal=(long)pricePerSlot[i]*orderQty[i]; subtotal+=slotTotal; if(isDrink(orderNames[i])) hasDrink=true; }

        boolean promoApplied=false; String promoDrink=null;
        if(subtotal>50_000 && hasDrink){ for(int i=0;i<orders;i++){ if(isDrink(orderNames[i]) && orderQty[i]>=1){ promoApplied=true; promoDrink=orderNames[i]; break; } } }

        double discount=0.0; double afterDiscount=subtotal;
        if(subtotal>100_000){ discount=subtotal*0.10; afterDiscount=subtotal-discount; }
        double tax=afterDiscount*0.10; double serviceFee=20_000.0; double totalPay=afterDiscount+tax+serviceFee;

        System.out.println("\n🧾 Ringkasan Pesanan:");
        for(int i=0;i<orders;i++){
            long slotTotal=(long)pricePerSlot[i]*orderQty[i];
            System.out.printf("%d) %s x%d -> Rp %,d\n", i+1, orderNames[i], orderQty[i], slotTotal);
            if(promoApplied && promoDrink!=null && promoDrink.equalsIgnoreCase(orderNames[i])) System.out.println("   (Promo: +1 "+promoDrink+" GRATIS!)");
        }
        System.out.printf("Subtotal        : Rp %,d\n", subtotal);
        if(discount>0) System.out.printf("Diskon 10%%      : -Rp %,d\n",(long)discount);
        System.out.printf("Pajak 10%%       : Rp %,d\n",(long)tax);
        System.out.printf("Biaya layanan   : Rp %,d\n",(long)serviceFee);
        System.out.printf("TOTAL BAYAR     : Rp %,d\n",(long)totalPay);

        // --=== [Payment Loop] ===--
        long amountToPay=(long)Math.round(totalPay);
        boolean paid=false; Wallet chosen=null;
        while(!paid){
            System.out.println("\n💳 Pilih metode pembayaran:");
            for(int i=0;i<wallets.length;i++) System.out.printf("%d) %s (Saldo: Rp %,d)\n", i+1, wallets[i].name, wallets[i].balance);
            System.out.print("Pilih (1-"+wallets.length+") atau 'b' untuk batal: ");
            String sel=sc.nextLine().trim();
            if(sel.equalsIgnoreCase("b")){System.out.println("⚠️ Pembayaran dibatalkan. Kembali ke menu."); break;}
            int widx=-1; try { widx=Integer.parseInt(sel)-1; } catch(Exception e){ widx=-1; }
            if(widx<0 || widx>=wallets.length){System.out.println("⚠️ Pilihan tidak valid."); continue;}
            chosen=wallets[widx];

            boolean verified=false;
            while(!verified){
                System.out.print("🔑 Masukkan kata kunci untuk "+chosen.name+" (atau 'g' ganti metode): ");
                String pwd=sc.nextLine().trim();
                if(pwd.equalsIgnoreCase("g")){ System.out.println("🔄 Ganti metode."); break; }
                if(!chosen.verify(pwd)){
                    System.out.println("❌ Verifikasi gagal. Pilih: 1) coba lagi 2) ganti 3) batal");
                    String opt=sc.nextLine().trim();
                    if(opt.equals("1")) continue;
                    else if(opt.equals("2")) break;
                    else{ System.out.println("⚠️ Pembayaran dibatalkan."); break;}
                } else verified=true;
            }
            if(!verified) continue;
            if(!chosen.canPay(amountToPay)){
                System.out.println("⚠️ Saldo tidak cukup. Pilih: 1) metode lain 2) batal"); String opt=sc.nextLine().trim();
                if(opt.equals("1")) continue; else{ System.out.println("⚠️ Pembayaran dibatalkan."); break;}
            }

            chosen.deduct(amountToPay);
            String transactionHash=sha256(chosen.name+"|"+amountToPay+"|"+System.currentTimeMillis());
            paid=true;

            // --=== [Save Struk + Generate 1 Kupon Random] ===--
            File strukFile=new File("struk.txt");
            try(FileWriter fw=new FileWriter(strukFile)){
                fw.write("====== STRUK K-DELIGHTS ======\n");
                for(int i=0;i<orders;i++){
                    long slotTotal=(long)pricePerSlot[i]*orderQty[i];
                    fw.write(String.format("%d) %s x%d -> Rp %,d\n", i+1, orderNames[i], orderQty[i], slotTotal));
                    if(promoApplied && promoDrink!=null && promoDrink.equalsIgnoreCase(orderNames[i])){
                        fw.write("   (Promo: +1 "+promoDrink+" GRATIS!)\n");
                    }
                }
                fw.write("--------------------------------\n");
                fw.write(String.format("TOTAL BAYAR     : Rp %,d\n", amountToPay));
                fw.write("Dibayar via     : "+chosen.name+"\n");
                fw.write("Transaksi ID    : "+transactionHash+"\n");
                fw.write(String.format("Sisa saldo %s : Rp %,d\n", chosen.name, chosen.balance));

                // generate 1 kupon per transaksi


                String c = generateRandomCoupon(); 

                saveCoupon(couponFile, c, 50_000L);
                fw.write("🎟 Kupon untuk kunjungan berikutnya:\n");
                fw.write("Kupon #1: " + c + "\n"); 

            } catch(Exception e){System.out.println("⚠️ Gagal simpan struk: "+e.getMessage());}

            System.out.println("\n✅ Pembayaran sukses! Struk tersimpan di struk.txt");
            System.out.println("Transaksi ID: "+transactionHash);
        }

        System.out.println("\n👋 Terima kasih telah memesan di K-Delights!");
        try { ProcessBuilder pb = new ProcessBuilder("figlet", "Sampai Jumpa!"); Process p = pb.start(); p.waitFor(); } 
        catch (Exception e) { System.out.println("Figlet tidak tersedia: " + e.getMessage()); }

        sc.close();
 }
}
