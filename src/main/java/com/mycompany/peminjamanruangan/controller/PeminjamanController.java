package com.mycompany.peminjamanruangan.controller;

import com.mycompany.peminjamanruangan.dao.PeminjamanDAO;
import com.mycompany.peminjamanruangan.model.*;
import com.mycompany.peminjamanruangan.View.peminjamanView;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class PeminjamanController {

    // Salinan kerja di memori. Sumber data utama adalah database.
    private final ArrayList<Peminjaman> daftarPeminjaman = new ArrayList<>();
    private final PeminjamanDAO dao = new PeminjamanDAO();
    private final peminjamanView view;

    public PeminjamanController(peminjamanView view) {
        this.view = view;

        try {
            daftarPeminjaman.addAll(dao.findAll());
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Gagal memuat data peminjaman dari MariaDB.", e);
        }
    }

    public void jalankan() {
        boolean berjalan = true;
        while (berjalan) {
            view.tampilkanMenu();
            switch (view.bacaPilihanMenu()) {
                case 1 -> ajukanSurat();
                case 2 -> tampilkanStatus();
                case 3 -> hapusSurat();
                case 4 -> gantiRuangan();
                case 5 -> {
                    berjalan = false;
                    view.pesan("\n>> Sistem telah dihentikan.");
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // MENU 1: PENGAJUAN SURAT
    // Jadwal diisi SEBELUM memilih ruangan, karena cek bentrok
    // membutuhkan ruangan dan jadwal sekaligus.
    // ---------------------------------------------------------------
    private void ajukanSurat() {
        System.out.println("\n=== PENGAJUAN SURAT PEMINJAMAN ===");

        try {
            // Data peminjam (polimorfisme: Internal / Eksternal)
            Peminjam peminjam = buatPeminjam();

            // Data surat
            String tujuan = view.bacaString("Tujuan peminjaman        : ");

            int nomorSurat;
            while (true) {
                nomorSurat = view.bacaInt("Nomor Surat         (INT): ");
                if (nomorSurat <= 0) {
                    view.pesan(">> Nomor surat harus lebih dari 0.");
                    continue;
                }
                if (nomorSuratSudahDigunakan(nomorSurat)) {
                    view.pesan(">> Nomor surat tersebut sudah digunakan.");
                    continue;
                }
                break;
            }

            // Jadwal
            String tanggal  = view.bacaTanggal("Tanggal Peminjaman       : ");
            String jamMulai = view.bacaJam("Jam Mulai                : ");
            String jamSelesai;
            while (true) {
                jamSelesai = view.bacaJam("Jam Selesai              : ");
                LocalTime mulai   = LocalTime.parse(jamMulai);
                LocalTime selesai = LocalTime.parse(jamSelesai);
                if (selesai.isAfter(mulai)) break;
                view.pesan(">> Jam selesai harus setelah jam mulai.");
            }

            // Pilih ruangan master + cek bentrok
            Ruangan ruangan;
            while (true) {
                ruangan = pilihRuangan();
                if (ruangan == null) {
                    view.pesan(">> Pengajuan dibatalkan.");
                    view.tekanEnter();
                    return;
                }
                if (!dao.adaBentrok(ruangan.getIdRuangan(),
                        LocalDate.parse(tanggal), jamMulai, jamSelesai, -1)) {
                    break;
                }
                view.pesan(">> Ruangan sudah dipinjam pada jadwal tersebut. Pilih ruangan lain.");
            }

            Peminjaman peminjaman = new Peminjaman(
                    peminjam,
                    tujuan,
                    nomorSurat,
                    ruangan,
                    new Jadwal(tanggal, jamMulai, jamSelesai)
            );

            // Database dulu, memori kemudian.
            dao.simpan(peminjaman);
            daftarPeminjaman.add(peminjaman);
            view.pesan("\n>> SURAT BERHASIL DISIMPAN KE DATABASE!");

        } catch (SQLException e) {
            view.pesan(">> Gagal menyimpan peminjaman: " + e.getMessage());
        }

        view.tekanEnter();
    }

    // ---------------------------------------------------------------
    // MENU 2: CEK STATUS (muat ulang dari database)
    // ---------------------------------------------------------------
    private void tampilkanStatus() {
        System.out.println("\n=== STATUS PEMINJAMAN ===");

        try {
            daftarPeminjaman.clear();
            daftarPeminjaman.addAll(dao.findAll());

            if (daftarPeminjaman.isEmpty()) {
                view.pesan(">> Belum ada data peminjaman.");
            } else {
                for (Peminjaman p : daftarPeminjaman) {
                    view.tampilkanStatus(p);
                }
            }
        } catch (SQLException e) {
            view.pesan(">> Gagal mengambil data: " + e.getMessage());
        }

        view.tekanEnter();
    }

    // ---------------------------------------------------------------
    // MENU 3: HAPUS PEMINJAMAN
    // ---------------------------------------------------------------
    private void hapusSurat() {
        System.out.println("\n=== HAPUS PEMINJAMAN ===");
        int nomor = view.bacaInt("Masukkan Nomor Surat: ");
        Peminjaman p = cariPeminjaman(nomor);

        if (p == null) {
            view.pesan(">> Nomor surat tidak ditemukan.");
        } else if (view.bacaKonfirmasi("\nYakin ingin menghapus surat ini? (Y/N): ")
                       .equalsIgnoreCase("Y")) {
            try {
                if (dao.hapus(nomor)) {
                    daftarPeminjaman.remove(p);
                    view.pesan(">> Surat peminjaman berhasil dihapus!");
                } else {
                    view.pesan(">> Data tidak ditemukan di database.");
                }
            } catch (SQLException e) {
                view.pesan(">> Gagal menghapus data: " + e.getMessage());
            }
        } else {
            view.pesan(">> Penghapusan dibatalkan.");
        }

        view.tekanEnter();
    }

    // ---------------------------------------------------------------
    // MENU 4: GANTI RUANGAN
    // ---------------------------------------------------------------
    private void gantiRuangan() {
        System.out.println("\n=== GANTI RUANGAN ===");
        Peminjaman p = cariPeminjaman(view.bacaInt("Masukkan Nomor Surat: "));

        if (p == null) {
            view.pesan(">> Nomor surat tidak ditemukan.");
            view.tekanEnter();
            return;
        }

        view.pesan("\nRuangan saat ini : " + p.getRuangan().getNamaRuangan()
                   + " (" + p.getRuangan().getJenisRuangan() + ")");

        try {
            Ruangan baru = pilihRuangan();

            if (baru == null) {
                view.pesan(">> Penggantian ruangan dibatalkan.");
            } else if (baru.getIdRuangan() == p.getRuangan().getIdRuangan()) {
                view.pesan(">> Itu adalah ruangan yang sedang dipakai.");
            } else if (dao.adaBentrok(baru.getIdRuangan(),
                    LocalDate.parse(p.getJadwal().getTanggal()),
                    p.getJadwal().getJamMulai(),
                    p.getJadwal().getJamSelesai(),
                    p.getNomorSurat())) {
                view.pesan(">> Ruangan tersebut bentrok dengan peminjaman lain.");
            } else if (dao.gantiRuangan(p.getNomorSurat(), baru.getIdRuangan())) {
                // Database berhasil -> baru ubah memori.
                p.setRuangan(baru);
                view.pesan(">> Ruangan berhasil diganti ke " + baru.getNamaRuangan()
                           + " (" + baru.getJenisRuangan() + ")");
            } else {
                view.pesan(">> Gagal: nomor surat tidak ditemukan di database.");
            }
        } catch (SQLException e) {
            view.pesan(">> Gagal mengganti ruangan: " + e.getMessage());
        }

        view.tekanEnter();
    }

    // ---------------------------------------------------------------
    // Helper: membuat objek Peminjam (polimorfisme: Internal / Eksternal)
    // ---------------------------------------------------------------
    private Peminjam buatPeminjam() {
        int kategori = view.pilihKategoriPeminjam();
        String nama  = view.bacaString("Nama peminjam            : ");

        if (kategori == 1) {
            String nip = view.bacaString("NIP / NIM                : ");
            return new PeminjamInternal(nama, nip);
        } else {
            String instansi = view.bacaString("Nama instansi            : ");
            return new PeminjamEksternal(nama, instansi);
        }
    }

    // ---------------------------------------------------------------
    // Helper: pilih ruangan dari data master.
    // Mengembalikan null jika tidak ada ruangan atau pengguna batal (0).
    // ---------------------------------------------------------------
    private Ruangan pilihRuangan() throws SQLException {
        List<Ruangan> daftar = dao.findAllRuangan();

        if (daftar.isEmpty()) {
            view.pesan(">> Belum ada ruangan terdaftar di database.");
            return null;
        }

        view.tampilkanDaftarRuangan(daftar);

        while (true) {
            int id = view.bacaInt("Pilih ID ruangan (0 = batal): ");
            if (id == 0) return null;
            for (Ruangan r : daftar) {
                if (r.getIdRuangan() == id) return r;
            }
            view.pesan(">> ID tidak ada dalam daftar.");
        }
    }

    // ---------------------------------------------------------------
    // Helpers pencarian & validasi
    // ---------------------------------------------------------------
    private boolean nomorSuratSudahDigunakan(int nomor) {
        for (Peminjaman p : daftarPeminjaman)
            if (p.getNomorSurat() == nomor) return true;
        return false;
    }

    private Peminjaman cariPeminjaman(int nomor) {
        for (Peminjaman p : daftarPeminjaman)
            if (p.getNomorSurat() == nomor) return p;
        return null;
    }
}
