package com.mycompany.peminjamanruangan.View;

import com.mycompany.peminjamanruangan.model.Peminjaman;
import com.mycompany.peminjamanruangan.model.Ruangan;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class peminjamanView {
    private final Scanner scanner;

    public peminjamanView(Scanner scanner) {
        this.scanner = scanner;
    }

    public void tampilkanMenu() {
        System.out.println("\n=== SISTEM PENGAJUAN SURAT PEMINJAMAN RUANGAN ===");
        System.out.println("1. Pengajuan Surat");
        System.out.println("2. Cek Status");
        System.out.println("3. Hapus Peminjaman");
        System.out.println("4. Ganti Ruangan");
        System.out.println("5. Keluar");
    }

    public int bacaPilihanMenu() {
        while (true) {
            int pilihan = bacaInt("Pilih menu (1-5): ");
            if (pilihan >= 1 && pilihan <= 5) return pilihan;
            System.out.println(">> Pilihan menu harus antara 1 sampai 5.");
        }
    }

    public String bacaString(String pesan) {
        while (true) {
            System.out.print(pesan);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) return input;
            System.out.println(">> Input tidak boleh kosong.");
        }
    }

    public int bacaInt(String pesan) {
        while (true) {
            System.out.print(pesan);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println(">> Input harus berupa angka.");
            }
        }
    }

    public String bacaTanggal(String pesan) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        while (true) {
            System.out.print(pesan);
            String input = scanner.nextLine().trim();
            try {
                LocalDate.parse(input, formatter);
                return input;
            } catch (DateTimeParseException e) {
                System.out.println(">> Format tanggal salah. Gunakan YYYY-MM-DD.");
            }
        }
    }

    public String bacaJam(String pesan) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        while (true) {
            System.out.print(pesan);
            String input = scanner.nextLine().trim();
            try {
                LocalTime.parse(input, formatter);
                return input;
            } catch (DateTimeParseException e) {
                System.out.println(">> Format jam salah. Gunakan HH:mm.");
            }
        }
    }

    public String bacaKonfirmasi(String pesan) {
        while (true) {
            System.out.print(pesan);
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("Y") || input.equalsIgnoreCase("N")) return input;
            System.out.println(">> Masukkan hanya Y atau N.");
        }
    }

    public int pilihKategoriPeminjam() {
        System.out.println("\n=== KATEGORI PEMINJAM ===");
        System.out.println("1. Internal  (Dosen / Mahasiswa / Staff)");
        System.out.println("2. Eksternal (Perusahaan / Organisasi Luar)");
        while (true) {
            int pilihan = bacaInt("Pilih kategori peminjam (1-2): ");
            if (pilihan == 1 || pilihan == 2) return pilihan;
            System.out.println(">> Pilihan hanya 1 atau 2.");
        }
    }

    /**
     * Menampilkan daftar ruangan master.
     * Polimorfisme: getJenisRuangan() dan getDetailRuangan() berbeda
     * hasilnya untuk setiap subclass Ruangan.
     */
    public void tampilkanDaftarRuangan(List<Ruangan> daftar) {
        System.out.println("\n=== DAFTAR RUANGAN ===");
        for (Ruangan r : daftar) {
            System.out.println(r.getIdRuangan() + ". " + r.getNamaRuangan()
                    + " [" + r.getJenisRuangan() + "]"
                    + " kapasitas " + r.getKapasitas()
                    + " | " + r.getDetailRuangan());
        }
    }

    /**
     * Menampilkan detail satu data peminjaman.
     * Polimorfisme bekerja di sini: p.getPeminjam().getIdentitas() dan
     * p.getRuangan().getDetailRuangan() memanggil implementasi yang berbeda
     * tergantung subclass konkret yang tersimpan di dalam objek Peminjaman.
     */
    public void tampilkanStatus(Peminjaman p) {
        System.out.println("\n--------------------------------");
        System.out.println("Nomor Surat  : " + p.getNomorSurat());
        System.out.println("Kategori     : " + p.getPeminjam().getKategori());
        System.out.println("Peminjam     : " + p.getPeminjam().getIdentitas());
        System.out.println("Jenis Ruangan: " + p.getRuangan().getJenisRuangan());
        System.out.println("Nama Ruangan : " + p.getRuangan().getNamaRuangan());
        System.out.println("Kapasitas    : " + p.getRuangan().getKapasitas());
        System.out.println("Detail       : " + p.getRuangan().getDetailRuangan());
        System.out.println("Tujuan       : " + p.getTujuan());
        System.out.println("Tanggal      : " + p.getJadwal().getTanggal());
        System.out.println("Waktu        : " + p.getJadwal().getJamMulai()
                           + " - " + p.getJadwal().getJamSelesai());
    }

    public void tekanEnter() {
        System.out.println("\nTekan ENTER untuk kembali...");
        scanner.nextLine();
    }

    public void pesan(String pesan) {
        System.out.println(pesan);
    }
}
